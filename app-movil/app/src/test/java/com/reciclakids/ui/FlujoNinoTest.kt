package com.reciclakids.ui

import android.os.Looper
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.reciclakids.contenedorDePrueba
import com.reciclakids.ui.theme.ReciclaKidsTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Duration

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

    private fun entrarComo(nombre: String, codigo: String = "4729") {
        escribirYConfirmar(codigo)
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
        compose.onNodeWithContentDescription("Pausa").assertIsDisplayed()

        compose.onNodeWithContentDescription("Pausa").performClick()
        compose.onNodeWithText("Menú").performClick()
        compose.onNodeWithText("Reto diario").performClick()

        compose.onNodeWithContentDescription("Pausa").assertIsDisplayed()
    }

    // --- Juego (reto fácil 1111: botella, cáscara, lata y resto de comida) ---

    private val blanca = "Caneca blanca: aprovechables"
    private val verde = "Caneca verde: orgánicos"

    private fun empezarRetoFacil() {
        entrarComo("Ana L.", codigo = "1111")
        compose.onNodeWithText("Reto diario").performClick()
        compose.onNodeWithText("¡Ya entendí!").performClick()
        compose.onNodeWithContentDescription("0 puntos").assertIsDisplayed()
    }

    /**
     * Avanza el reloj de Compose y el del hilo principal de Robolectric: las pausas del juego
     * (celebración y rebote) corren en el ViewModel, fuera de la composición.
     */
    private fun esperar(ms: Long) {
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(ms)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))
        compose.waitForIdle()
    }

    private fun tocarCaneca(descripcion: String) {
        compose.onNodeWithContentDescription(descripcion).performClick()
        esperar(1_200)
    }

    @Test
    fun unErrorNoRestaPuntosYMuestraLaCanecaQueBrilla() {
        empezarRetoFacil()

        compose.onNodeWithContentDescription(verde).performClick()

        compose.onNodeWithText("¡Casi! Mira la caneca que brilla").assertIsDisplayed()
        compose.onNodeWithContentDescription("0 puntos").assertIsDisplayed()
    }

    @Test
    fun alTerminarSeCelebraYElRetoDejaDeEstarPendiente() {
        empezarRetoFacil()
        tocarCaneca(verde)
        listOf(blanca, verde, blanca, verde).forEach(::tocarCaneca)

        compose.onNodeWithText("¡Separaste 4 residuos!").assertIsDisplayed()
        compose.onNodeWithText("Tu acuario está más limpio").assertIsDisplayed()
        // Con un error no hay insignia «Amiga tortuga» ni racha de 5: no hay premio.
        compose.onNodeWithText("Ver premio").assertDoesNotExist()

        compose.onNodeWithText("Menú").performClick()
        compose.onNodeWithText("Reto diario").assertIsDisplayed()
        compose.onNodeWithContentDescription("Hay un reto nuevo").assertDoesNotExist()
    }

    @Test
    fun sinErroresHayPremio() {
        empezarRetoFacil()
        listOf(blanca, verde, blanca, verde).forEach(::tocarCaneca)

        compose.onNodeWithText("Ver premio").performClick()

        compose.onNodeWithText("Modo Niño: sigue la insignia obtenida (UI-16).").assertIsDisplayed()
    }

    @Test
    fun arrastrarElResiduoCercaDeSuCanecaCuentaComoAcierto() {
        empezarRetoFacil()
        val residuo = compose.onNodeWithContentDescription("botella plástica").fetchSemanticsNode().boundsInRoot.center
        val caneca = compose.onNodeWithContentDescription(blanca).fetchSemanticsNode().boundsInRoot.center

        compose.onNodeWithContentDescription("botella plástica").performTouchInput {
            down(center)
            repeat(10) { moveBy((caneca - residuo) / 10f) }
            up()
        }
        compose.waitForIdle()

        compose.onNodeWithText("¡Muy bien!").assertIsDisplayed()
    }

    @Test
    fun soltarLejosDeLasCanecasNoCuentaComoError() {
        empezarRetoFacil()

        compose.onNodeWithContentDescription("botella plástica").performTouchInput {
            down(center)
            repeat(5) { moveBy(Offset(0f, 20f)) }
            up()
        }
        esperar(1_000)

        compose.onNodeWithText("¡Casi! Mira la caneca que brilla").assertDoesNotExist()
        compose.onNodeWithText("¡Muy bien!").assertDoesNotExist()
    }
}
