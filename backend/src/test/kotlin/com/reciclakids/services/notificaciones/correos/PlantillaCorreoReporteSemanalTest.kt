package com.reciclakids.services.notificaciones.correos

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlantillaCorreoReporteSemanalTest {

    private val datos = EjemplosCorreo.reporteSemanal

    private fun renderizar(cambios: DatosCorreoReporteSemanal.() -> DatosCorreoReporteSemanal = { this }) =
        PlantillaCorreoReporteSemanal.renderizar(datos.cambios())

    private fun pct(valor: Int) = formatearPorcentaje(valor)

    private fun tiempo(minutos: Int, dias: Int, limite: Int?) =
        PlantillaCorreoReporteSemanal.fraseTiempoDeJuego(TiempoDeJuego(minutos, dias, limite))

    @Test
    fun asuntoDelResumen() {
        assertEquals("El resumen de la semana de Salomé", renderizar().asunto)
    }

    @Test
    fun encabezadoConElRangoDeLaSemana() {
        assertTrue(">Semana del 15 al 19 de septiembre</p>" in renderizar().html)
        val entreMeses = renderizar {
            copy(inicioSemana = LocalDate.of(2025, 9, 29), finSemana = LocalDate.of(2025, 10, 3))
        }
        assertTrue("Semana del 29 de septiembre al 3 de octubre" in entreMeses.html)
        assertTrue("Semana del 29 de septiembre al 3 de octubre" in entreMeses.textoPlano)
    }

    @Test
    fun cifrasDelResumen() {
        val html = renderizar().html
        assertTrue(Regex(""">4/5</p><p[^>]*>retos</p>""").containsMatchIn(html))
        assertTrue(Regex("""color:#2E6B45;[^>]*>${pct(88)}</p><p[^>]*>aciertos</p>""").containsMatchIn(html))
        assertTrue(Regex(""">5</p><p[^>]*>racha</p>""").containsMatchIn(html))
    }

    @Test
    fun aciertosBajosNoVanEnVerde() {
        val html = renderizar { copy(porcentajeAciertos = 70) }.html
        assertTrue(Regex("""color:#171C1E;[^>]*>${pct(70)}</p>""").containsMatchIn(html))
    }

    @Test
    fun mensajeInterpretativoResaltaLaCategoria() {
        val correo = renderizar()
        assertTrue(
            "Esta semana mejoró separando <strong style=\"font-weight:700;\">orgánicos</strong>: " +
                "de ${pct(74)} a ${pct(92)} de aciertos." in correo.html,
        )
        assertTrue("background-color:#B6F0C6" in correo.html && "color:#0B2612" in correo.html)
        assertTrue("Esta semana mejoró separando orgánicos: de ${pct(74)} a ${pct(92)} de aciertos." in correo.textoPlano)
    }

    @Test
    fun sinMensajeInterpretativoSeOmiteElRecuadro() {
        val correo = renderizar { copy(mensajeInterpretativo = null) }
        assertFalse("#B6F0C6" in correo.html)
        assertFalse("mejoró" in correo.html || "mejoró" in correo.textoPlano)
    }

    @Test
    fun cadaFilaLlevaMuestraNombreYPorcentaje() {
        val html = renderizar().html
        listOf(TipoCaneca.Blanca to 94, TipoCaneca.Verde to 92, TipoCaneca.Negra to 61).forEach { (caneca, valor) ->
            val fila = Regex(
                """background-color:${caneca.color};border:2px solid ${caneca.borde};.*?""" +
                    """>${caneca.categoria}</td>.*?>${pct(valor)}</td>""",
            )
            assertTrue(fila.containsMatchIn(html), "La fila de ${caneca.categoria} necesita muestra, nombre y porcentaje")
        }
        val orden = listOf("Aprovechables", "Orgánicos", "No aprovechables").map { html.indexOf(">$it</td>") }
        assertEquals(orden.sorted(), orden, "Las filas van en el orden del diseño")
    }

    @Test
    fun colorDeLaBarraCambiaEn80() {
        assertEquals("#2E6B45", PlantillaCorreoReporteSemanal.colorBarraAciertos(80))
        assertEquals("#2E6B45", PlantillaCorreoReporteSemanal.colorBarraAciertos(100))
        assertEquals("#E8A33D", PlantillaCorreoReporteSemanal.colorBarraAciertos(79))
        assertEquals("#E8A33D", PlantillaCorreoReporteSemanal.colorBarraAciertos(0))

        val html = renderizar { copy(aciertosPorCaneca = mapOf(TipoCaneca.Blanca to 80, TipoCaneca.Verde to 79)) }.html
        assertTrue("""<td width="80%" height="10" bgcolor="#2E6B45"""" in html)
        assertTrue("""<td width="79%" height="10" bgcolor="#E8A33D"""" in html)
    }

    @Test
    fun barrasEnLosExtremos() {
        val html = renderizar { copy(aciertosPorCaneca = mapOf(TipoCaneca.Blanca to 100, TipoCaneca.Verde to 0)) }.html
        assertTrue("""<td width="100%" height="10" bgcolor="#2E6B45"""" in html)
        assertFalse("""<td width="0%"""" in html)
        assertTrue(">${pct(0)}</td>" in html, "Con 0 % la fila sigue diciendo el porcentaje")
    }

    @Test
    fun omiteLaFilaDeUnaCanecaSinDatos() {
        val correo = renderizar { copy(aciertosPorCaneca = mapOf(TipoCaneca.Blanca to 94, TipoCaneca.Verde to 92)) }
        assertFalse("No aprovechables" in correo.html)
        assertFalse("No aprovechables" in correo.textoPlano)
        assertTrue(">Aprovechables</td>" in correo.html)
    }

    @Test
    fun sinDatosPorCanecaSeOmiteLaSeccion() {
        val correo = renderizar { copy(aciertosPorCaneca = emptyMap()) }
        assertFalse("Aciertos por caneca" in correo.html)
        assertFalse("Aciertos por caneca" in correo.textoPlano)
    }

    @Test
    fun insigniasDeLaSemanaEnSingularYPlural() {
        val strong = "<strong style=\"font-weight:700;\">"
        assertTrue(
            "2 insignias nuevas: ${strong}Amiga tortuga</strong> y ${strong}Racha de 5</strong>" in renderizar().html,
        )

        val una = renderizar { copy(insigniasSemana = listOf("Amiga tortuga")) }
        assertTrue("1 insignia nueva: ${strong}Amiga tortuga</strong>" in una.html)
        assertTrue("1 insignia nueva: Amiga tortuga." in una.textoPlano)

        val tres = renderizar { copy(insigniasSemana = listOf("Amiga tortuga", "Racha de 5", "Coral feliz")) }
        assertTrue("3 insignias nuevas: Amiga tortuga, Racha de 5 y Coral feliz." in tres.textoPlano)

        val ninguna = renderizar { copy(insigniasSemana = emptyList()) }
        assertFalse("insignia" in ninguna.html.lowercase())
        assertFalse("insignia" in ninguna.textoPlano.lowercase())
        assertFalse("#FFF8E9" in ninguna.html)
    }

    @Test
    fun tiempoDeJuegoFrenteAlLimite() {
        val f = EspacioFijo
        assertEquals(
            "Tiempo de juego: 1${f}h 12${f}min en la semana, por debajo del límite de 20${f}min diarios que definiste.",
            tiempo(minutos = 72, dias = 4, limite = 20),
        )
        assertEquals(
            "Tiempo de juego: 1${f}h 20${f}min en la semana, igual al límite de 20${f}min diarios que definiste.",
            tiempo(minutos = 80, dias = 4, limite = 20),
        )
        assertEquals(
            "Tiempo de juego: 1${f}h 21${f}min en la semana, por encima del límite de 20${f}min diarios que definiste.",
            tiempo(minutos = 81, dias = 4, limite = 20),
        )
        assertEquals("Tiempo de juego: 1${f}h 12${f}min en la semana.", tiempo(minutos = 72, dias = 4, limite = null))
        assertEquals("Tiempo de juego: esta semana no jugó.", tiempo(minutos = 0, dias = 0, limite = 20))
        assertTrue(tiempo(minutos = 72, dias = 4, limite = 20) in renderizar().html)
    }

    @Test
    fun pieDelReporteSemanal() {
        val correo = renderizar()
        val f = EspacioFijo
        val aviso = "Enviado los viernes a las 5:00${f}p.${f}m. Solo contiene información de Salomé."
        assertTrue(aviso in correo.html && aviso in correo.textoPlano)
        assertTrue(">Abrir el reporte completo</a>" in correo.html)
        listOf(">Preferencias de correo</a>", ">Privacidad</a>", ">Dejar de recibir</a>").forEach {
            assertTrue(it in correo.html)
        }
    }

    @Test
    fun textoOcultoResumeLaSemana() {
        assertTrue("display:none;" in renderizar().html)
        assertTrue("4 de 5 retos · ${pct(88)} de aciertos · racha de 5" in renderizar().html)
    }

    @Test
    fun textoPlanoTieneLasCifras() {
        val texto = renderizar().textoPlano
        listOf(
            "ReciclaKids · Semana del 15 al 19 de septiembre",
            "Retos: 4 de 5",
            "Aciertos: ${pct(88)}",
            "Racha: 5",
            "- Aprovechables (caneca blanca): ${pct(94)}",
            "- Orgánicos (caneca verde): ${pct(92)}",
            "- No aprovechables (caneca negra): ${pct(61)}",
            "2 insignias nuevas: Amiga tortuga y Racha de 5.",
            "Abrir el reporte completo: ${datos.enlaceReporte}",
            "1${EspacioFijo}h 12${EspacioFijo}min",
            "Dejar de recibir: ${datos.enlacesPie.baja}",
        ).forEach { assertTrue(it in texto, "Falta «$it» en el texto plano") }
        assertFalse('<' in texto, "El texto plano no lleva marcado")
    }

    @Test
    fun escapaLosDatosMaliciosos() {
        val correo = renderizar {
            copy(
                nombreNino = "<script>alert('x')</script>",
                insigniasSemana = listOf("<img src=x onerror=alert(1)>"),
                mensajeInterpretativo = MensajeInterpretativo("Mejoró <b>", "<i>orgánicos</i>", "<a href=\"https://malo.example\">"),
            )
        }
        listOf("<script", "<img", "<b>", "<i>", "href=\"https://malo.example\"").forEach {
            assertFalse(it in correo.html, "Se coló «$it» sin escapar")
        }
        assertTrue("Solo contiene información de &lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt;." in correo.html)
        assertTrue("<strong style=\"font-weight:700;\">&lt;i&gt;orgánicos&lt;/i&gt;</strong>" in correo.html)
    }

    @Test
    fun rechazaEnlacesQueNoSonHttp() {
        assertFailsWith<IllegalArgumentException> { renderizar { copy(enlaceReporte = "javascript:alert(1)") } }
    }

    @Test
    fun cadaCorreoSoloNombraASuPropioNino() {
        GrupoFicticio.forEach { nombre ->
            val correo = renderizar { copy(nombreNino = nombre) }
            val todo = correo.asunto + correo.html + correo.textoPlano
            assertTrue(nombre in todo)
            (GrupoFicticio - nombre).forEach { otro ->
                assertFalse(otro in todo, "El reporte de $nombre menciona a $otro")
            }
            assertSinComparacionesConElGrupo(todo)
        }
    }

    @Test
    fun validaLosDatos() {
        assertFailsWith<IllegalArgumentException> { renderizar { copy(porcentajeAciertos = 101) } }
        assertFailsWith<IllegalArgumentException> { renderizar { copy(retosCompletados = 6) } }
        assertFailsWith<IllegalArgumentException> { renderizar { copy(aciertosPorCaneca = mapOf(TipoCaneca.Negra to -1)) } }
        assertFailsWith<IllegalArgumentException> { renderizar { copy(finSemana = LocalDate.of(2025, 9, 1)) } }
        assertFailsWith<IllegalArgumentException> { TiempoDeJuego(minutosSemana = 10, diasConJuego = 0, limiteDiarioMinutos = 20) }
        assertFailsWith<IllegalArgumentException> { MensajeInterpretativo("Mejoró ", " ", "") }
    }
}
