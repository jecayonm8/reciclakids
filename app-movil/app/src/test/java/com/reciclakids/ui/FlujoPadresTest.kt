package com.reciclakids.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import com.reciclakids.contenedorDePrueba
import com.reciclakids.di.ContenedorApp
import com.reciclakids.network.DatosDemoPadres
import com.reciclakids.network.RegistroAcudiente
import com.reciclakids.ui.theme.ReciclaKidsTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Recorre P4 · Padres sobre el NavHost real, desde el inicio de sesión del acudiente. */
@RunWith(RobolectricTestRunner::class)
class FlujoPadresTest {

    @get:Rule
    val compose = createComposeRule()

    private lateinit var contenedor: ContenedorApp

    private fun campo(etiqueta: String) = compose.onNode(hasSetTextAction() and hasText(etiqueta))

    @Before
    fun entrarComoAcudiente() {
        contenedor = contenedorDePrueba()
        runBlocking {
            contenedor.servicioAcceso.registrarAcudiente(
                RegistroAcudiente("Mariana Ríos", "mariana.r@correo.com", "clave-de-prueba", "JB-2M91")
            )
        }
        compose.setContent { ReciclaKidsTheme { ReciclaKidsApp(contenedor = contenedor) } }
        compose.onNodeWithContentDescription("ReciclaKids. Toca la pantalla para continuar").performClick()
        compose.onNodeWithText("Adultos").performClick()
        compose.onNodeWithContentDescription("Mantén presionado tres segundos para entrar").performTouchInput { down(center) }
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(1_000)
        campo("Correo").performTextInput("mariana.r@correo.com")
        campo("Contraseña").performTextInput("clave-de-prueba")
        compose.onNodeWithText("Entrar").performScrollTo().performClick()

        compose.onNodeWithText("Salomé M.").assertIsDisplayed()
    }

    @Test
    fun delInicioSeLlegaAlReporteYALosLogros() {
        compose.onNodeWithText("Ver el reporte completo").performScrollTo().performClick()
        compose.onNodeWithText("Reporte semanal").assertIsDisplayed()
        compose.onNodeWithText("Evolución").assertIsDisplayed()

        compose.onNodeWithText("Ver todas").performScrollTo().performClick()
        compose.onNodeWithText("Logros de Salomé M.").assertIsDisplayed()
    }

    @Test
    fun elLimiteDeTiempoQuePoneElAcudienteLoUsaElModoNino() {
        compose.onNodeWithText("20 min al día").performScrollTo().performClick()
        compose.onNodeWithText("Control parental").assertIsDisplayed()

        compose.onNodeWithContentDescription("Más tiempo").performScrollTo().performClick()

        compose.onNodeWithText("25 min").assertIsDisplayed()
        // El mismo control que lee el vigilante de tiempo del Modo Niño (UI-19).
        assertEquals(25, contenedor.controlesParentales.de(DatosDemoPadres.IdSalome).limiteMinutosDiarios)
    }

    @Test
    fun lasPestanasCambianDeSeccionYCerrarSesionVuelveAlSelector() {
        compose.onNodeWithText("Avisos").performClick()
        compose.onNodeWithText("Notificaciones").assertIsDisplayed()

        compose.onNodeWithText("Cuenta").performClick()
        compose.onNodeWithText("mariana.r@correo.com").assertIsDisplayed()
        compose.onNodeWithText("Cerrar sesión").performScrollTo().performClick()

        compose.onNodeWithText("¡A jugar!").assertIsDisplayed()
    }
}
