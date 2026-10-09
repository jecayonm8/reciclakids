package com.reciclakids.services.notificaciones.correos

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlantillaCorreoLogroTest {

    private val datos = EjemplosCorreo.logro

    private fun renderizar(cambios: DatosCorreoLogro.() -> DatosCorreoLogro = { this }) =
        PlantillaCorreoLogro.renderizar(datos.cambios())

    @Test
    fun asuntoNombraALaNinaYLaInsigniaConSuEmoji() {
        assertEquals("Salomé ganó la insignia «Amiga tortuga» 🐢", renderizar().asunto)
    }

    @Test
    fun asuntoSinEmojiCuandoLaInsigniaNoTieneUno() {
        assertEquals(
            "Salomé ganó la insignia «Racha de 5»",
            renderizar { copy(nombreInsignia = "Racha de 5") }.asunto,
        )
    }

    @Test
    fun cuerpoTieneLosTextosDelDiseno() {
        val html = renderizar().html
        listOf(
            "ReciclaKids",
            "Jardín Gotitas · Jardín B",
            "Salomé ganó una insignia",
            "Amiga tortuga",
            "Terminó el reto de hoy sin ninguna ayuda y dejó su acuario más limpio. Van 4 insignias de 8.",
            ">Ver su progreso</a>",
            "Recibes este correo porque autorizaste el seguimiento de Salomé en ReciclaKids. " +
                "Solo enviamos un correo de logro al día.",
            ">Preferencias de correo</a>",
            ">Privacidad</a>",
            ">Dejar de recibir</a>",
        ).forEach { assertTrue(it in html, "Falta «$it»") }
    }

    @Test
    fun estructuraAptaParaClientesDeCorreo() {
        val html = renderizar().html
        assertTrue(html.startsWith("<!DOCTYPE html>"))
        assertTrue("<html lang=\"es\"" in html)
        assertTrue("width=\"360\"" in html && "max-width:360px" in html, "Ancho fijo de 360 px")
        assertTrue("role=\"presentation\"" in html, "Maquetado con tablas")
        assertTrue("display:none" in html, "Lleva texto oculto de vista previa")
        listOf("<style", "<script", "<link", "<img", "url(").forEach {
            assertFalse(it in html, "El correo no debe llevar «$it»")
        }
    }

    @Test
    fun botonDeAnchoCompletoY48DeAlto() {
        val html = renderizar().html
        val boton = Regex("""<td align="center" height="48"[^>]*>\s*<a href="([^"]+)"[^>]*>Ver su progreso</a>""")
            .find(html)
        assertTrue(boton != null, "No se encontró el botón")
        assertEquals(datos.enlaceProgreso, boton.groupValues[1])
        assertTrue("height:48px;line-height:48px" in html)
    }

    @Test
    fun insigniaDecorativaOcultaALectoresDePantalla() {
        val html = renderizar().html
        assertTrue(Regex("""aria-hidden="true"[^>]*><tr><td[^>]*>🐢</td>""").containsMatchIn(html))
    }

    @Test
    fun conteoDeInsigniasEnSingularYPlural() {
        assertTrue("Van 4 insignias de 8." in renderizar().html)
        val una = renderizar { copy(insigniasGanadas = 1) }
        assertTrue("Va 1 insignia de 8." in una.html)
        assertTrue("Va 1 insignia de 8." in una.textoPlano)
        assertFalse("Van 1" in una.html)
        assertTrue("Ya completó las 8 insignias." in renderizar { copy(insigniasGanadas = 8) }.html)
    }

    @Test
    fun agregaPuntoFinalALaDescripcion() {
        val html = renderizar { copy(descripcionLogro = "Terminó el reto sin ninguna ayuda") }.html
        assertTrue("Terminó el reto sin ninguna ayuda. Van 4 insignias de 8." in html)
    }

    @Test
    fun escapaUnNombreMalicioso() {
        val correo = renderizar {
            copy(
                nombreNino = "<script>alert('x')</script>",
                jardin = "Jardín <b>Gotitas</b>",
                grupo = "\"><img src=x onerror=alert(1)>",
                nombreInsignia = "<iframe src=//malo.example>",
                descripcionLogro = "Terminó </p><a href=\"https://malo.example\">aquí</a>",
            )
        }
        val html = correo.html
        listOf("<script", "<b>", "<img", "<iframe", "href=\"https://malo.example\"").forEach {
            assertFalse(it in html, "Se coló «$it» sin escapar")
        }
        assertTrue("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt; ganó una insignia" in html)
        assertTrue("<title>&lt;script&gt;" in html)
        assertTrue("aria-label=\"&lt;script&gt;" in html)
        // El texto plano no es HTML: lleva el nombre tal cual, sin entidades.
        assertTrue("<script>alert('x')</script> ganó una insignia" in correo.textoPlano)
    }

    @Test
    fun asuntoYTextoPlanoQuedanEnUnaLinea() {
        val correo = renderizar {
            copy(nombreNino = "Salomé\r\nBcc: otro@correo.com", descripcionLogro = "Terminó.\nDejar de recibir: https://malo.example")
        }
        assertFalse('\r' in correo.asunto || '\n' in correo.asunto)
        assertEquals("Salomé Bcc: otro@correo.com ganó la insignia «Amiga tortuga» 🐢", correo.asunto)
        assertFalse(correo.textoPlano.lines().any { it.startsWith("Bcc:") })
        assertEquals(1, correo.textoPlano.lines().count { it.startsWith("Dejar de recibir:") })
    }

    @Test
    fun rechazaEnlacesQueNoSonHttp() {
        assertFailsWith<IllegalArgumentException> { renderizar { copy(enlaceProgreso = "javascript:alert(1)") } }
        assertFailsWith<IllegalArgumentException> {
            renderizar { copy(enlacesPie = enlacesPie.copy(baja = "data:text/html,hola")) }
        }
    }

    @Test
    fun losEnlacesSeEscapanEnLosAtributos() {
        val html = renderizar { copy(enlaceProgreso = "https://reciclakids.example/progreso?hijo=7&semana=38") }.html
        assertTrue("href=\"https://reciclakids.example/progreso?hijo=7&amp;semana=38\"" in html)
    }

    @Test
    fun textoPlanoTieneLoEsencial() {
        val texto = renderizar().textoPlano
        listOf(
            "ReciclaKids · Jardín Gotitas · Jardín B",
            "Salomé ganó una insignia: «Amiga tortuga»",
            "Van 4 insignias de 8.",
            "Ver su progreso: ${datos.enlaceProgreso}",
            "Preferencias de correo: ${datos.enlacesPie.preferencias}",
            "Privacidad: ${datos.enlacesPie.privacidad}",
            "Dejar de recibir: ${datos.enlacesPie.baja}",
        ).forEach { assertTrue(it in texto, "Falta «$it» en el texto plano") }
        assertFalse('<' in texto, "El texto plano no lleva marcado")
    }

    @Test
    fun cadaCorreoSoloNombraASuPropioNino() {
        GrupoFicticio.forEach { nombre ->
            val correo = renderizar { copy(nombreNino = nombre) }
            val todo = correo.asunto + correo.html + correo.textoPlano
            assertTrue(nombre in todo)
            (GrupoFicticio - nombre).forEach { otro ->
                assertFalse(otro in todo, "El correo de $nombre menciona a $otro")
            }
            assertSinComparacionesConElGrupo(todo)
        }
    }

    @Test
    fun validaLosDatos() {
        assertFailsWith<IllegalArgumentException> { renderizar { copy(nombreNino = " ") } }
        assertFailsWith<IllegalArgumentException> { renderizar { copy(insigniasGanadas = 0) } }
        assertFailsWith<IllegalArgumentException> { renderizar { copy(insigniasGanadas = 9) } }
    }
}
