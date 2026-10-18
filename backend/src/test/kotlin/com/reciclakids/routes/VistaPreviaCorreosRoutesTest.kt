package com.reciclakids.routes

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VistaPreviaCorreosRoutesTest {

    // Que la vista previa se apague con la configuración lo prueba AplicacionTest con el servidor completo.
    private fun ApplicationTestBuilder.conVistaPrevia() {
        application { routing { rutasVistaPreviaCorreos() } }
    }

    @Test
    fun vistaPreviaDelCorreoDeLogro() = testApplication {
        conVistaPrevia()
        val respuesta = client.get("/correos/vista-previa/logro")
        assertEquals(HttpStatusCode.OK, respuesta.status)
        assertEquals(ContentType.Text.Html, respuesta.contentType()?.withoutParameters())
        val html = respuesta.bodyAsText()
        assertTrue("<html lang=\"es\"" in html)
        assertTrue("Salomé ganó una insignia" in html)
    }

    @Test
    fun vistaPreviaDelReporteSemanal() = testApplication {
        conVistaPrevia()
        val respuesta = client.get("/correos/vista-previa/reporte-semanal")
        assertEquals(HttpStatusCode.OK, respuesta.status)
        assertEquals(ContentType.Text.Html, respuesta.contentType()?.withoutParameters())
        assertTrue("Semana del 15 al 19 de septiembre" in respuesta.bodyAsText())
    }

    @Test
    fun vistaPreviaEnTextoPlano() = testApplication {
        conVistaPrevia()
        val respuesta = client.get("/correos/vista-previa/reporte-semanal?formato=texto")
        assertEquals(ContentType.Text.Plain, respuesta.contentType()?.withoutParameters())
        val texto = respuesta.bodyAsText()
        assertTrue(texto.startsWith("Asunto: El resumen de la semana de Salomé"))
        assertTrue("Retos: 4 de 5" in texto)
    }

}
