package com.reciclakids.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import com.reciclakids.contenedorDePrueba
import com.reciclakids.model.CuentaAdulto
import com.reciclakids.model.RolAdulto
import com.reciclakids.network.RegistroAcudiente
import com.reciclakids.network.ResultadoRegistro
import com.reciclakids.network.ServicioAcceso
import com.reciclakids.network.ServicioAccesoEnMemoria
import com.reciclakids.ui.theme.ReciclaKidsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * El acceso con lo que solo pasa con el backend: un código de vinculación que no existe y la
 * sesión que queda abierta en el teléfono.
 */
@RunWith(RobolectricTestRunner::class)
class FlujoSesionTest {

    @get:Rule
    val compose = createComposeRule()

    /** Como el backend: solo existe el código de Salomé, y puede quedar una sesión guardada. */
    private class AccesoConBackend(
        private val enMemoria: ServicioAccesoEnMemoria = ServicioAccesoEnMemoria(latenciaMs = 0),
        var guardada: CuentaAdulto? = null,
    ) : ServicioAcceso by enMemoria {
        var cierres = 0

        override suspend fun registrarAcudiente(datos: RegistroAcudiente): ResultadoRegistro =
            if (datos.codigoVinculacion == "JB-2M91") enMemoria.registrarAcudiente(datos) else ResultadoRegistro.CodigoInvalido

        override suspend fun sesionGuardada(): CuentaAdulto? = guardada

        override fun cerrarSesion() {
            cierres++
            guardada = null
        }
    }

    private fun abrirApp(acceso: ServicioAcceso) {
        compose.setContent { ReciclaKidsTheme { ReciclaKidsApp(contenedor = contenedorDePrueba(servicioAcceso = acceso)) } }
        compose.onNodeWithContentDescription("ReciclaKids. Toca la pantalla para continuar").performClick()
    }

    private fun campo(etiqueta: String) = compose.onNode(hasSetTextAction() and hasText(etiqueta))

    private fun pasarPuertaDeAdultos() {
        compose.onNodeWithText("Adultos").performClick()
        compose.onNodeWithContentDescription("Mantén presionado tres segundos para entrar").performTouchInput { down(center) }
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(1_000)
    }

    @Test
    fun unCodigoDeVinculacionQueNoExisteVuelveAUi05ConElErrorEnSuCampo() {
        abrirApp(AccesoConBackend())
        pasarPuertaDeAdultos()
        compose.onNodeWithText("Crear cuenta").performScrollTo().performClick()
        compose.onNodeWithText("Acudiente").performClick()
        campo("Tu nombre").performTextInput("Mariana Ríos")
        campo("Correo").performTextInput("mariana.r@correo.com")
        campo("Contraseña").performTextInput("clave-de-prueba")
        campo("Código de vinculación").performTextInput("JB-0000")
        compose.onNodeWithText("Continuar").performScrollTo().performClick()
        compose.onNode(isToggleable()).performScrollTo().performClick()
        compose.onNodeWithText("Crear cuenta").performScrollTo().performClick()

        // De vuelta en UI-05, con lo escrito y el error donde se corrige.
        val error = "Ese código no existe. Revísalo o pídeselo a la docente"
        compose.onNodeWithText(error).performScrollTo().assertIsDisplayed()
        campo("Tu nombre").assertIsDisplayed()

        // Al cambiar el código el error se va, y con el correcto se crea la cuenta.
        campo("Código de vinculación").performTextClearance()
        campo("Código de vinculación").performTextInput("JB-2M91")
        compose.onAllNodesWithText(error).assertCountEquals(0)
        compose.onNodeWithText("Continuar").performScrollTo().performClick()
        compose.onNode(isToggleable()).performScrollTo().performClick()
        compose.onNodeWithText("Crear cuenta").performScrollTo().performClick()
        compose.onNodeWithText("Cuenta creada. Ya puedes iniciar sesión.").assertIsDisplayed()
    }

    @Test
    fun laSesionGuardadaAbreSuModoAlPasarLaPuertaYCerrarlaPideIniciarSesion() {
        val acceso = AccesoConBackend(guardada = CuentaAdulto("Laura Restrepo", "laura.r@jardin.edu.co", RolAdulto.Docente, "Gotitas", "Jardín B"))
        abrirApp(acceso)
        // El niño sigue viendo primero su selector: la sesión no salta la puerta para adultos.
        compose.onNodeWithText("¡A jugar!").assertIsDisplayed()

        pasarPuertaDeAdultos()
        compose.onNodeWithText("Hola, Laura").assertIsDisplayed()

        compose.onNodeWithText("Cuenta").performClick()
        compose.onNodeWithText("Cerrar sesión").performScrollTo().performClick()
        compose.onNodeWithText("¡A jugar!").assertIsDisplayed()
        assertEquals(1, acceso.cierres)

        pasarPuertaDeAdultos()
        compose.onNodeWithText("Inicia sesión").assertIsDisplayed()
    }
}
