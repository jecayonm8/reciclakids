package com.reciclakids.services

import com.reciclakids.config.BaseDatos
import com.reciclakids.models.CatalogoResiduos
import com.reciclakids.models.CategoriaResiduo
import com.reciclakids.models.Insignia
import com.reciclakids.plugins.Tokens
import com.reciclakids.repositories.AcudienteDeNino
import com.reciclakids.repositories.CuentasRepositorio
import com.reciclakids.repositories.GrupoDocente
import com.reciclakids.repositories.NinoGuardado
import com.reciclakids.repositories.NinosRepositorio
import com.reciclakids.repositories.ResultadosRepositorio
import com.reciclakids.repositories.RetosRepositorio
import com.reciclakids.services.DocenteServicio.Companion.porcentaje
import com.reciclakids.services.notificaciones.correos.CorreoRenderizado
import com.reciclakids.services.notificaciones.correos.DatosCorreoLogro
import com.reciclakids.services.notificaciones.correos.DatosCorreoReporteSemanal
import com.reciclakids.services.notificaciones.correos.EnlacesPieCorreo
import com.reciclakids.services.notificaciones.correos.EnviadorCorreo
import com.reciclakids.services.notificaciones.correos.MensajeInterpretativo
import com.reciclakids.services.notificaciones.correos.PlantillaCorreoLogro
import com.reciclakids.services.notificaciones.correos.PlantillaCorreoRecuperacion
import com.reciclakids.services.notificaciones.correos.PlantillaCorreoReporteSemanal
import com.reciclakids.services.notificaciones.correos.TiempoDeJuego
import com.reciclakids.services.notificaciones.correos.TipoCaneca
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.slf4j.LoggerFactory
import java.time.LocalDate
import kotlin.math.roundToInt

/** Un correo listo para salir, con lo que se guarda en el historial de UI-32. */
private data class CorreoPendiente(
    val acudiente: AcudienteDeNino,
    val ninoId: String,
    val tipo: String,
    val correo: CorreoRenderizado,
    val insignia: Insignia? = null,
    val retosCompletados: Int = 0,
    val retosPublicados: Int = 0,
    val aciertos: Int? = null,
)

/**
 * Componente de Notificaciones (C4 N3): correo de logro (UI-36), reporte semanal (UI-37) y enlace
 * de recuperación. Respeta el control parental: el acudiente decide qué recibe (UI-33).
 */
