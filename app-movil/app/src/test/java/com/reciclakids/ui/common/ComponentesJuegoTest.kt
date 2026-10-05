package com.reciclakids.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.reciclakids.ui.theme.ReciclaKidsTheme
import com.reciclakids.ui.theme.Tactil
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ComponentesJuegoTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun tecladoEntregaCadaDigitoYLasAcciones() {
        val eventos = mutableListOf<String>()
        compose.setContent {
            ReciclaKidsTheme {
                TecladoNumericoNino(
                    onDigito = { eventos += it.toString() },
                    onBorrar = { eventos += "borrar" },
                    onConfirmar = { eventos += "entrar" },
                )
            }
        }

        "4729".forEach { compose.onNodeWithText(it.toString()).performClick() }
        compose.onNodeWithContentDescription("Borrar").performClick()
        compose.onNodeWithContentDescription("Entrar").performClick()

        assertEquals(listOf("4", "7", "2", "9", "borrar", "entrar"), eventos)
    }

    @Test
    fun teclasCumplenElMinimoTactilDelNino() {
        compose.setContent {
            ReciclaKidsTheme { TecladoNumericoNino(onDigito = {}, onBorrar = {}, onConfirmar = {}) }
        }

        "0123456789".forEach {
            compose.onNodeWithText(it.toString())
                .assertHeightIsAtLeast(Tactil.minimoNino)
                .assertWidthIsAtLeast(Tactil.minimoNino)
        }
    }

    @Test
    fun casillasDescribenElCodigoEscrito() {
        var codigo by mutableStateOf("")
        compose.setContent { ReciclaKidsTheme { CasillasCodigo(codigo) } }
        compose.onNodeWithContentDescription("Código del reto sin escribir").assertIsDisplayed()

        codigo = "47"
        compose.onNodeWithContentDescription("Código del reto: 4 7").assertIsDisplayed()

        // Nunca pinta más dígitos que casillas.
        codigo = "472913"
        compose.onNodeWithContentDescription("Código del reto: 4 7 2 9").assertIsDisplayed()
    }

    @Test
    fun canecaSeIdentificaPorCategoriaYRespondeAlToque() {
        var tocada: TipoCaneca? = null
        compose.setContent {
            ReciclaKidsTheme {
                Column { TipoCaneca.entries.forEach { tipo -> Caneca(tipo, onClick = { tocada = tipo }) } }
            }
        }

        compose.onNodeWithContentDescription("Caneca blanca: aprovechables").assertHasClickAction()
        compose.onNodeWithContentDescription("Caneca negra: no aprovechables").assertHasClickAction()
        compose.onNodeWithContentDescription("Caneca verde: orgánicos").performClick()

        assertEquals(TipoCaneca.Verde, tocada)
    }

    @Test
    fun botonNinoRespondeYCumpleElMinimoTactil() {
        var toques = 0
        compose.setContent {
            ReciclaKidsTheme { BotonNino("Seguir", onClick = { toques++ }, variante = VarianteBotonNino.Confirmar) }
        }

        compose.onNodeWithText("Seguir")
            .assertHeightIsAtLeast(Tactil.minimoNino)
            .assertWidthIsAtLeast(Tactil.minimoNino)
            .performClick()

        assertEquals(1, toques)
    }

    @Test
    fun rachaMuestraBonificacionDesdeTresAciertos() {
        var racha by mutableStateOf(2)
        compose.setContent { ReciclaKidsTheme { IndicadorRacha(racha) } }
        compose.onNodeWithContentDescription("Racha de 2").assertIsDisplayed()

        racha = RachaConBonificacion
        compose.onNodeWithContentDescription("Racha de 3, burbujas dobles").assertIsDisplayed()
    }

    @Test
    fun indicadorDeConexionNuncaHablaDeError() {
        var estado by mutableStateOf(EstadoConexion.SinConexion)
        compose.setContent { ReciclaKidsTheme { IndicadorSinConexion(estado) } }
        compose.onNodeWithText("Guardado aquí").assertIsDisplayed()

        estado = EstadoConexion.Sincronizando
        compose.onNodeWithText("Guardando…").assertIsDisplayed()

        estado = EstadoConexion.Sincronizado
        compose.onNodeWithText("¡Guardado!").assertIsDisplayed()
    }

    @Test
    fun insigniaPendienteDiceCuantoFalta() {
        compose.setContent {
            ReciclaKidsTheme { TarjetaInsignia("Pulpo ordenado", "Faltan 2 retos", ganada = false) }
        }

        compose.onNodeWithContentDescription("Insignia por descubrir: Pulpo ordenado. Faltan 2 retos").assertIsDisplayed()
    }

    @Test
    fun acuarioAcotaElNivelFueraDeRango() {
        var nivel by mutableStateOf(0)
        compose.setContent { ReciclaKidsTheme { AcuarioEstado(nivel) { PersonajeMarino(TipoPersonaje.Pez, tamano = 80.dp) } } }
        compose.onNodeWithText("pez\nflota").assertIsDisplayed()

        nivel = 9
        compose.onNodeWithText("pez\nflota").assertIsDisplayed()
    }
}
