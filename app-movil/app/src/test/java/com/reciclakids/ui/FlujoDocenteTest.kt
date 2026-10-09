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
import com.reciclakids.network.RegistroDocente
import com.reciclakids.ui.theme.ReciclaKidsTheme
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Recorre P3 · Docente sobre el NavHost real, desde el inicio de sesión. */
@RunWith(RobolectricTestRunner::class)
class FlujoDocenteTest {

    @get:Rule
    val compose = createComposeRule()

    private fun campo(etiqueta: String) = compose.onNode(hasSetTextAction() and hasText(etiqueta))

    @Before
    fun entrarComoDocente() {
        val contenedor = contenedorDePrueba()
        runBlocking {
            contenedor.servicioAcceso.registrarDocente(
                RegistroDocente("Laura Restrepo", "laura.r@jardin.edu.co", "Gotitas", "Jardín B", "clave-de-prueba")
            )
        }
        compose.setContent { ReciclaKidsTheme { ReciclaKidsApp(contenedor = contenedor) } }
        compose.onNodeWithContentDescription("ReciclaKids. Toca la pantalla para continuar").performClick()
        compose.onNodeWithText("Adultos").performClick()
        compose.onNodeWithContentDescription("Mantén presionado tres segundos para entrar").performTouchInput { down(center) }
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(1_000)
        campo("Correo").performTextInput("laura.r@jardin.edu.co")
        campo("Contraseña").performTextInput("clave-de-prueba")
        compose.onNodeWithText("Entrar").performScrollTo().performClick()

        compose.onNodeWithText("Hola, Laura").assertIsDisplayed()
    }

    @Test
    fun crearYPublicarUnRetoLlevaAlCodigoParaElGrupo() {
        compose.onNodeWithContentDescription("Crear reto").performClick()
        compose.onNodeWithText("Crear reto").assertIsDisplayed()

        repeat(3) { compose.onNodeWithText("Siguiente").performClick() }
        compose.onNodeWithText("Todo listo").performClick()
        compose.onNodeWithText("Publicar").performClick()

        compose.onNodeWithText("¡Reto publicado!").assertIsDisplayed()
        compose.onNodeWithText("Mostrar al grupo").performClick()
        compose.onNodeWithText("Escriban este código").assertIsDisplayed()
        compose.onNodeWithText("Todavía no entra ningún niño").performScrollTo().assertIsDisplayed()

        compose.onNodeWithContentDescription("Volver").performClick()
        compose.onNodeWithText("Reto de aprovechables y orgánicos").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun reutilizarDesdeElTableroSaltaALaVistaPrevia() {
        compose.onNodeWithText("Crear a partir de uno anterior").performScrollTo().performClick()

        compose.onNodeWithText("Reutilizar un reto").assertIsDisplayed()
        compose.onNodeWithContentDescription("Usar «Envases del salón»").performClick()

        compose.onNodeWithText("Así lo verá el niño").assertIsDisplayed()
        compose.onNodeWithText("Todo listo").assertIsDisplayed()
    }

    @Test
    fun lasPestanasCambianDeSeccionYCerrarSesionVuelveAlSelector() {
        compose.onNodeWithText("Biblioteca").performClick()
        compose.onNodeWithText("Biblioteca de retos").assertIsDisplayed()

        compose.onNodeWithText("Grupo").performClick()
        compose.onNodeWithText("Mi grupo · Jardín B").assertIsDisplayed()
        compose.onNodeWithText("Salomé M.").performClick()
        compose.onNodeWithText("Solo tú y su acudiente ven esta información").assertIsDisplayed()
        compose.onNodeWithContentDescription("Volver").performClick()

        compose.onNodeWithText("Cuenta").performClick()
        compose.onNodeWithText("Cerrar sesión").performScrollTo().performClick()

        compose.onNodeWithText("¡A jugar!").assertIsDisplayed()
    }
}
