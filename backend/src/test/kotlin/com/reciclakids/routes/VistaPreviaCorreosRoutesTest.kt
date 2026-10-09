package com.reciclakids.routes

import com.example.com.reciclakids.plugins.configureRouting
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VistaPreviaCorreosRoutesTest {

    private fun ApplicationTestBuilder.conVistaPrevia(activa: Boolean) {
        environment { config = MapApplicationConfig("reciclakids.correos.vistaPrevia" to activa.toString()) }
        application { configureRouting() }
    }

    @Test
    fun laConfiguracionPorDefectoEnciendeLaVistaPrevia() = testApplication {
        configure()
        assertEquals(HttpStatusCode.OK, client.get("/correos/vista-previa/logro").status)
        assertEquals(HttpStatusCode.OK, client.get("/").status)
    }

    @Test
    fun vistaPreviaDelCorreoDeLogro() = testApplication {
        conVistaPrevia(activa = true)
        val respuesta = client.get("/correos/vista-previa/logro")
        assertEquals(HttpStatusCode.OK, respuesta.status)
        assertEquals(ContentType.Text.Html, respuesta.contentType()?.withoutParameters())
        val html = respuesta.bodyAsText()
        assertTrue("<html lang=\"es\"" in html)
        assertTrue("Salomé ganó una insignia" in html)
    }

    @Test
    fun vistaPreviaDelReporteSemanal() = testApplication {
        conVistaPrevia(activa = true)
        val respuesta = client.get("/correos/vista-previa/reporte-semanal")
        assertEquals(HttpStatusCode.OK, respuesta.status)
        assertEquals(ContentType.Text.Html, respuesta.contentType()?.withoutParameters())
        assertTrue("Semana del 15 al 19 de septiembre" in respuesta.bodyAsText())
    }

    @Test
    fun vistaPreviaEnTextoPlano() = testApplication {
        conVistaPrevia(activa = true)
        val respuesta = client.get("/correos/vista-previa/reporte-semanal?formato=texto")
        assertEquals(ContentType.Text.Plain, respuesta.contentType()?.withoutParameters())
        val texto = respuesta.bodyAsText()
        assertTrue(texto.startsWith("Asunto: El resumen de la semana de Salomé"))
        assertTrue("Retos: 4 de 5" in texto)
    }

    @Test
    fun apagadaNoExponeLasRutas() = testApplication {
        conVistaPrevia(activa = false)
        assertEquals(HttpStatusCode.NotFound, client.get("/correos/vista-previa/logro").status)
        assertEquals(HttpStatusCode.NotFound, client.get("/correos/vista-previa/reporte-semanal").status)
        assertEquals(HttpStatusCode.OK, client.get("/").status)
    }
}
