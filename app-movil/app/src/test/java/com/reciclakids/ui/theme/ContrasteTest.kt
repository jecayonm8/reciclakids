package com.reciclakids.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** El diseño exige contraste WCAG AA: 4,5:1 en texto normal y 3:1 desde 24 sp. */
class ContrasteTest {

    private fun contraste(a: Color, b: Color): Float {
        val (clara, oscura) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (clara + 0.05f) / (oscura + 0.05f)
    }

    private fun assertContraste(nombre: String, texto: Color, fondo: Color, minimo: Float) {
        val valor = contraste(texto, fondo)
        assertTrue("$nombre: contraste $valor menor que $minimo", valor >= minimo)
    }

    @Test
    fun rolesMaterialCumplenAAParaTextoNormal() {
        val c = ReciclaKidsLightColors
        assertContraste("onPrimary/primary", c.onPrimary, c.primary, 4.5f)
        assertContraste("onPrimaryContainer/primaryContainer", c.onPrimaryContainer, c.primaryContainer, 4.5f)
        assertContraste("onSecondary/secondary", c.onSecondary, c.secondary, 4.5f)
        assertContraste("onSecondaryContainer/secondaryContainer", c.onSecondaryContainer, c.secondaryContainer, 4.5f)
        assertContraste("onTertiary/tertiary", c.onTertiary, c.tertiary, 4.5f)
        assertContraste("onTertiaryContainer/tertiaryContainer", c.onTertiaryContainer, c.tertiaryContainer, 4.5f)
        assertContraste("onError/error", c.onError, c.error, 4.5f)
        assertContraste("onErrorContainer/errorContainer", c.onErrorContainer, c.errorContainer, 4.5f)
        assertContraste("onSurface/surface", c.onSurface, c.surface, 4.5f)
        assertContraste("onSurfaceVariant/surface", c.onSurfaceVariant, c.surface, 4.5f)
        assertContraste("onSurfaceVariant/surfaceContainer", c.onSurfaceVariant, c.surfaceContainer, 4.5f)
        assertContraste("primary/surface", c.primary, c.surface, 4.5f)
    }

    @Test
    fun iconosDeCanecaCumplenContrasteNoTextual() {
        val k = ReciclaKidsColors
        assertContraste("caneca blanca", k.onCanecaBlanca, k.canecaBlanca, 3f)
        assertContraste("caneca negra", k.onCanecaNegra, k.canecaNegra, 3f)
        assertContraste("caneca verde", k.onCanecaVerde, k.canecaVerde, 3f)
    }

    @Test
    fun botonesDelNinoCumplenAA() {
        val k = ReciclaKidsColors
        assertContraste("botón jugar", k.onBotonJugar, k.botonJugar, 4.5f)
        assertContraste("botón confirmar", Color.White, k.botonConfirmar, 4.5f)
    }

    @Test
    fun cadaDigitoTieneEstiloYContrasteDeTextoGrande() {
        assertEquals("0123456789".toSet(), EstilosDigito.keys)
        EstilosDigito.forEach { (digito, estilo) ->
            assertContraste("dígito $digito", estilo.texto, estilo.fondo, 3f)
        }
    }

    @Test
    fun digitosConLaMismaFormaTienenColorDistinto() {
        // El niño distingue el código por figura y color: la pareja (forma, color) nunca se repite.
        EstilosDigito.values.groupBy { it.forma }.forEach { (forma, estilos) ->
            assertEquals("forma $forma", estilos.size, estilos.map { it.fondo }.toSet().size)
        }
    }
}
