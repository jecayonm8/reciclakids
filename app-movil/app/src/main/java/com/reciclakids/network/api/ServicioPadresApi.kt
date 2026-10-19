package com.reciclakids.network.api

import com.reciclakids.model.CatalogoResiduos
import com.reciclakids.model.Conteo
import com.reciclakids.model.ControlParental
import com.reciclakids.model.CorreoEnviado
import com.reciclakids.model.DiaDeJuego
import com.reciclakids.model.HijoVinculado
import com.reciclakids.model.HistorialCorreos
import com.reciclakids.model.InicioPadres
import com.reciclakids.model.LogroHijo
import com.reciclakids.model.LogrosHijo
import com.reciclakids.model.PerfilAcudiente
import com.reciclakids.model.PrivacidadHijo
import com.reciclakids.model.ReporteHijo
import com.reciclakids.model.SemanaHijo
import com.reciclakids.model.TipoCorreo
import com.reciclakids.network.DatosPerfilAcudiente
import com.reciclakids.network.Respuesta
import com.reciclakids.network.ResultadoEdicionPerfil
import com.reciclakids.network.ResultadoVinculacion
import com.reciclakids.network.ServicioPadres
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * El Modo Padres (UI-29 a UI-35) contra la API /acudiente, con el token del acudiente. El backend
 * solo responde por los hijos vinculados a la cuenta: otro niño es un 404.
 */
