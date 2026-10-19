package com.reciclakids.services.notificaciones.correos

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.InetSocketAddress
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Un SendGrid de mentira: un servidor HTTP local que guarda lo que recibe y responde lo que se le diga. */
class EnviadorCorreoSendGridTest {

    private class Recibida(val ruta: String, val autorizacion: String?, val tipo: String?, val cuerpo: String)

    private val recibidas = CopyOnWriteArrayList<Recibida>()
    private val respuestas = ArrayDeque<Int>()
    private val servidor: HttpServer = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
        createContext("/") { intercambio ->
            recibidas += Recibida(
                ruta = intercambio.requestURI.path,
                autorizacion = intercambio.requestHeaders.getFirst("Authorization"),
                tipo = intercambio.requestHeaders.getFirst("Content-Type"),
                cuerpo = intercambio.requestBody.readBytes().decodeToString(),
            )
            val estado = synchronized(respuestas) { respuestas.removeFirstOrNull() ?: 202 }
            intercambio.sendResponseHeaders(estado, -1)
            intercambio.close()
        }
        start()
    }

    private val url get() = "http://127.0.0.1:${servidor.address.port}/v3/mail/send"

    private fun enviador(remitente: String = "ReciclaKids <no-responder@reciclakids.co>") = EnviadorCorreoSendGrid(
        claveApi = "SG.clave-secreta-de-prueba",
        remitente = remitente,
        urlApi = url,
        pausaEntreReintentos = Duration.ofMillis(1),
    )

    private val correo = CorreoRenderizado(
        asunto = "Salomé ganó la insignia «Racha de 5»",
        html = "<html><body><p>Hola</p></body></html>",
        textoPlano = "Hola",
    )

    @AfterTest
    fun cerrar() = servidor.stop(0)

    @Test
    fun mandaElCorreoALaApiDeSendGridConLaClaveYElContenidoCompleto() = runBlocking {
        enviador().enviar("marta@casa.co", correo)

        val recibida = recibidas.single()
        assertEquals("/v3/mail/send", recibida.ruta)
        assertEquals("Bearer SG.clave-secreta-de-prueba", recibida.autorizacion)
        assertEquals("application/json", recibida.tipo)

        val json = Json.parseToJsonElement(recibida.cuerpo).jsonObject
        val destinatario = json.getValue("personalizations").jsonArray.single().jsonObject.getValue("to").jsonArray.single().jsonObject
        assertEquals("marta@casa.co", destinatario.getValue("email").jsonPrimitive.content)
        val de = json.getValue("from").jsonObject
        assertEquals("no-responder@reciclakids.co", de.getValue("email").jsonPrimitive.content)
        assertEquals("ReciclaKids", de.getValue("name").jsonPrimitive.content)
        assertEquals("Salomé ganó la insignia «Racha de 5»", json.getValue("subject").jsonPrimitive.content)
        // SendGrid exige text/plain primero.
        val contenido = json.getValue("content").jsonArray.map { it.jsonObject }
        assertEquals(listOf("text/plain", "text/html"), contenido.map { it.getValue("type").jsonPrimitive.content })
        assertEquals("Hola", contenido[0].getValue("value").jsonPrimitive.content)
        assertTrue("<p>Hola</p>" in contenido[1].getValue("value").jsonPrimitive.content)
        // Los enlaces de baja y recuperación deben llegar sin pasar por el redireccionador de clics.
        val seguimiento = json.getValue("tracking_settings").jsonObject
        assertEquals(false, seguimiento.getValue("click_tracking").jsonObject.getValue("enable").jsonPrimitive.boolean)
        assertEquals(false, seguimiento.getValue("open_tracking").jsonObject.getValue("enable").jsonPrimitive.boolean)
    }

    @Test
    fun elRemitenteSinNombreVaSoloConSuCorreo() = runBlocking {
        enviador("no-responder@reciclakids.co").enviar("a@b.co", correo)
        val de = Json.parseToJsonElement(recibidas.single().cuerpo).jsonObject.getValue("from").jsonObject
        assertEquals("no-responder@reciclakids.co", de.getValue("email").jsonPrimitive.content)
        assertNull(de["name"])
    }

    @Test
    fun reintentaConLimiteDeUsoYFallasDelServidorYLuegoLoLogra() = runBlocking {
        synchronized(respuestas) { respuestas += listOf(429, 503) }
        enviador().enviar("a@b.co", correo)
        assertEquals(3, recibidas.size)
    }

    @Test
    fun siSendGridSigueFallandoSeRindeSinDecirAQuienSeEnviaba() {
        synchronized(respuestas) { respuestas += listOf(500, 500, 500) }
        val error = assertFailsWith<ErrorEnvioCorreo> { runBlocking { enviador().enviar("secreto@casa.co", correo) } }
        assertEquals(3, recibidas.size) // el intento original y dos reintentos
        assertTrue("500" in error.message.orEmpty())
        assertTrue("secreto@casa.co" !in error.message.orEmpty())
        assertTrue("SG." !in error.message.orEmpty())
    }

    @Test
    fun unaClaveSinPermisoSeExplicaYNoSeReintenta() {
        synchronized(respuestas) { respuestas += 403 }
        val error = assertFailsWith<ErrorEnvioCorreo> { runBlocking { enviador().enviar("a@b.co", correo) } }
        assertEquals(1, recibidas.size)
        assertTrue("Mail Send" in error.message.orEmpty())
        assertTrue("verificado" in error.message.orEmpty())
    }

    @Test
    fun unaClaveInvalidaSeDiceClaro() {
        synchronized(respuestas) { respuestas += 401 }
        val error = assertFailsWith<ErrorEnvioCorreo> { runBlocking { enviador().enviar("a@b.co", correo) } }
        assertEquals(1, recibidas.size)
        assertTrue("clave de API" in error.message.orEmpty())
    }

    @Test
    fun siNadaResponde() {
        servidor.stop(0)
        val error = assertFailsWith<ErrorEnvioCorreo> { runBlocking { enviador().enviar("a@b.co", correo) } }
        assertTrue("no respondió" in error.message.orEmpty())
    }

    @Test
    fun noArrancaSinClaveNiConUnRemitenteQueNoEsUnCorreo() {
        assertFailsWith<IllegalArgumentException> { EnviadorCorreoSendGrid(" ", "a@b.co") }
        assertFailsWith<IllegalArgumentException> { EnviadorCorreoSendGrid("SG.x", "no-es-un-correo") }
        assertFailsWith<IllegalArgumentException> { EnviadorCorreoSendGrid("SG.x", "ReciclaKids <sin-arroba>") }
    }

    @Test
    fun leeLosFormatosDeRemitente() {
        assertEquals(DireccionRemitente("a@b.co", "ReciclaKids"), DireccionRemitente.leer("ReciclaKids <a@b.co>"))
        assertEquals(DireccionRemitente("a@b.co", "ReciclaKids"), DireccionRemitente.leer("\"ReciclaKids\" <a@b.co>"))
        assertEquals(DireccionRemitente("a@b.co", null), DireccionRemitente.leer("a@b.co"))
        assertEquals(DireccionRemitente("a@b.co", null), DireccionRemitente.leer(" <a@b.co> "))
        assertNotNull(DireccionRemitente.leer("Nombre Con Espacios <correo.largo+etiqueta@sub.dominio.co>"))
    }

    @Test
    fun unAsuntoConComillasNoRompeElJson() {
        // El cuerpo se arma con JsonObject, sin plantillas de texto: un asunto con comillas no rompe el JSON.
        val raro = CorreoRenderizado("Asunto con \"comillas\" y \\ barra", "<p>\"hola\"</p>", "línea 1\nlínea 2")
        runBlocking { enviador().enviar("a@b.co", raro) }
        val json = Json.parseToJsonElement(recibidas.single().cuerpo).jsonObject
        assertEquals("Asunto con \"comillas\" y \\ barra", json.getValue("subject").jsonPrimitive.content)
        assertEquals("<p>\"hola\"</p>", json.getValue("content").jsonArray[1].jsonObject.getValue("value").jsonPrimitive.content)
    }
}
