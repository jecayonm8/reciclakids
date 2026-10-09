package com.reciclakids.services.notificaciones.correos

import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertTrue

/** El botón del correo pide 7:1; el resto del texto, WCAG AA (4,5:1). */
class ContrasteCorreoTest {

    private fun luminancia(hex: String): Double {
        val rgb = hex.removePrefix("#").toInt(16)
        fun canal(valor: Int): Double {
            val s = valor / 255.0
            return if (s <= 0.04045) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * canal(rgb shr 16 and 0xFF) + 0.7152 * canal(rgb shr 8 and 0xFF) + 0.0722 * canal(rgb and 0xFF)
    }

    private fun contraste(a: String, b: String): Double {
        val (clara, oscura) = listOf(luminancia(a), luminancia(b)).sortedDescending()
        return (clara + 0.05) / (oscura + 0.05)
    }

    private fun assertContraste(nombre: String, texto: String, fondo: String, minimo: Double) {
        val valor = contraste(texto, fondo)
        assertTrue(valor >= minimo, "$nombre: contraste ${"%.2f".format(valor)} menor que $minimo")
    }

    @Test
    fun botonCumpleSieteAUno() {
        assertContraste("botón", ColoresCorreo.Blanco, ColoresCorreo.Boton, 7.0)
    }

    @Test
    fun textosCumplenAA() {
        val c = ColoresCorreo
        assertContraste("encabezado del logro", c.Blanco, c.Primario, 4.5)
        assertContraste("encabezado del reporte", c.Blanco, c.PrimarioOscuro, 4.5)
        assertContraste("nombre de la insignia", c.Primario, c.Superficie, 4.5)
        assertContraste("texto", c.Texto, c.Superficie, 4.5)
        assertContraste("texto secundario", c.TextoSecundario, c.Superficie, 4.5)
        assertContraste("cifras y pie", c.TextoSecundario, c.Recuadro, 4.5)
        assertContraste("enlaces del pie", c.Enlace, c.Recuadro, 4.5)
        assertContraste("aciertos en verde", c.Terciario, c.Recuadro, 4.5)
        assertContraste("mensaje interpretativo", c.EnContenedorTerciario, c.ContenedorTerciario, 4.5)
        assertContraste("insignias de la semana", c.Texto, c.InsigniaRecuadro, 4.5)
    }
}
