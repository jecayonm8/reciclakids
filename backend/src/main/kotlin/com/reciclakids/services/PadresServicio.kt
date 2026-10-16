package com.reciclakids.services

import com.reciclakids.config.BaseDatos
import com.reciclakids.models.CatalogoResiduos
import com.reciclakids.models.Insignia
import com.reciclakids.models.dto.ConteoDto
import com.reciclakids.models.dto.ControlParentalDto
import com.reciclakids.models.dto.CorreoEnviadoDto
import com.reciclakids.models.dto.DiaDeJuegoDto
import com.reciclakids.models.dto.HijoDto
import com.reciclakids.models.dto.HistorialCorreosDto
import com.reciclakids.models.dto.InicioPadresDto
import com.reciclakids.models.dto.LogroDto
import com.reciclakids.models.dto.LogrosHijoDto
import com.reciclakids.models.dto.PerfilAcudienteDto
import com.reciclakids.models.dto.PerfilAcudienteSolicitud
import com.reciclakids.models.dto.PrivacidadHijoDto
import com.reciclakids.models.dto.PuntoEvolucionDto
import com.reciclakids.models.dto.ReporteHijoDto
import com.reciclakids.models.dto.SemanaHijoDto
import com.reciclakids.models.dto.TipoCorreoDto
import com.reciclakids.models.dto.VinculacionDto
import com.reciclakids.plugins.DatosInvalidos
import com.reciclakids.plugins.NoEncontrado
import com.reciclakids.repositories.ControlGuardado
import com.reciclakids.repositories.CuentasRepositorio
import com.reciclakids.repositories.HijoGuardado
import com.reciclakids.repositories.NinosRepositorio
import com.reciclakids.repositories.ResultadosRepositorio
import com.reciclakids.repositories.RetosRepositorio
import com.reciclakids.services.DocenteServicio.Companion.inicioSemana
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Lo que necesita el Modo Padres (UI-29 a UI-35). Cada consulta verifica primero que el hijo esté
 * vinculado a la cuenta: un acudiente nunca ve a otros niños (RNF, validado en el backend).
 */