class ServicioPadresApi(
    private val cliente: ClienteApi,
    private val sesion: SesionAdultoGuardada? = null,
    reloj: Clock = Clock.systemDefaultZone(),
    /**
     * El límite nuevo también vale de inmediato en el Modo Niño de este teléfono, si el niño juega
     * en él; en otro teléfono llega con su próxima sesión o sincronización.
     */
    private val alGuardarLimite: suspend (ninoId: String, minutos: Int) -> Unit = { _, _ -> },
) : ServicioPadres {

    private val hoyServidor = HoyServidor(reloj)

    override fun hoy(): LocalDate = hoyServidor.hoy()

    override suspend fun hijos(): Respuesta<List<HijoVinculado>> =
        cliente.get<List<HijoDto>>("acudiente/hijos").comoRespuesta { hijos -> hijos.map { it.aModelo() } }

    override suspend fun inicio(hijoId: String): Respuesta<InicioPadres> =
        cliente.get<InicioPadresDto>("acudiente/hijos/$hijoId/inicio").comoRespuesta {
            hoyServidor.recordar(it.hoy)
            InicioPadres(
                hijo = it.hijo.aModelo(),
                hoy = LocalDate.parse(it.hoy),
                semana = it.semana.aModelo(),
                anterior = it.anterior?.aModelo(),
                ultimoLogro = it.ultimoLogro?.aModelo(),
                insigniasGanadas = it.insigniasGanadas,
                limiteMinutos = it.limiteMinutos,
                sinDatos = it.sinDatos,
            )
        }

    override suspend fun reporte(hijoId: String, semana: LocalDate?): Respuesta<ReporteHijo> {
        val consulta = semana?.let { "?semana=$it" }.orEmpty()
        return cliente.get<ReporteHijoDto>("acudiente/hijos/$hijoId/reporte$consulta").comoRespuesta {
            hoyServidor.recordar(it.hoy)
            ReporteHijo(
                hijo = it.hijo.aModelo(),
                hoy = LocalDate.parse(it.hoy),
                semanas = it.semanas.map(LocalDate::parse),
                semana = it.semana.aModelo(),
                anterior = it.anterior?.aModelo(),
                evolucion = it.evolucion.map { punto -> LocalDate.parse(punto.semana) to punto.aciertos },
                limiteMinutos = it.limiteMinutos,
            )
        }
    }

    override suspend fun logros(hijoId: String): Respuesta<LogrosHijo> =
        cliente.get<LogrosHijoDto>("acudiente/hijos/$hijoId/logros").comoRespuesta {
            hoyServidor.recordar(it.hoy)
            LogrosHijo(it.hijo.aModelo(), LocalDate.parse(it.hoy), it.logros.map { logro -> logro.aModelo() }, it.retosCompletados, it.diasConReto)
        }

    override suspend fun correos(hijoId: String): Respuesta<HistorialCorreos> =
        cliente.get<HistorialCorreosDto>("acudiente/hijos/$hijoId/correos").comoRespuesta {
            hoyServidor.recordar(it.hoy)
            HistorialCorreos(
                hijo = it.hijo.aModelo(),
                hoy = LocalDate.parse(it.hoy),
                correos = it.correos.map { correo ->
                    CorreoEnviado(
                        tipo = when (correo.tipo) {
                            TipoCorreoDto.Logro -> TipoCorreo.Logro
                            TipoCorreoDto.ReporteSemanal -> TipoCorreo.ReporteSemanal
                        },
                        enviado = LocalDateTime.parse(correo.enviado),
                        destinatario = correo.destinatario,
                        insignia = correo.insignia,
                        retosCompletados = correo.retosCompletados,
                        retosPublicados = correo.retosPublicados,
                        aciertos = correo.aciertos,
                    )
                },
            )
        }

    override suspend fun controlParental(hijoId: String): Respuesta<ControlParental> =
        cliente.get<ControlParentalDto>("acudiente/hijos/$hijoId/control-parental").comoRespuesta {
            ControlParental(it.limiteMinutosDiarios, it.dificultadSugerida, it.correosLogro, it.reporteSemanal, it.recordatorioSinJugar)
        }

    override suspend fun guardarControlParental(hijoId: String, control: ControlParental): Boolean {
        val dto = ControlParentalDto(
            control.limiteMinutosDiarios,
            control.dificultadSugerida,
            control.correosLogro,
            control.reporteSemanal,
            control.recordatorioSinJugar,
        )
        val guardado = cliente.put<Unit>("acudiente/hijos/$hijoId/control-parental", dto).exitosa
        if (guardado) alGuardarLimite(hijoId, control.limiteMinutosDiarios)
        return guardado
    }

    override suspend fun privacidad(hijoId: String): Respuesta<PrivacidadHijo> =
        cliente.get<PrivacidadHijoDto>("acudiente/hijos/$hijoId/privacidad").comoRespuesta {
            PrivacidadHijo(
                hijo = it.hijo.aModelo(),
                autorizadaEl = LocalDate.parse(it.autorizadaEl),
                autorizadaPor = it.autorizadaPor,
                registros = it.registros,
                insignias = it.insignias,
                solicitudEliminacion = it.solicitudEliminacion?.let(LocalDate::parse),
            )
        }

    override suspend fun solicitarEliminacion(hijoId: String): LocalDate? =
        (cliente.post<SolicitudEliminacionDto>("acudiente/hijos/$hijoId/privacidad/solicitud-eliminacion") as? RespuestaApi.Exito)
            ?.dato?.let { runCatching { LocalDate.parse(it.solicitadaEl) }.getOrNull() }

    override suspend fun cancelarSolicitudEliminacion(hijoId: String): Boolean =
        cliente.delete<Unit>("acudiente/hijos/$hijoId/privacidad/solicitud-eliminacion").exitosa

    override suspend fun perfil(): Respuesta<PerfilAcudiente> =
        cliente.get<PerfilAcudienteDto>("acudiente/perfil").comoRespuesta { it.aModelo() }

    override suspend fun guardarPerfil(datos: DatosPerfilAcudiente): ResultadoEdicionPerfil =
        when (val respuesta = cliente.put<PerfilAcudienteDto>("acudiente/perfil", PerfilAcudienteSolicitud(datos.nombre, datos.correo))) {
            is RespuestaApi.Exito -> {
                val perfil = respuesta.dato
                sesion?.leer()?.let { sesion.actualizarCuenta(it.cuenta.copy(nombre = perfil.nombre, correo = perfil.correo)) }
                ResultadoEdicionPerfil.Guardado
            }
            is RespuestaApi.Rechazo -> if (respuesta.error == "correo_en_uso") ResultadoEdicionPerfil.CorreoEnUso else ResultadoEdicionPerfil.SinConexion
            RespuestaApi.SinConexion, RespuestaApi.SesionVencida -> ResultadoEdicionPerfil.SinConexion
        }

    override suspend fun vincular(codigo: String): ResultadoVinculacion =
        when (val respuesta = cliente.post<VinculacionDto>("acudiente/vinculaciones", VincularSolicitud(codigo.trim().uppercase()))) {
            is RespuestaApi.Exito -> {
                val hijo = runCatching { respuesta.dato.hijo.aModelo() }.getOrNull()
                when {
                    hijo == null -> ResultadoVinculacion.SinConexion
                    respuesta.dato.yaEstaba -> ResultadoVinculacion.YaVinculado(hijo)
                    else -> ResultadoVinculacion.Vinculado(hijo)
                }
            }
            is RespuestaApi.Rechazo -> if (respuesta.error == "codigo_invalido") ResultadoVinculacion.CodigoInvalido else ResultadoVinculacion.SinConexion
            RespuestaApi.SinConexion, RespuestaApi.SesionVencida -> ResultadoVinculacion.SinConexion
        }
}

private fun HijoDto.aModelo() = HijoVinculado(
    id = id,
    nombre = nombre,
    grupo = grupo,
    edad = edad,
    avatar = avatar,
    vinculadoEl = LocalDate.parse(vinculadoEl),
)

private fun SemanaHijoDto.aModelo() = SemanaHijo(
    inicio = LocalDate.parse(inicio),
    retosPublicados = retosPublicados,
    dias = dias.map { dia ->
        DiaDeJuego(
            fecha = LocalDate.parse(dia.fecha),
            minutos = dia.minutos,
            completo = dia.completo,
            porCategoria = dia.porCategoria.mapValues { (_, conteo) -> Conteo(conteo.aciertos, conteo.total) },
            rachaMaxima = dia.rachaMaxima,
            confusiones = dia.confusiones.mapNotNull(CatalogoResiduos::porId),
        )
    },
    insignias = insignias.map { it.aModelo() },
)

private fun LogroDto.aModelo() = LogroHijo(insignia, ganadaEl?.let(LocalDate::parse))

private fun PerfilAcudienteDto.aModelo() = PerfilAcudiente(nombre, correo, hijos.map { it.aModelo() })
