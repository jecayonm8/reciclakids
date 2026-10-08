package com.reciclakids.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.reciclakids.contenedorDePrueba
import com.reciclakids.ui.theme.ReciclaKidsTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Recorre la entrada del Modo Niño sobre el NavHost real: «¡A jugar!» → código → avatar. */
@RunWith(RobolectricTestRunner::class)
class FlujoNinoTest {

    @get:Rule
    val compose = createComposeRule()

    @Before
    fun abrirElTeclado() {
        compose.setContent {
            ReciclaKidsTheme {
                ReciclaKidsApp(contenedor = contenedorDePrueba())
            }
        }
        compose.onNodeWithContentDescription("ReciclaKids. Toca la pantalla para continuar").performClick()
        compose.onNodeWithText("¡A jugar!").performClick()
        compose.onNodeWithText("Toca los números que te dijo la profe").assertIsDisplayed()
    }

    private fun escribirYConfirmar(codigo: String) {
        codigo.forEach { compose.onNodeWithText(it.toString()).performClick() }
        compose.onNodeWithContentDescription("Entrar").performClick()
    }

    @Test
    fun unCodigoEquivocadoInvitaAProbarOtraVez() {
        escribirYConfirmar("5555")

        compose.onNodeWithText("Ese código no es. ¡Probemos otra vez!").assertIsDisplayed()
    }

    @Test
    fun unCodigoVencidoPideUnoNuevoALaProfe() {
        escribirYConfirmar("1234")

        compose.onNodeWithText("Ese código ya descansó. Pídele uno nuevo a la profe").assertIsDisplayed()
    }

    private fun entrarComo(nombre: String) {
        escribirYConfirmar("4729")
        compose.onNodeWithText("¡Ese es! Vamos a jugar").assertIsDisplayed()
        compose.mainClock.advanceTimeBy(1_500)

        compose.onNodeWithText("¿Quién eres?").assertIsDisplayed()
        compose.onNodeWithText(nombre).performClick()
        // El borde ámbar se ve un momento antes de avanzar.
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(1_000)
    }

    @Test
    fun elCodigoDelDiaLlevaAElegirAvatarYLuegoAlMenu() {
        entrarComo("Salomé M.")

        compose.onNodeWithText("Salomé M.").assertIsDisplayed()
        compose.onNodeWithText("Reto diario").assertIsDisplayed()
        compose.onNodeWithContentDescription("Hay un reto nuevo").assertIsDisplayed()
    }

    @Test
    fun elTutorialSoloSaleLaPrimeraVez() {
        entrarComo("Juan T.")

        compose.onNodeWithText("Reto diario").performClick()
        compose.onNodeWithText("Arrastra el residuo a su caneca").assertIsDisplayed()
        compose.onNodeWithText("¡Ya entendí!").performClick()
        compose.onNodeWithText("Modo Niño: sigue el juego (UI-12).").assertIsDisplayed()

        compose.onNodeWithText("Volver").performClick()
        compose.onNodeWithText("Reto diario").performClick()

        compose.onNodeWithText("Modo Niño: sigue el juego (UI-12).").assertIsDisplayed()
    }
}