class PadresServicio(
    private val bd: BaseDatos,
    private val cuentas: CuentasRepositorio,
    private val ninos: NinosRepositorio,
    private val retos: RetosRepositorio,
    private val resultados: ResultadosRepositorio,
    private val reloj: Reloj,
) {
    suspend fun hijos(acudienteId: String): List<HijoDto> = bd.transaccion { ninos.hijosDe(acudienteId).map { it.aDto() } }

    suspend fun inicio(acudienteId: String, hijoId: String): InicioPadresDto = bd.transaccion {
        val hijo = hijoDe(acudienteId, hijoId)
        val hoy = reloj.hoy()
        val lunes = inicioSemana(hoy)
        val insignias = resultados.insignias(hijo.nino.id)
        val ultimo = insignias.maxByOrNull { it.value }
        InicioPadresDto(
            hijo = hijo.aDto(),
            hoy = hoy.toString(),
            semana = semana(hijo, lunes, insignias),
            anterior = semana(hijo, lunes.minusWeeks(1), insignias),
            ultimoLogro = ultimo?.let { LogroDto(it.key, it.value.toString()) },
            insigniasGanadas = insignias.size,
            limiteMinutos = ninos.control(hijo.nino.id).limiteMinutos,
            sinDatos = resultados.contarIntentos(hijo.nino.id) == 0,
        )
    }

    /** [semana] es el lunes a ver; `null` es la más reciente con datos o la actual. */
    suspend fun reporte(acudienteId: String, hijoId: String, semana: LocalDate?): ReporteHijoDto = bd.transaccion {
        val hijo = hijoDe(acudienteId, hijoId)
        val hoy = reloj.hoy()
        val actual = inicioSemana(hoy)
        val semanas = (0L until SemanasReporte).map { actual.minusWeeks(it) }
        val elegida = semana?.let(::inicioSemana)?.takeIf { it in semanas } ?: actual
        val insignias = resultados.insignias(hijo.nino.id)
        val desde = semanas.last()
        val intentos = resultados.intentosDe(listOf(hijo.nino.id), desde, hoy)
        ReporteHijoDto(
            hijo = hijo.aDto(),
            hoy = hoy.toString(),
            semanas = semanas.map { it.toString() },
            semana = semana(hijo, elegida, insignias),
            anterior = semana(hijo, elegida.minusWeeks(1), insignias),
            evolucion = semanas.reversed().mapNotNull { lunes ->
                DocenteServicio.porcentaje(intentos.filter { inicioSemana(it.fecha) == lunes })?.let { PuntoEvolucionDto(lunes.toString(), it) }
            },
            limiteMinutos = ninos.control(hijo.nino.id).limiteMinutos,
        )
    }

    suspend fun logros(acudienteId: String, hijoId: String): LogrosHijoDto = bd.transaccion {
        val hijo = hijoDe(acudienteId, hijoId)
        val insignias = resultados.insignias(hijo.nino.id)
        LogrosHijoDto(
            hijo = hijo.aDto(),
            hoy = reloj.hoy().toString(),
            logros = Insignia.entries.map { LogroDto(it, insignias[it]?.toString()) },
            retosCompletados = resultados.retosCompletados(hijo.nino.id),
            diasConReto = resultados.diasConReto(hijo.nino.id),
        )
    }

    suspend fun correos(acudienteId: String, hijoId: String): HistorialCorreosDto = bd.transaccion {
        val hijo = hijoDe(acudienteId, hijoId)
        HistorialCorreosDto(
            hijo = hijo.aDto(),
            hoy = reloj.hoy().toString(),
            correos = resultados.correos(acudienteId, hijo.nino.id).map {
                CorreoEnviadoDto(
                    tipo = if (it.tipo == "LOGRO") TipoCorreoDto.Logro else TipoCorreoDto.ReporteSemanal,
                    enviado = it.enviadoEn.atZoneSameInstant(reloj.ahora().offset).toLocalDateTime().toString(),
                    destinatario = it.destinatario,
                    insignia = it.insignia,
                    retosCompletados = it.retosCompletados,
                    retosPublicados = it.retosPublicados,
                    aciertos = it.aciertos,
                )
            },
        )
    }

    suspend fun controlParental(acudienteId: String, hijoId: String): ControlParentalDto = bd.transaccion {
        ninos.control(hijoDe(acudienteId, hijoId).nino.id).aDto()
    }

    suspend fun guardarControlParental(acudienteId: String, hijoId: String, control: ControlParentalDto) {
        val limite = control.limiteMinutosDiarios
        if (limite !in 5..45 || limite % 5 != 0) throw DatosInvalidos("El tiempo diario va de 5 a 45 minutos, de 5 en 5", "limite_invalido")
        bd.transaccion {
            val hijo = hijoDe(acudienteId, hijoId)
            ninos.guardarControl(
                hijo.nino.id,
                ControlGuardado(limite, control.dificultadSugerida, control.correosLogro, control.reporteSemanal),
                reloj.ahora(),
            )
        }
    }

    suspend fun privacidad(acudienteId: String, hijoId: String): PrivacidadHijoDto = bd.transaccion {
        val hijo = hijoDe(acudienteId, hijoId)
        val acudiente = cuentas.usuarioPorId(acudienteId)!!
        PrivacidadHijoDto(
            hijo = hijo.aDto(),
            autorizadaEl = hijo.autorizadaEn.toLocalDate().toString(),
            autorizadaPor = acudiente.nombre,
            registros = resultados.contarIntentos(hijo.nino.id),
            insignias = resultados.insignias(hijo.nino.id).size,
            solicitudEliminacion = ninos.solicitudEliminacion(hijo.nino.id)?.toString(),
        )
    }

    suspend fun solicitarEliminacion(acudienteId: String, hijoId: String): LocalDate = bd.transaccion {
        val hijo = hijoDe(acudienteId, hijoId)
        val hoy = reloj.hoy()
        ninos.solicitarEliminacion(hijo.nino.id, acudienteId, hoy)
        ninos.solicitudEliminacion(hijo.nino.id) ?: hoy
    }

    suspend fun cancelarEliminacion(acudienteId: String, hijoId: String) = bd.transaccion {
        ninos.cancelarEliminacion(hijoDe(acudienteId, hijoId).nino.id)
    }

    suspend fun perfil(acudienteId: String): PerfilAcudienteDto = bd.transaccion {
        val acudiente = cuentas.usuarioPorId(acudienteId)!!
        PerfilAcudienteDto(acudiente.nombre, acudiente.correo, ninos.hijosDe(acudienteId).map { it.aDto() })
    }

    suspend fun actualizarPerfil(acudienteId: String, datos: PerfilAcudienteSolicitud): PerfilAcudienteDto {
        val nombre = Validacion.texto(datos.nombre, "tu nombre", 120)
        val correo = Validacion.correo(datos.correo)
        bd.transaccion {
            val otro = cuentas.usuarioPorCorreo(correo)
            if (otro != null && otro.id != acudienteId) throw CuentasServicio.correoEnUso()
            cuentas.actualizarUsuario(acudienteId, nombre, correo)
        }
        return perfil(acudienteId)
    }

    suspend fun vincular(acudienteId: String, codigo: String): VinculacionDto = bd.transaccion {
        val nino = ninos.porCodigoVinculacion(codigo) ?: throw CuentasServicio.codigoInvalido()
        val nuevo = ninos.vincular(acudienteId, nino.id, reloj.ahora())
        val hijo = ninos.hijosDe(acudienteId).first { it.nino.id == nino.id }
        VinculacionDto(hijo.aDto(), yaEstaba = !nuevo)
    }

    private fun JdbcTransaction.hijoDe(acudienteId: String, hijoId: String): HijoGuardado =
        ninos.hijosDe(acudienteId).firstOrNull { it.nino.id == hijoId }
            // 404 y no 403: no se confirma que exista un niño que no es suyo.
            ?: throw NoEncontrado("hijo_no_encontrado", "Ese niño no está vinculado a tu cuenta")

    /** Lo que jugó el niño de lunes a viernes de la semana de [lunes] (SemanaHijo en la app). */
    private fun JdbcTransaction.semana(hijo: HijoGuardado, lunes: LocalDate, insignias: Map<Insignia, LocalDate>): SemanaHijoDto {
        val viernes = lunes.plusDays(4)
        val ids = listOf(hijo.nino.id)
        val partidas = resultados.partidasDe(ids, lunes, viernes)
        val intentos = resultados.intentosDe(ids, lunes, viernes)
        val dias = partidas.groupBy { it.fecha }.map { (fecha, delDia) ->
            val intentosDia = intentos.filter { it.fecha == fecha }
            DiaDeJuegoDto(
                fecha = fecha.toString(),
                minutos = (delDia.sumOf { it.tiempoMs } / 60_000.0).roundToInt(),
                completo = delDia.any { it.terminada },
                porCategoria = intentosDia.groupBy { CatalogoResiduos.porId(it.residuoId)!!.categoria }
                    .mapValues { (_, lista) -> ConteoDto(lista.count { it.correcto }, lista.size) },
                rachaMaxima = delDia.maxOf { it.rachaMaxima },
                confusiones = intentosDia.filter { !it.correcto }.map { it.residuoId }.distinct(),
            )
        }
        return SemanaHijoDto(
            inicio = lunes.toString(),
            retosPublicados = retos.publicadosEntre(hijo.nino.grupoId, lunes, viernes).size,
            dias = dias,
            insignias = insignias.filterValues { it in lunes..viernes.plusDays(2) }.map { LogroDto(it.key, it.value.toString()) },
        )
    }

    private fun HijoGuardado.aDto() = HijoDto(
        id = nino.id,
        nombre = nino.nombre,
        grupo = grupo,
        edad = nino.edad,
        avatar = nino.avatar,
        vinculadoEl = vinculadoEn.toLocalDate().toString(),
    )

    private fun ControlGuardado.aDto() = ControlParentalDto(limiteMinutos, dificultadSugerida, correosLogro, reporteSemanal)

    companion object {
        private const val SemanasReporte = 4L
    }
}
