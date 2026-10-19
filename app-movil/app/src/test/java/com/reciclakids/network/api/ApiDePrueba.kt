package com.reciclakids.network.api

import com.reciclakids.local.AlmacenSeguro
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/** Lo que la app mandó al backend falso. [ruta] va sin «/api/v1/». */
data class Peticion(val metodo: HttpMethod, val ruta: String, val consulta: String, val cuerpo: String, val token: String?) {
    val json: JsonObject get() = Json.parseToJsonElement(cuerpo).jsonObject
}

class AlmacenEnMemoria : AlmacenSeguro {
    val valores = mutableMapOf<String, String>()
    override fun leer(clave: String): String? = valores[clave]
    override fun guardar(clave: String, valor: String) {
        valores[clave] = valor
    }
    override fun borrar(clave: String) {
        valores.remove(clave)
    }
}

/**
 * Un backend falso con MockEngine: [responder] decide la respuesta de cada petición y quedan
 * registradas en [peticiones] para revisar ruta, cuerpo y token.
 */
class ApiDePrueba(private val responder: suspend MockRequestHandleScope.(Peticion) -> HttpResponseData) {
    val peticiones = mutableListOf<Peticion>()
    val almacen = AlmacenEnMemoria()
    val sesion = SesionAdultoGuardada(almacen)
    val cliente = ClienteApi(
        urlBase = "http://backend.prueba/api/v1/",
        motor = MockEngine { solicitud -> responder(registrar(solicitud)) },
        sesion = sesion,
        despachador = Dispatchers.Unconfined,
    )

    fun rutas(): List<String> = peticiones.map { "${it.metodo.value} ${it.ruta}" }

    /** Deja abierta la sesión de un adulto como si hubiera iniciado sesión. */
    fun conSesion(acceso: String = "acceso-1", refresco: String = "refresco-1", cuenta: CuentaDto = CuentaDocente) {
        sesion.guardar(SesionDto(acceso, 3600, refresco, cuenta))
    }

    private fun registrar(solicitud: HttpRequestData): Peticion {
        val peticion = Peticion(
            metodo = solicitud.method,
            ruta = solicitud.url.encodedPath.removePrefix("/api/v1/"),
            consulta = solicitud.url.encodedQuery,
            cuerpo = (solicitud.body as? TextContent)?.text.orEmpty(),
            token = solicitud.headers[HttpHeaders.Authorization]?.removePrefix("Bearer "),
        )
        synchronized(peticiones) { peticiones += peticion }
        return peticion
    }

    companion object {
        val CuentaDocente = CuentaDto("Laura Gómez", "laura@jardin.edu.co", com.reciclakids.model.RolAdulto.Docente, "Jardín Arcoíris", "Jardín B")
        val CuentaAcudiente = CuentaDto("Marta Ruiz", "marta@correo.com", com.reciclakids.model.RolAdulto.Acudiente)
    }
}

fun MockRequestHandleScope.json(cuerpo: String, estado: HttpStatusCode = HttpStatusCode.OK): HttpResponseData =
    respond(cuerpo, estado, headersOf(HttpHeaders.ContentType, "application/json"))

fun MockRequestHandleScope.vacia(estado: HttpStatusCode = HttpStatusCode.NoContent): HttpResponseData = respond("", estado)

fun MockRequestHandleScope.error(estado: HttpStatusCode, codigo: String): HttpResponseData =
    json("""{"error":"$codigo","mensaje":"Mensaje en español"}""", estado)

/** El JSON de una sesión como lo devuelve POST /auth/sesion y /auth/refresco. */
fun sesionJson(acceso: String, refresco: String, rol: String = "Docente") =
    """{"tokenAcceso":"$acceso","venceEnSegundos":3600,"tokenRefresco":"$refresco",""" +
        """"cuenta":{"nombre":"Laura Gómez","correo":"laura@jardin.edu.co","rol":"$rol","jardin":"Jardín Arcoíris","grupo":"Jardín B"}}"""
