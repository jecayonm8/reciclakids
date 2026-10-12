package com.reciclakids.network

import com.reciclakids.model.ControlParental
import com.reciclakids.model.CorreoEnviado
import com.reciclakids.model.CuentaAdulto
import com.reciclakids.model.DiaDeJuego
import com.reciclakids.model.HijoVinculado
import com.reciclakids.model.HistorialCorreos
import com.reciclakids.model.InicioPadres
import com.reciclakids.model.Insignia
import com.reciclakids.model.LogroHijo
import com.reciclakids.model.LogrosHijo
import com.reciclakids.model.PerfilAcudiente
import com.reciclakids.model.PrivacidadHijo
import com.reciclakids.model.ReporteHijo
import com.reciclakids.model.SemanaHijo
import com.reciclakids.model.TipoCorreo
import com.reciclakids.model.inicioSemana
import com.reciclakids.util.esDiaHabil
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Semanas que se pueden elegir en el reporte (UI-30), contando la actual. */
const val SemanasEnReporte = 4

/** Hora de los correos de cada tipo, como en los prototipos. */
private val HoraReporteSemanal = LocalTime.of(17, 0)
private val HoraCorreoLogro = LocalTime.of(18, 10)

/**
 * Implementación PROVISIONAL mientras el backend no expone resultados: todo vive en memoria y se
 * pierde al cerrar la app. Con [conDatosDemo] Salomé trae cuatro semanas de juego; sin él, los
 * hijos vinculados todavía no han jugado y cada pantalla muestra su estado vacío.
 *
 * El control parental se guarda en [controles], el mismo que lee el Modo Niño para cortar el juego.
 */
