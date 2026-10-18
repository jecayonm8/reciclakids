package com.reciclakids.network.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.reflect.TypeInfo
import io.ktor.util.reflect.typeInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlin.coroutines.CoroutineContext

/** Respuesta de la API ya clasificada para que cada servicio la traduzca a sus resultados. */
sealed interface RespuestaApi<out T> {
    data class Exito<out T>(val dato: T) : RespuestaApi<T>

    /** 4xx con el código estable del cuerpo `{"error", "mensaje"}`. */
    data class Rechazo(val estado: Int, val error: String) : RespuestaApi<Nothing>

    /** Falla de red, tiempo agotado o error del servidor (5xx): se puede reintentar más tarde. */
    data object SinConexion : RespuestaApi<Nothing>

    /** La sesión del adulto venció y no se pudo refrescar: hay que iniciar sesión de nuevo. */
    data object SesionVencida : RespuestaApi<Nothing>
}

/** Con qué se identifica una petición. */
sealed interface Credencial {
    data object Ninguna : Credencial

    /** El token de acceso del adulto con sesión; ante un 401 se refresca solo una vez. */
    data object Adulto : Credencial

    /** El token de un niño, para sus resultados (dura una semana). */
    data class Nino(val token: String) : Credencial
}

/** Formato JSON de la API: el mismo del backend (plugins/Serializacion.kt). */
val JsonApi = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}

/**
 * Cliente HTTP de la API REST /api/v1 (ADR-07). Clasifica cada respuesta en [RespuestaApi] y lleva
 * la sesión de los adultos: añade el token de acceso, lo refresca ante un 401 (la API rota el de
 * refresco en cada uso) y, si el refresco falla, borra la sesión y avisa por [sesionVencida].
 */
