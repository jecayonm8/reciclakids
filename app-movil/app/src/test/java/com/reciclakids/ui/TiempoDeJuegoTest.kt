package com.reciclakids.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.reciclakids.contenedorDePrueba
import com.reciclakids.model.ControlParental
import com.reciclakids.ui.theme.ReciclaKidsTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** UI-19: al agotar el tiempo del control parental, el Modo Niño se cierra con una razón amable. */
@RunWith(RobolectricTestRunner::class)
class TiempoDeJuegoTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val titulo = "¡Los peces también descansan!\nVuelve mañana"

    @Test
    fun alAgotarElLimiteDiarioSeMuestraElDescansoYNoSePuedeSalir() {
        compose.setContent {
            ReciclaKidsTheme { ReciclaKidsApp(contenedor = contenedorDePrueba(ControlParental(limiteMinutosDiarios = 1))) }
        }
        compose.onNodeWithContentDescription("ReciclaKids. Toca la pantalla para continuar").performClick()
        compose.onNodeWithText("¡A jugar!").performClick()
        "4729".forEach { compose.onNodeWithText(it.toString()).performClick() }
        compose.onNodeWithContentDescription("Entrar").performClick()
        compose.mainClock.advanceTimeBy(1_500)
        compose.onNodeWithText("Mateo S.").performClick()
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(1_000)
        compose.onNodeWithText("Reto diario").assertIsDisplayed()

        compose.mainClock.advanceTimeBy(65_000)

        compose.onNodeWithText(titulo).assertIsDisplayed()
        compose.onNodeWithText("Ya jugaste tu 1 minuto").assertIsDisplayed()

        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText(titulo).assertIsDisplayed()
    }
}