class ServicioPadresEnMemoria(
    private val cuenta: CuentaAdulto,
    private val controles: ControlesParentales = ControlesParentales(),
    private val latenciaMs: Long = 600,
    private val reloj: Clock = Clock.systemDefaultZone(),
    conDatosDemo: Boolean = true,
) : ServicioPadres {

    /** Mientras sea `true` todo responde como si no hubiera red: sirve para probar los estados de error. */
    @Volatile
    var sinConexion = false

    private val mutex = Mutex()
    private val vinculados: MutableList<HijoVinculado> =
        mutableListOf(DatosDemoPadres.salome(hoy()), DatosDemoPadres.martin(hoy()))

    /** Días jugados por cada niño. Lo que se borra al procesar una eliminación no vuelve. */
    private val dias: MutableMap<String, List<DiaDeJuego>> =
        if (conDatosDemo) mutableMapOf(DatosDemoPadres.IdSalome to DatosDemoPadres.diasSalome(hoy())) else mutableMapOf()
    private val solicitudes = mutableMapOf<String, LocalDate>()

    override fun hoy(): LocalDate = LocalDate.now(reloj)

    private fun ahora(): LocalDateTime = LocalDateTime.now(reloj)

    override suspend fun hijos(): Respuesta<List<HijoVinculado>> = consultar { vinculados.toList() }

    override suspend fun inicio(hijoId: String): Respuesta<InicioPadres> = consultar {
        val hijo = hijo(hijoId)
        val hoy = hoy()
        val insignias = insignias(hijoId)
        val semana = semana(hijoId, inicioSemana(hoy))
        InicioPadres(
            hijo = hijo,
            hoy = hoy,
            semana = semana,
            anterior = semana(hijoId, semana.inicio.minusWeeks(1)).takeIf { it.jugo },
            ultimoLogro = insignias.maxByOrNull { it.value }?.let { LogroHijo(it.key, it.value) },
            insigniasGanadas = insignias.size,
            limiteMinutos = controles.de(hijoId).limiteMinutosDiarios,
            sinDatos = diasDe(hijoId).isEmpty(),
        )
    }

    override suspend fun reporte(hijoId: String, semana: LocalDate?): Respuesta<ReporteHijo> = consultar {
        val hoy = hoy()
        val semanas = semanasJugadas(hijoId)
        val inicio = semana?.let(::inicioSemana)?.takeIf { it in semanas } ?: semanas.firstOrNull() ?: inicioSemana(hoy)
        ReporteHijo(
            hijo = hijo(hijoId),
            hoy = hoy,
            semanas = semanas,
            semana = semana(hijoId, inicio),
            anterior = semana(hijoId, inicio.minusWeeks(1)).takeIf { it.jugo },
            evolucion = semanas.reversed().mapNotNull { lunes -> semana(hijoId, lunes).aciertos?.let { lunes to it } },
            limiteMinutos = controles.de(hijoId).limiteMinutosDiarios,
        )
    }

    override suspend fun logros(hijoId: String): Respuesta<LogrosHijo> = consultar {
        val ganadas = insignias(hijoId)
        val jugados = diasDe(hijoId).filter { it.completo }
        LogrosHijo(
            hijo = hijo(hijoId),
            hoy = hoy(),
            logros = Insignia.entries.map { LogroHijo(it, ganadas[it]) },
            retosCompletados = jugados.size,
            diasConReto = jugados.map { it.fecha }.distinct().size,
        )
    }

    override suspend fun correos(hijoId: String): Respuesta<HistorialCorreos> = consultar {
        val ahora = ahora()
        val logros = insignias(hijoId).map { (insignia, fecha) ->
            CorreoEnviado(TipoCorreo.Logro, fecha.atTime(HoraCorreoLogro), cuenta.correo, insignia = insignia)
        }
        val reportes = semanasJugadas(hijoId).map { semana(hijoId, it) }.map { semana ->
            CorreoEnviado(
                tipo = TipoCorreo.ReporteSemanal,
                enviado = semana.inicio.with(DayOfWeek.FRIDAY).atTime(HoraReporteSemanal),
                destinatario = cuenta.correo,
                retosCompletados = semana.retosCompletados,
                retosPublicados = semana.retosPublicados,
                aciertos = semana.aciertos,
            )
        }
        val enviados = (logros + reportes).filter { !it.enviado.isAfter(ahora) }.sortedByDescending { it.enviado }
        HistorialCorreos(hijo(hijoId), hoy(), enviados)
    }

    override suspend fun controlParental(hijoId: String): Respuesta<ControlParental> = consultar {
        hijo(hijoId)
        controles.de(hijoId)
    }

    override suspend fun guardarControlParental(hijoId: String, control: ControlParental): Boolean = accion {
        hijo(hijoId)
        controles.guardar(hijoId, control)
    }

    override suspend fun privacidad(hijoId: String): Respuesta<PrivacidadHijo> = consultar {
        val hijo = hijo(hijoId)
        PrivacidadHijo(
            hijo = hijo,
            autorizadaEl = hijo.vinculadoEl,
            autorizadaPor = cuenta.nombre,
            registros = diasDe(hijoId).size,
            insignias = insignias(hijoId).size,
            solicitudEliminacion = solicitudes[hijoId],
        )
    }

    override suspend fun solicitarEliminacion(hijoId: String): LocalDate? {
        delay(latenciaMs)
        if (sinConexion) return null
        return mutex.withLock {
            hijo(hijoId)
            solicitudes.getOrPut(hijoId) { hoy() }
        }
    }

    override suspend fun cancelarSolicitudEliminacion(hijoId: String): Boolean = accion { solicitudes.remove(hijoId) }

    override suspend fun perfil(): Respuesta<PerfilAcudiente> = consultar {
        PerfilAcudiente(cuenta.nombre, cuenta.correo, vinculados.toList())
    }

    override suspend fun vincular(codigo: String): ResultadoVinculacion {
        delay(latenciaMs)
        if (sinConexion) return ResultadoVinculacion.SinConexion
        return mutex.withLock {
            val id = DatosDemoPadres.codigos[codigo.trim().uppercase()]
            val hijo = id?.let { DatosDemoPadres.porId(it, hoy()) }
            val existente = vinculados.firstOrNull { it.id == id }
            when {
                hijo == null -> ResultadoVinculacion.CodigoInvalido
                existente != null -> ResultadoVinculacion.YaVinculado(existente)
                else -> {
                    vinculados += hijo
                    ResultadoVinculacion.Vinculado(hijo)
                }
            }
        }
    }

    /** Un acudiente solo consulta a sus propios hijos: cualquier otro id es un error de programación. */
    private fun hijo(id: String): HijoVinculado =
        requireNotNull(vinculados.firstOrNull { it.id == id }) { "El niño $id no está vinculado a esta cuenta" }

    private fun diasDe(hijoId: String): List<DiaDeJuego> = dias[hijoId].orEmpty()

    private fun insignias(hijoId: String): Map<Insignia, LocalDate> = DatosDemoPadres.insignias(hijoId, diasDe(hijoId))

    /** Lunes de las semanas con juego, de la más reciente a la más antigua. */
    private fun semanasJugadas(hijoId: String): List<LocalDate> =
        diasDe(hijoId).map { inicioSemana(it.fecha) }.distinct().sortedDescending().take(SemanasEnReporte)

    private fun semana(hijoId: String, lunes: LocalDate): SemanaHijo {
        val hoy = hoy()
        val habiles = (0L..4L).map { lunes.plusDays(it) }.filter { esDiaHabil(it) && !it.isAfter(hoy) }
        val insignias = insignias(hijoId)
        return SemanaHijo(
            inicio = lunes,
            retosPublicados = habiles.size,
            dias = diasDe(hijoId).filter { it.fecha in habiles },
            insignias = insignias.filterValues { it in habiles }.map { LogroHijo(it.key, it.value) }.sortedBy { it.ganadaEl },
        )
    }

    private suspend fun <T> consultar(bloque: () -> T): Respuesta<T> {
        delay(latenciaMs)
        if (sinConexion) return Respuesta.SinConexion
        return Respuesta.Ok(mutex.withLock { bloque() })
    }

    private suspend fun accion(bloque: () -> Unit): Boolean {
        delay(latenciaMs)
        if (sinConexion) return false
        mutex.withLock { bloque() }
        return true
    }
}