class NotificacionesServicio(
    private val bd: BaseDatos,
    private val cuentas: CuentasRepositorio,
    private val ninos: NinosRepositorio,
    private val retos: RetosRepositorio,
    private val resultados: ResultadosRepositorio,
    private val enviador: EnviadorCorreo,
    private val tokens: Tokens,
    private val reloj: Reloj,
    private val urlPublica: String,
) : AvisoInsignias, NotificadorCuentas {

    private val log = LoggerFactory.getLogger(NotificacionesServicio::class.java)

    override suspend fun enlaceRecuperacion(correo: String, nombre: String, enlace: String) {
        enviador.enviar(correo, PlantillaCorreoRecuperacion.renderizar(nombre, enlace))
    }

    /** Correo de logro: como mucho uno al día por niño y acudiente, y solo si el acudiente lo quiere. */
    override suspend fun nuevas(ganadas: InsigniasGanadas) {
        val pendientes = bd.transaccion {
            val nino = ninos.porId(ganadas.ninoId) ?: return@transaccion emptyList()
            if (!ninos.control(nino.id).correosLogro) return@transaccion emptyList()
            val grupo = cuentas.grupoPorId(nino.grupoId) ?: return@transaccion emptyList()
            val insignia = ganadas.insignias.last()
            val total = resultados.insignias(nino.id).size
            val inicioDelDia = reloj.ahora().toLocalDate().atStartOfDay(reloj.ahora().offset).toOffsetDateTime()
            ninos.acudientesDe(nino.id)
                .filter { resultados.correosLogroDesde(it.id, nino.id, inicioDelDia) == 0 }
                .map { acudiente ->
                    val datos = DatosCorreoLogro(
                        nombreNino = primerNombre(nino),
                        jardin = grupo.jardin,
                        grupo = grupo.nombre,
                        nombreInsignia = insignia.nombre,
                        descripcionLogro = insignia.logro,
                        insigniasGanadas = total.coerceIn(1, Insignia.entries.size),
                        insigniasTotales = Insignia.entries.size,
                        enlaceProgreso = "$urlPublica/padres",
                        enlacesPie = enlacesPie(acudiente.id, nino.id),
                    )
                    CorreoPendiente(acudiente, nino.id, "LOGRO", PlantillaCorreoLogro.renderizar(datos), insignia = insignia)
                }
        }
        entregar(pendientes)
    }

    /** Reporte de la semana de [lunes] a los acudientes de un grupo. Devuelve cuántos correos salieron. */
    suspend fun reporteSemanalDeGrupo(grupoId: String, lunes: LocalDate): Int {
        val pendientes = bd.transaccion {
            val grupo = cuentas.grupoPorId(grupoId) ?: return@transaccion emptyList()
            ninos.delGrupo(grupo.id).flatMap { reportesDeNino(grupo, it, lunes) }
        }
        return entregar(pendientes)
    }

    /** Tarea programada de los viernes: el reporte de la semana en curso a todos los grupos. */
    suspend fun reporteSemanalDeTodos(lunes: LocalDate): Int =
        bd.transaccion { cuentas.idsDeGrupos() }.sumOf { reporteSemanalDeGrupo(it, lunes) }

    /** Apaga los dos tipos de correo del niño para ese acudiente (enlace «Dejar de recibir»). */
    suspend fun darDeBaja(token: String): Boolean {
        val (acudienteId, ninoId) = tokens.leerBaja(token) ?: return false
        return bd.transaccion {
            if (!ninos.estaVinculado(acudienteId, ninoId)) return@transaccion false
            val control = ninos.control(ninoId)
            ninos.guardarControl(ninoId, control.copy(correosLogro = false, reporteSemanal = false), reloj.ahora())
            true
        }
    }

    private fun JdbcTransaction.reportesDeNino(grupo: GrupoDocente, nino: NinoGuardado, lunes: LocalDate): List<CorreoPendiente> {
        val control = ninos.control(nino.id)
        if (!control.reporteSemanal) return emptyList()
        val viernes = lunes.plusDays(4)
        val publicados = retos.publicadosEntre(grupo.id, lunes, viernes)
        if (publicados.isEmpty()) return emptyList()
        val ids = listOf(nino.id)
        val partidas = resultados.partidasDe(ids, lunes, viernes)
        val intentos = resultados.intentosDe(ids, lunes, viernes)
        val previos = resultados.intentosDe(ids, lunes.minusWeeks(1), viernes.minusWeeks(1))
        val completados = partidas.filter { it.terminada }.map { it.retoId }.toSet().intersect(publicados.map { it.id }.toSet()).size
        val aciertos = porcentaje(intentos) ?: 0
        val insignias = resultados.insignias(nino.id).filterValues { it in lunes..viernes.plusDays(2) }.keys
        fun datos(pie: EnlacesPieCorreo) = DatosCorreoReporteSemanal(
            nombreNino = primerNombre(nino),
            inicioSemana = lunes,
            finSemana = viernes,
            retosCompletados = completados,
            retosPublicados = publicados.size,
            porcentajeAciertos = aciertos,
            racha = partidas.maxOfOrNull { it.rachaMaxima } ?: 0,
            mensajeInterpretativo = mejora(intentos, previos),
            aciertosPorCaneca = DocenteServicio.porCategoria(intentos).mapKeys { caneca(it.key) },
            insigniasSemana = insignias.sortedBy { it.ordinal }.map { it.nombre },
            tiempoDeJuego = TiempoDeJuego(
                minutosSemana = (partidas.sumOf { it.tiempoMs } / 60_000.0).roundToInt(),
                diasConJuego = partidas.map { it.fecha }.toSet().size,
                limiteDiarioMinutos = control.limiteMinutos,
            ),
            enlaceReporte = "$urlPublica/padres",
            enlacesPie = pie,
        )
        return ninos.acudientesDe(nino.id).map { acudiente ->
            CorreoPendiente(
                acudiente = acudiente,
                ninoId = nino.id,
                tipo = "REPORTE_SEMANAL",
                correo = PlantillaCorreoReporteSemanal.renderizar(datos(enlacesPie(acudiente.id, nino.id))),
                retosCompletados = completados,
                retosPublicados = publicados.size,
                aciertos = aciertos,
            )
        }
    }

    /** Envía y guarda en el historial. Un correo que falla no detiene a los demás. */
    private suspend fun entregar(pendientes: List<CorreoPendiente>): Int {
        var enviados = 0
        for (pendiente in pendientes) {
            try {
                enviador.enviar(pendiente.acudiente.correo, pendiente.correo)
                bd.transaccion {
                    resultados.registrarCorreo(
                        id = nuevoId(),
                        acudienteId = pendiente.acudiente.id,
                        ninoId = pendiente.ninoId,
                        tipo = pendiente.tipo,
                        destinatario = pendiente.acudiente.correo,
                        insignia = pendiente.insignia,
                        retosCompletados = pendiente.retosCompletados,
                        retosPublicados = pendiente.retosPublicados,
                        aciertos = pendiente.aciertos,
                        ahora = reloj.ahora(),
                    )
                }
                enviados++
            } catch (error: Exception) {
                log.warn("No se pudo enviar un correo de tipo {}", pendiente.tipo, error)
            }
        }
        return enviados
    }

    private fun enlacesPie(acudienteId: String, ninoId: String) = EnlacesPieCorreo(
        preferencias = "$urlPublica/padres/preferencias",
        privacidad = "$urlPublica/privacidad",
        baja = "$urlPublica/correos/baja?token=${tokens.baja(acudienteId, ninoId)}",
    )

    companion object {
        /** El correo nombra al niño solo por su primer nombre: «Salomé». */
        fun primerNombre(nino: NinoGuardado): String = nino.nombre.substringBefore(' ')

        fun caneca(categoria: CategoriaResiduo): TipoCaneca = when (categoria) {
            CategoriaResiduo.Aprovechable -> TipoCaneca.Blanca
            CategoriaResiduo.Organico -> TipoCaneca.Verde
            CategoriaResiduo.NoAprovechable -> TipoCaneca.Negra
        }

        /** La categoría que más mejoró frente a la semana anterior, si subió al menos 5 puntos. */
        fun mejora(
            intentos: List<com.reciclakids.repositories.IntentoGuardado>,
            previos: List<com.reciclakids.repositories.IntentoGuardado>,
        ): MensajeInterpretativo? {
            val ahora = DocenteServicio.porCategoria(intentos)
            val antes = DocenteServicio.porCategoria(previos)
            return ahora.keys.filter { it in antes }
                .map { Triple(it, antes.getValue(it), ahora.getValue(it)) }
                .filter { (_, desde, hasta) -> hasta - desde >= MejoraMinimaPuntos }
                .maxByOrNull { (_, desde, hasta) -> hasta - desde }
                ?.let { (categoria, desde, hasta) -> MensajeInterpretativo.mejoraEn(caneca(categoria), desde, hasta) }
        }

        private const val MejoraMinimaPuntos = 5

        init {
            // El catálogo y las canecas del correo deben cubrir las mismas categorías.
            check(CatalogoResiduos.todos.map { it.categoria }.toSet() == CategoriaResiduo.entries.toSet())
        }
    }
}