class ClienteApi(
    urlBase: String,
    motor: HttpClientEngine,
    private val sesion: SesionAdultoGuardada,
    /** Las peticiones, el cifrado de los tokens y el JSON van fuera del hilo principal. */
    private val despachador: CoroutineContext = Dispatchers.IO,
) {
    private val http = HttpClient(motor) {
        expectSuccess = false
        install(ContentNegotiation) { json(JsonApi) }
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 20_000
            requestTimeoutMillis = 30_000
        }
        defaultRequest { url(urlBase) }
    }

    private val refresco = Mutex()
    private val vencidas = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Emite cuando la sesión del adulto venció: la app vuelve al inicio de sesión. */
    val sesionVencida: SharedFlow<Unit> = vencidas.asSharedFlow()

    suspend inline fun <reified T> get(ruta: String, credencial: Credencial = Credencial.Adulto): RespuestaApi<T> =
        pedir(typeInfo<T>(), credencial) {
            method = HttpMethod.Get
            url(ruta)
        }

    suspend inline fun <reified T> post(ruta: String, cuerpo: Any? = null, credencial: Credencial = Credencial.Adulto): RespuestaApi<T> =
        pedir(typeInfo<T>(), credencial) { conCuerpo(HttpMethod.Post, ruta, cuerpo) }

    suspend inline fun <reified T> put(ruta: String, cuerpo: Any, credencial: Credencial = Credencial.Adulto): RespuestaApi<T> =
        pedir(typeInfo<T>(), credencial) { conCuerpo(HttpMethod.Put, ruta, cuerpo) }

    suspend inline fun <reified T> delete(ruta: String, credencial: Credencial = Credencial.Adulto): RespuestaApi<T> =
        pedir(typeInfo<T>(), credencial) {
            method = HttpMethod.Delete
            url(ruta)
        }

    /** [armar] pone el método, la ruta (relativa a la URL base, sin «/» inicial) y el cuerpo. */
    suspend fun <T> pedir(tipo: TypeInfo, credencial: Credencial, armar: HttpRequestBuilder.() -> Unit): RespuestaApi<T> =
        withContext(despachador) {
            when (credencial) {
                Credencial.Ninguna -> enviar(armar)?.let { clasificar<T>(it, tipo) } ?: RespuestaApi.SinConexion
                is Credencial.Nino -> enviar {
                    armar()
                    bearerAuth(credencial.token)
                }?.let { clasificar<T>(it, tipo) } ?: RespuestaApi.SinConexion
                Credencial.Adulto -> comoAdulto(tipo, armar)
            }
        }

    private suspend fun <T> comoAdulto(tipo: TypeInfo, armar: HttpRequestBuilder.() -> Unit): RespuestaApi<T> {
        val token = sesion.leer()?.tokenAcceso ?: return vencida()
        val primera = enviar {
            armar()
            bearerAuth(token)
        } ?: return RespuestaApi.SinConexion
        if (primera.status != HttpStatusCode.Unauthorized) return clasificar(primera, tipo)
        return when (refrescar(token)) {
            Refresco.Listo -> {
                val nuevo = sesion.leer()?.tokenAcceso ?: return vencida()
                val segunda = enviar {
                    armar()
                    bearerAuth(nuevo)
                } ?: return RespuestaApi.SinConexion
                if (segunda.status == HttpStatusCode.Unauthorized) vencida() else clasificar(segunda, tipo)
            }
            Refresco.SinConexion -> RespuestaApi.SinConexion
            Refresco.Vencida -> vencida()
        }
    }

    private enum class Refresco { Listo, SinConexion, Vencida }

    /**
     * Cambia el token de refresco por uno nuevo. Las peticiones que recibieron 401 a la vez pasan
     * de a una: si otra ya refrescó mientras esta esperaba, usa ese token y no vuelve a rotar
     * (reusar un token de refresco viejo cierra todas las sesiones de la cuenta).
     */
    private suspend fun refrescar(tokenRechazado: String): Refresco = refresco.withLock {
        val actual = sesion.leer() ?: return Refresco.Vencida
        if (actual.tokenAcceso != tokenRechazado) return Refresco.Listo
        val respuesta = enviar { conCuerpo(HttpMethod.Post, "auth/refresco", RefrescoSolicitud(actual.tokenRefresco)) }
            ?: return Refresco.SinConexion
        when {
            respuesta.status.isSuccess() -> {
                val nueva = leerCuerpo<SesionDto>(respuesta, typeInfo<SesionDto>()) ?: return Refresco.SinConexion
                sesion.guardar(nueva)
                Refresco.Listo
            }
            respuesta.status.value >= 500 -> Refresco.SinConexion
            else -> Refresco.Vencida
        }
    }

    private fun vencida(): RespuestaApi<Nothing> {
        sesion.borrar()
        vencidas.tryEmit(Unit)
        return RespuestaApi.SesionVencida
    }

    /** `null` si no hubo respuesta del servidor: sin red, sin DNS o tiempo agotado. */
    private suspend fun enviar(armar: HttpRequestBuilder.() -> Unit): HttpResponse? = try {
        http.request(armar)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    private suspend fun <T> clasificar(respuesta: HttpResponse, tipo: TypeInfo): RespuestaApi<T> {
        val estado = respuesta.status.value
        return when {
            respuesta.status.isSuccess() -> leerCuerpo<T>(respuesta, tipo)?.let { RespuestaApi.Exito(it) } ?: RespuestaApi.SinConexion
            estado >= 500 -> RespuestaApi.SinConexion
            else -> RespuestaApi.Rechazo(estado, leerCuerpo<ErrorDto>(respuesta, typeInfo<ErrorDto>())?.error.orEmpty())
        }
    }

    /** `null` si el cuerpo no tiene la forma esperada: se trata como una falla del servidor. */
    @Suppress("UNCHECKED_CAST")
    private suspend fun <T> leerCuerpo(respuesta: HttpResponse, tipo: TypeInfo): T? = try {
        if (tipo.type == Unit::class) Unit as T else respuesta.body<T>(tipo)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
}

/** Método, ruta y cuerpo JSON de una petición. */
fun HttpRequestBuilder.conCuerpo(metodo: HttpMethod, ruta: String, cuerpo: Any?) {
    method = metodo
    url(ruta)
    if (cuerpo != null) {
        contentType(ContentType.Application.Json)
        setBody(cuerpo)
    }
}
