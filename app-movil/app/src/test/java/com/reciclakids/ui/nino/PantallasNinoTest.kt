package com.reciclakids.ui.nino

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import com.reciclakids.model.Insignia
import com.reciclakids.model.ProgresoNino
import com.reciclakids.ui.common.EstadoCodigo
import com.reciclakids.ui.theme.ReciclaKidsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Pantallas del Modo Niño montadas solas, para revisar su disposición y su accesibilidad. */
@RunWith(RobolectricTestRunner::class)
class PantallasNinoTest {

    @get:Rule
    val compose = createComposeRule()

    // --- UI-08 Código del reto ---

    @Test
    fun elTecladoNoSeMueveCuandoCambiaElMensajeDelPez() {
        var estado by mutableStateOf(EstadoCodigo.Editando)
        compose.setContent {
            ReciclaKidsTheme {
                CodigoRetoScreen("", estado, onDigito = {}, onBorrar = {}, onConfirmar = {}, onRepetirVoz = {})
            }
        }
        val tecla = compose.onNodeWithText("0").getUnclippedBoundsInRoot()

        EstadoCodigo.entries.forEach {
            estado = it
            compose.waitForIdle()
            assertEquals("con el mensaje de $it", tecla, compose.onNodeWithText("0").getUnclippedBoundsInRoot())
        }
    }

    @Test
    fun soloElMensajeActualDelPezSeLee() {
        compose.setContent {
            ReciclaKidsTheme {
                CodigoRetoScreen("", EstadoCodigo.Vencido, onDigito = {}, onBorrar = {}, onConfirmar = {}, onRepetirVoz = {})
            }
        }

        compose.onNodeWithText("Ese código ya descansó. Pídele uno nuevo a la profe").assertIsDisplayed()
        compose.onNodeWithText("Toca los números que te dijo la profe").assertDoesNotExist()
    }

    // --- UI-10 Menú · acuario ---

    @Test
    fun ajustesEsUnBotonQueTalkBackPuedeActivar() {
        var ajustes = 0
        compose.setContent {
            ReciclaKidsTheme {
                MenuAcuarioScreen(
                    nombre = "Salomé M.", nivel = 2, retoPendiente = false, hayConexion = true,
                    onRetoDiario = {}, onContinuar = {}, onInsignias = {}, onAjustes = { ajustes++ },
                )
            }
        }

        compose.onNodeWithContentDescription("Ajustes")
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performSemanticsAction(SemanticsActions.OnClick)

        assertEquals(1, ajustes)
    }

    // --- UI-17 Colección de insignias ---

    @Test
    fun alFinalDeLaColeccionLaUltimaFilaQuedaSobreElAguaYNoSobreLaArena() {
        compose.setContent {
            ReciclaKidsTheme {
                ColeccionInsigniasScreen(
                    ganadas = emptySet(),
                    progreso = ProgresoNino("n"),
                    diasConReto = 0,
                    onTocar = {},
                    onVolver = {},
                    // Una pantalla baja obliga a desplazarse hasta el final.
                    modifier = Modifier.height(400.dp).testTag("coleccion"),
                )
            }
        }

        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(Insignia.entries.lastIndex)

        val pantalla = compose.onNodeWithTag("coleccion").getUnclippedBoundsInRoot()
        val ultima = compose.onNode(hasContentDescription(Insignia.entries.last().nombre, substring = true))
            .getUnclippedBoundsInRoot()
        val bordeArena = pantalla.bottom - 50.dp
        assertTrue("la última insignia termina en ${ultima.bottom} y la arena empieza en $bordeArena", ultima.bottom <= bordeArena)
    }
}
