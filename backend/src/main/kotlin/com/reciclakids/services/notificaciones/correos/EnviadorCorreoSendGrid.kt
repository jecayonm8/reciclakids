package com.reciclakids.services.notificaciones.correos

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/** Un envío que SendGrid rechazó o que no se pudo completar. El mensaje nunca lleva destinatarios. */
class ErrorEnvioCorreo(mensaje: String) : RuntimeException(mensaje)

/**
 * Envío por SendGrid (API v3 de correo). Alternativa a Amazon SES (ADR-10) que no exige un dominio
 * propio: basta una clave de API con el permiso «Mail Send» y un remitente verificado en SendGrid
 * («Single Sender Verification»). Usa el cliente HTTP del JDK, sin dependencias nuevas.
 *
 * El registro solo lleva el estado HTTP: ni destinatario ni asunto, que nombran al niño
 * (Ley 1581 de 2012), ni la clave.
 */
class EnviadorCorreoSendGrid(
    private val claveApi: String,
    remitente: String,
    private val urlApi: String = UrlPorDefecto,
    private val cliente: HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
    private val pausaEntreReintentos: Duration = Duration.ofMillis(800),
) : EnviadorCorreo {

    private val log = LoggerFactory.getLogger(EnviadorCorreoSendGrid::class.java)
    private val remitente = DireccionRemitente.leer(remitente)

    init {
        require(claveApi.isNotBlank()) { "Falta la clave de API de SendGrid (SENDGRID_API_KEY)" }
    }

    override suspend fun enviar(destinatario: String, correo: CorreoRenderizado) = withContext(Dispatchers.IO) {
        val peticion = HttpRequest.newBuilder(URI.create(urlApi))
            .timeout(Duration.ofSeconds(20))
            .header("Authorization", "Bearer $claveApi")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(cuerpo(destinatario, correo)))
            .build()

        var intento = 0
        while (true) {
            intento++
            val estado = try {
                cliente.send(peticion, HttpResponse.BodyHandlers.discarding()).statusCode()
            } catch (error: IOException) {
                if (intento > Reintentos) throw ErrorEnvioCorreo("SendGrid no respondió (${error.javaClass.simpleName})")
                delay(pausaEntreReintentos.toMillis() * intento)
                continue
            }
            when {
                estado in 200..299 -> {
                    log.info("Correo enviado por SendGrid (estado {})", estado)
                    return@withContext
                }
                // 429 (límite de uso) y 5xx son pasajeros: se reintenta. El resto no se arregla reintentando.
                (estado == 429 || estado >= 500) && intento <= Reintentos -> delay(pausaEntreReintentos.toMillis() * intento)
                else -> throw ErrorEnvioCorreo("SendGrid rechazó el envío (estado $estado)${pista(estado)}")
            }
        }
    }

    private fun cuerpo(destinatario: String, correo: CorreoRenderizado): String {
        fun direccion(correoElectronico: String, nombre: String? = null) = JsonObject(
            buildMap {
                put("email", JsonPrimitive(correoElectronico))
                if (nombre != null) put("name", JsonPrimitive(nombre))
            },
        )
        return Json.encodeToString(
            JsonObject.serializer(),
            JsonObject(
                mapOf(
                    "personalizations" to JsonArray(listOf(JsonObject(mapOf("to" to JsonArray(listOf(direccion(destinatario))))))),
                    "from" to direccion(remitente.correo, remitente.nombre),
                    "subject" to JsonPrimitive(correo.asunto),
                    // SendGrid exige text/plain antes que text/html.
                    "content" to JsonArray(
                        listOf(
                            JsonObject(mapOf("type" to JsonPrimitive("text/plain"), "value" to JsonPrimitive(correo.textoPlano))),
                            JsonObject(mapOf("type" to JsonPrimitive("text/html"), "value" to JsonPrimitive(correo.html))),
                        ),
                    ),
                    // Sin seguimiento de clics ni de aperturas: los enlaces del correo (baja, recuperación)
                    // deben llegar tal cual, y no queremos medir qué lee cada familia.
                    "tracking_settings" to JsonObject(
                        mapOf(
                            "click_tracking" to JsonObject(mapOf("enable" to JsonPrimitive(false))),
                            "open_tracking" to JsonObject(mapOf("enable" to JsonPrimitive(false))),
                        ),
                    ),
                ),
            ),
        )
    }

    /** Qué revisar según el estado; ayuda a quien lee los registros de Cloud Run. */
    private fun pista(estado: Int): String = when (estado) {
        401 -> ": la clave de API no es válida"
        403 -> ": la clave no tiene el permiso «Mail Send» o el remitente no está verificado en SendGrid"
        else -> ""
    }

    companion object {
        const val UrlPorDefecto = "https://api.sendgrid.com/v3/mail/send"
        private const val Reintentos = 2
    }
}

/** «ReciclaKids <no-responder@dominio.co>» o solo «no-responder@dominio.co». */
data class DireccionRemitente(val correo: String, val nombre: String?) {
    companion object {
        private val ConNombre = Regex("^\\s*\"?([^<\"]*?)\"?\\s*<\\s*([^<>\\s@]+@[^<>\\s@]+\\.[^<>\\s@]+)\\s*>\\s*$")
        private val Sola = Regex("^\\s*([^<>\\s@]+@[^<>\\s@]+\\.[^<>\\s@]+)\\s*$")

        fun leer(valor: String): DireccionRemitente {
            ConNombre.matchEntire(valor)?.let { return DireccionRemitente(it.groupValues[2], it.groupValues[1].ifBlank { null }) }
            Sola.matchEntire(valor)?.let { return DireccionRemitente(it.groupValues[1], null) }
            throw IllegalArgumentException("CORREO_REMITENTE no es una dirección válida: use «Nombre <correo@dominio>» o «correo@dominio»")
        }
    }
}
