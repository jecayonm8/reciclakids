package com.reciclakids.network.api

import com.reciclakids.model.CatalogoResiduos
import com.reciclakids.model.Nino
import com.reciclakids.model.Reto
import com.reciclakids.network.PartidaEnviada
import com.reciclakids.network.ResultadoCodigo
import com.reciclakids.network.ResultadoEnvio
import com.reciclakids.network.ServicioResultados
import com.reciclakids.network.ServicioRetos
import java.time.LocalDate

/**
 * Las sesiones de los niños en este teléfono: el niño entra «tipo Kahoot» (código del reto y su
 * nombre) y recibe un token propio que dura una semana (ADR-09). Al abrirla se guarda también su
 * límite diario de juego, que el Modo Niño hace cumplir sin red.
 */
class SesionesNino(
    private val cliente: ClienteApi,
    private val tokens: TokensNino,
    private val guardarLimite: suspend (ninoId: String, minutos: Int) -> Unit,
) {
    suspend fun abrir(codigo: String, ninoId: String): RespuestaApi<SesionNinoDto> {
        val respuesta = cliente.post<SesionNinoDto>("nino/sesiones", SesionNinoSolicitud(codigo, ninoId), Credencial.Ninguna)
        if (respuesta is RespuestaApi.Exito) {
            tokens.guardar(ninoId, respuesta.dato.token, respuesta.dato.venceEnSegundos)
            guardarLimite(ninoId, respuesta.dato.limiteMinutosDiarios)
        }
        return respuesta
    }

    fun token(ninoId: String): String? = tokens.leer(ninoId)

    fun olvidar(ninoId: String) = tokens.borrar(ninoId)

    /** Trae el límite diario que tenga ahora el niño (el acudiente pudo cambiarlo desde otro teléfono). */
    suspend fun actualizarLimite(ninoId: String, token: String) {
        val progreso = cliente.get<ProgresoNinoDto>("nino/progreso", Credencial.Nino(token))
        if (progreso is RespuestaApi.Exito) guardarLimite(ninoId, progreso.dato.limiteMinutosDiarios)
    }
}

/** Retos del día contra la API del Modo Niño (/nino/retos y /nino/sesiones). */
class ServicioRetosApi(
    private val cliente: ClienteApi,
    private val sesiones: SesionesNino,
) : ServicioRetos {

    /** La última respuesta al código: trae los niños del grupo y evita pedirlos otra vez en UI-09. */
    @Volatile
    private var ultimo: CodigoRetoDto? = null

    // El backend decide con su reloj si el código es de hoy (RNF-05): [hoy] no viaja.
    override suspend fun buscarReto(codigo: String, hoy: LocalDate): ResultadoCodigo =
        when (val respuesta = cliente.get<CodigoRetoDto>("nino/retos/${codigo.trim()}", Credencial.Ninguna)) {
            is RespuestaApi.Exito -> {
                ultimo = respuesta.dato
                runCatching { respuesta.dato.reto.aModelo() }.getOrNull()?.let { ResultadoCodigo.Valido(it) } ?: ResultadoCodigo.SinConexion
            }
            is RespuestaApi.Rechazo -> when (respuesta.error) {
                "codigo_vencido" -> ResultadoCodigo.Vencido
                "codigo_invalido" -> ResultadoCodigo.Invalido
                // 429: demasiados intentos desde la misma red; se puede volver a probar en un momento.
                else -> if (respuesta.estado == 404) ResultadoCodigo.Invalido else ResultadoCodigo.SinConexion
            }
            RespuestaApi.SinConexion, RespuestaApi.SesionVencida -> ResultadoCodigo.SinConexion
        }

    override suspend fun ninosDelGrupo(reto: Reto): List<Nino> {
        val datos = ultimo?.takeIf { it.reto.id == reto.id }
            ?: (cliente.get<CodigoRetoDto>("nino/retos/${reto.codigo}", Credencial.Ninguna) as? RespuestaApi.Exito)?.dato?.also { ultimo = it }
        return datos?.ninos.orEmpty().map { Nino(id = it.id, nombre = it.nombre) }
    }

    override suspend fun abrirSesion(reto: Reto, nino: Nino) {
        sesiones.abrir(reto.codigo, nino.id)
    }
}

/** Envía las partidas de cada niño a POST /nino/partidas con el token de ese niño. */
class ServicioResultadosApi(
    private val cliente: ClienteApi,
    private val sesiones: SesionesNino,
) : ServicioResultados {

    override suspend fun enviar(ninoId: String, partidas: List<PartidaEnviada>, codigoHoy: String?): ResultadoEnvio {
        val lote = LotePartidasSolicitud(partidas.map { it.aDto() })
        sesiones.token(ninoId)?.let { token ->
            val respuesta = enviarCon(token, lote)
            if (respuesta !is RespuestaApi.Rechazo || respuesta.estado != 401) return traducir(ninoId, token, respuesta)
            // El token venció o ya no sirve: se intenta abrir otra sesión con el código de hoy.
            sesiones.olvidar(ninoId)
        }
        if (codigoHoy == null) return ResultadoEnvio.SinSesion
        val token = when (val sesion = sesiones.abrir(codigoHoy, ninoId)) {
            is RespuestaApi.Exito -> sesion.dato.token
            is RespuestaApi.Rechazo -> return if (sesion.estado == 429) ResultadoEnvio.SinConexion else ResultadoEnvio.SinSesion
            RespuestaApi.SinConexion, RespuestaApi.SesionVencida -> return ResultadoEnvio.SinConexion
        }
        return traducir(ninoId, token, enviarCon(token, lote))
    }

    private suspend fun enviarCon(token: String, lote: LotePartidasSolicitud) =
        cliente.post<LotePartidasDto>("nino/partidas", lote, Credencial.Nino(token))

    private suspend fun traducir(ninoId: String, token: String, respuesta: RespuestaApi<LotePartidasDto>): ResultadoEnvio =
        when (respuesta) {
            is RespuestaApi.Exito -> {
                sesiones.actualizarLimite(ninoId, token)
                ResultadoEnvio.Recibido(respuesta.dato.insigniasNuevas)
            }
            is RespuestaApi.Rechazo -> when (respuesta.estado) {
                401 -> ResultadoEnvio.SinSesion
                429 -> ResultadoEnvio.SinConexion
                else -> ResultadoEnvio.Rechazado
            }
            RespuestaApi.SinConexion, RespuestaApi.SesionVencida -> ResultadoEnvio.SinConexion
        }
}

private fun RetoNinoDto.aModelo() = Reto(
    codigo = codigo,
    fecha = LocalDate.parse(fecha),
    dificultad = dificultad,
    residuos = residuos.mapNotNull(CatalogoResiduos::porId),
    categorias = categorias.toSet(),
    id = id,
)

private fun PartidaEnviada.aDto() = PartidaDto(
    idLocal = idLocal,
    retoId = retoId,
    terminada = terminada,
    aciertos = aciertos,
    errores = errores,
    rachaMaxima = rachaMaxima,
    tiempoMs = tiempoMs,
    intentos = intentos.map { IntentoDto(it.residuoId, it.categoriaElegida, it.correcto, it.momentoMs) },
)
