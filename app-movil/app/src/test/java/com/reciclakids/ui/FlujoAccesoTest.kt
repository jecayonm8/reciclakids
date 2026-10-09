package com.reciclakids.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import com.reciclakids.contenedorDePrueba
import com.reciclakids.ui.theme.ReciclaKidsTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Recorre P1 · Acceso común de punta a punta sobre el NavHost real. */
@RunWith(RobolectricTestRunner::class)
class FlujoAccesoTest {

    @get:Rule
    val compose = createComposeRule()

    @Before
    fun abrirApp() {
        compose.setContent {
            ReciclaKidsTheme { ReciclaKidsApp(contenedor = contenedorDePrueba()) }
        }
    }

    private fun campo(etiqueta: String) = compose.onNode(hasSetTextAction() and hasText(etiqueta))

    private fun saltarBienvenida() {
        compose.onNodeWithContentDescription("ReciclaKids. Toca la pantalla para continuar").performClick()
        compose.onNodeWithText("¡A jugar!").assertIsDisplayed()
    }

    private fun pasarPuertaDeAdultos() {
        compose.onNodeWithText("Adultos").performClick()
        compose.onNodeWithContentDescription("Mantén presionado tres segundos para entrar")
            .performTouchInput { down(center) }
        // El reloj automático recorre los 3 s del anillo; queda la pausa breve antes de abrir.
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(1_000)
        compose.onNodeWithText("Inicia sesión").assertIsDisplayed()
    }

    private fun iniciarSesion(correo: String, contrasena: String) {
        campo("Correo").performTextInput(correo)
        campo("Contraseña").performTextInput(contrasena)
        compose.onNodeWithText("Entrar").performScrollTo().performClick()
    }

    @Test
    fun laBienvenidaAvanzaSolaAlSelector() {
        compose.mainClock.advanceTimeBy(3_000)

        compose.onNodeWithText("¡A jugar!").assertIsDisplayed()
    }

    @Test
    fun jugarLlevaAlModoNinoSinPasarPorLaPuerta() {
        saltarBienvenida()

        compose.onNodeWithText("¡A jugar!").performClick()

        compose.onNodeWithText("Toca los números que te dijo la profe").assertIsDisplayed()
    }

    @Test
    fun unaCuentaQueNoExisteNoEntra() {
        saltarBienvenida()
        pasarPuertaDeAdultos()

        iniciarSesion("nadie@correo.com", "clave-de-prueba")

        compose.onNodeWithText("Correo o contraseña incorrectos").assertIsDisplayed()
    }

    @Test
    fun laDocenteSeRegistraYSuSesionAbreElModoDocente() {
        saltarBienvenida()
        pasarPuertaDeAdultos()
        compose.onNodeWithText("Crear cuenta").performScrollTo().performClick()

        campo("Nombre y apellido").performTextInput("Laura Restrepo")
        campo("Correo institucional").performTextInput("laura.r@jardin.edu.co")
        campo("Jardín infantil").performTextInput("Jardín Gotitas")
        campo("Grupo a cargo").performTextInput("Jardín B")
        campo("Contraseña").performTextInput("clave-de-prueba")
        compose.onNode(isToggleable()).performScrollTo().performClick()
        compose.onNodeWithText("Crear cuenta de docente").performScrollTo().performClick()

        compose.onNodeWithText("Cuenta creada. Ya puedes iniciar sesión.").assertIsDisplayed()
        iniciarSesion("laura.r@jardin.edu.co", "clave-de-prueba")

        compose.onNodeWithText("Hola, Laura").assertIsDisplayed()
    }

    @Test
    fun elAcudienteAutorizaLosDatosYSuSesionAbreElModoPadres() {
        saltarBienvenida()
        pasarPuertaDeAdultos()
        compose.onNodeWithText("Crear cuenta").performScrollTo().performClick()
        compose.onNodeWithText("Acudiente").performClick()

        campo("Tu nombre").performTextInput("Mariana Ríos")
        campo("Correo").performTextInput("mariana.r@correo.com")
        campo("Contraseña").performTextInput("clave-de-prueba")
        campo("Código de vinculación").performTextInput("JB-2M91")
        compose.onNodeWithText("Continuar").performScrollTo().performClick()

        compose.onNodeWithText("Datos de tu hijo o hija").assertIsDisplayed()
        compose.onNode(isToggleable()).performScrollTo().performClick()
        compose.onNodeWithText("Crear cuenta").performScrollTo().performClick()

        compose.onNodeWithText("Cuenta creada. Ya puedes iniciar sesión.").assertIsDisplayed()
        iniciarSesion("mariana.r@correo.com", "clave-de-prueba")

        // UI-29 abre con el hijo o hija vinculado y el atajo al reporte.
        compose.onNodeWithText("Salomé M.").assertIsDisplayed()
        compose.onNodeWithText("Ver el reporte completo").performScrollTo().assertIsDisplayed()
    }
}
