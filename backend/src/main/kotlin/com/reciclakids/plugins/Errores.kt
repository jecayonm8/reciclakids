package com.reciclakids.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

/** Cuerpo de todo error de la API: un código estable para la app y un mensaje en español. */
@Serializable
data class CuerpoError(val error: String, val mensaje: String)

/** Error esperado de la API: la ruta lo lanza y [configurarErrores] lo convierte en respuesta. */
open class ErrorApi(val estado: HttpStatusCode, val codigo: String, mensaje: String) : RuntimeException(mensaje)

class NoEncontrado(codigo: String = "no_encontrado", mensaje: String = "No existe") : ErrorApi(HttpStatusCode.NotFound, codigo, mensaje)
class Conflicto(codigo: String, mensaje: String) : ErrorApi(HttpStatusCode.Conflict, codigo, mensaje)
class DatosInvalidos(mensaje: String, codigo: String = "datos_invalidos") : ErrorApi(HttpStatusCode.UnprocessableEntity, codigo, mensaje)
class Prohibido(mensaje: String = "No tienes acceso a este recurso") : ErrorApi(HttpStatusCode.Forbidden, "prohibido", mensaje)
class NoAutenticado(codigo: String = "no_autenticado", mensaje: String = "Inicia sesión de nuevo") :
    ErrorApi(HttpStatusCode.Unauthorized, codigo, mensaje)

fun Application.configurarErrores() {
    val log = LoggerFactory.getLogger("com.reciclakids.Errores")
    install(StatusPages) {
        exception<ErrorApi> { call, error ->
            call.respond(error.estado, CuerpoError(error.codigo, error.message ?: error.codigo))
        }
        exception<BadRequestException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, CuerpoError("peticion_invalida", "La petición no tiene el formato esperado"))
        }
        exception<IllegalArgumentException> { call, error ->
            call.respond(HttpStatusCode.UnprocessableEntity, CuerpoError("datos_invalidos", error.message ?: "Datos inválidos"))
        }
        exception<Throwable> { call, error ->
            // Nunca se devuelve el detalle: puede llevar datos de menores o del esquema.
            log.error("Error no controlado en {}", call.request.local.uri, error)
            call.respond(HttpStatusCode.InternalServerError, CuerpoError("error_interno", "Algo falló en el servidor"))
        }
        status(HttpStatusCode.Unauthorized) { call, estado ->
            call.respond(estado, CuerpoError("no_autenticado", "Inicia sesión de nuevo"))
        }
        status(HttpStatusCode.TooManyRequests) { call, estado ->
            call.respond(estado, CuerpoError("demasiados_intentos", "Espera un momento antes de volver a intentarlo"))
        }
    }
}
