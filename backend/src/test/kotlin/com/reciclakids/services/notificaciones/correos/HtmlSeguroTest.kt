package com.reciclakids.services.notificaciones.correos

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HtmlSeguroTest {

    @Test
    fun escapaLosCaracteresEspecialesDeHtml() {
        assertEquals(
            "&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt; &amp; &quot;comillas&quot;",
            escaparHtml("<script>alert('x')</script> & \"comillas\""),
        )
        assertEquals("Salomé · Jardín B", escaparHtml("Salomé · Jardín B"))
    }

    @Test
    fun aceptaEnlacesHttpYHttps() {
        listOf(
            "https://reciclakids.example/padres/progreso",
            "http://localhost:8080/correos",
            "HTTPS://reciclakids.example/a?b=1&c=2",
        ).forEach { assertEquals(it, validarEnlace(it)) }
        assertEquals("https://reciclakids.example", validarEnlace("  https://reciclakids.example  "))
    }

    @Test
    fun rechazaEnlacesQueNoSonHttpNiHttps() {
        listOf(
            "javascript:alert(1)",
            "JavaScript:alert(1)",
            " javascript:alert(1)",
            "data:text/html,<script>alert(1)</script>",
            "mailto:mariana.r@correo.com",
            "/padres/progreso",
            "reciclakids.example/padres",
            "https://",
            "https:///sin-servidor",
            "https://reciclakids.example/con espacio",
            "",
        ).forEach { enlace ->
            assertFailsWith<IllegalArgumentException>(enlace) { validarEnlace(enlace) }
        }
    }

    @Test
    fun elEnlaceSeEscapaParaElAtributo() {
        assertEquals(
            "https://reciclakids.example/a?b=1&amp;c=2",
            atributoEnlace("https://reciclakids.example/a?b=1&c=2"),
        )
    }

    @Test
    fun enUnaLineaQuitaSaltosYControlesPeroRespetaElEspacioFijo() {
        assertEquals("Salomé Bcc: otro@correo.com", enUnaLinea(" Salomé\r\nBcc:\totro@correo.com "))
        assertEquals("88${EspacioFijo}%", enUnaLinea("88${EspacioFijo}%"))
        assertEquals(" separando ", sinSaltos(" separando\n"))
    }
}
