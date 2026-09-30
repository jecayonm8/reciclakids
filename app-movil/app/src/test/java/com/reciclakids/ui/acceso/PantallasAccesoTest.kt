package com.reciclakids.ui.acceso

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
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
import com.reciclakids.datos.acceso.RolAdulto
import com.reciclakids.ui.theme.ReciclaKidsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PantallasAccesoTest {

    @get:Rule
    val compose = createComposeRule()

    private fun campo(etiqueta: String) = compose.onNode(hasSetTextAction() and hasText(etiqueta))

    // --- Puerta para adultos ---

    private fun montarPuerta(onAbierta: () -> Unit) {
        compose.setContent {
            ReciclaKidsTheme {
                PuertaAdultosContenido(SumaPuerta(4, 3, listOf(5, 7, 9)), onAbierta = onAbierta, onCerrar = {})
            }
        }
    }

    @Test
    fun puertaNoSeAbreConUnaRespuestaEquivocada() {
        var aperturas = 0
        montarPuerta { aperturas++ }

        compose.onNodeWithText("5").performClick()
        compose.mainClock.advanceTimeBy(1_000)

        compose.onNodeWithText("No es esa, intenta de nuevo").assertIsDisplayed()
        assertEquals(0, aperturas)
    }

    @Test
    fun puertaSeAbreAlResolverLaSuma() {
        var aperturas = 0
        montarPuerta { aperturas++ }

        compose.onNodeWithText("7").performClick()
        compose.onNodeWithText("¡Correcto! Abriendo…").assertIsDisplayed()
        compose.mainClock.advanceTimeBy(1_000)

        assertEquals(1, aperturas)
    }

    /**
     * Con el reloj en manual, los cambios de estado que nacen de un toque solo llegan a la
     * composición cuando la prueba queda en reposo; por eso se espera antes y después de avanzar.
     */
    private fun avanzar(ms: Long) {
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(ms)
        compose.waitForIdle()
    }

    @Test
    fun puertaSeReiniciaSiSeSueltaAntesDeTresSegundos() {
        var aperturas = 0
        montarPuerta { aperturas++ }
        compose.mainClock.autoAdvance = false
        val circulo = compose.onNodeWithContentDescription("Mantén presionado tres segundos para entrar")

        circulo.performTouchInput { down(center) }
        avanzar(DuracionPuertaMs - 500L)
        compose.onNodeWithText("Sigue presionando…").assertIsDisplayed()
        circulo.performTouchInput { up() }
        avanzar(2_000)

        compose.onNodeWithText("Mantén el círculo presionado").assertIsDisplayed()
        assertEquals(0, aperturas)
    }

    @Test
    fun puertaSeAbreAlMantenerPresionadoTresSegundos() {
        var aperturas = 0
        montarPuerta { aperturas++ }
        compose.mainClock.autoAdvance = false
        val circulo = compose.onNodeWithContentDescription("Mantén presionado tres segundos para entrar")

        circulo.performTouchInput { down(center) }
        avanzar(DuracionPuertaMs + 500L)
        compose.onNodeWithText("¡Listo! Abriendo…").assertIsDisplayed()
        avanzar(1_000)

        assertEquals(1, aperturas)
    }

    // --- UI-07 Autorización de datos del menor ---

    @Test
    fun autorizacionNuncaVienePremarcadaYBloqueaCrearCuenta() {
        var creadas = 0
        compose.setContent {
            ReciclaKidsTheme {
                AutorizacionDatosMenorScreen(onAtras = {}, onCrearCuenta = { creadas++ }, onLeerPolitica = {})
            }
        }
        val casilla = compose.onNode(isToggleable())
        val crear = compose.onNodeWithText("Crear cuenta")

        casilla.assertIsOff()
        crear.assertIsNotEnabled()

        casilla.performScrollTo().performClick()
        crear.assertIsEnabled().performScrollTo().performClick()

        assertEquals(1, creadas)
    }

    // --- UI-04 / UI-05 Registro ---

    @Test
    fun registroDeAcudienteNoContinuaConCamposInvalidos() {
        var continuo = 0
        compose.setContent {
            ReciclaKidsTheme {
                RegistroScreen(
                    rol = RolAdulto.Acudiente, onCambiarRol = {}, docente = FormularioDocente(), acudiente = FormularioAcudiente(),
                    onAtras = {}, onCrearDocente = {}, onContinuarAcudiente = { continuo++ }, onLeerPolitica = {},
                )
            }
        }

        campo("Tu nombre").performTextInput("Mariana Ríos")
        campo("Correo").performTextInput("mariana.r")
        campo("Contraseña").performTextInput("corta")
        compose.onNodeWithText("Continuar").performScrollTo().performClick()

        compose.onNodeWithText("Escribe un correo válido").assertIsDisplayed()
        compose.onNodeWithText("Debe tener al menos 8 caracteres").assertIsDisplayed()
        compose.onNodeWithText("Completa este campo").assertIsDisplayed()
        assertEquals(0, continuo)
    }

    @Test
    fun registroDeAcudienteContinuaALaAutorizacionConDatosValidos() {
        val formulario = FormularioAcudiente()
        var continuo = 0
        compose.setContent {
            ReciclaKidsTheme {
                RegistroScreen(
                    rol = RolAdulto.Acudiente, onCambiarRol = {}, docente = FormularioDocente(), acudiente = formulario,
                    onAtras = {}, onCrearDocente = {}, onContinuarAcudiente = { continuo++ }, onLeerPolitica = {},
                )
            }
        }

        campo("Tu nombre").performTextInput("Mariana Ríos")
        campo("Correo").performTextInput("mariana.r@correo.com")
        campo("Contraseña").performTextInput("clave-de-prueba")
        campo("Código de vinculación").performTextInput("jb-2m91")
        compose.onNodeWithText("Continuar").performScrollTo().performClick()

        assertEquals(1, continuo)
        assertEquals("JB-2M91", formulario.aRegistro().codigoVinculacion)
    }

    @Test
    fun registroDeDocenteExigeAceptarLosTerminos() {
        val formulario = FormularioDocente().apply {
            nombre = "Laura Restrepo"
            correo = "laura.r@jardin.edu.co"
            jardin = "Jardín Gotitas"
            grupo = "Jardín B"
            contrasena = "clave-de-prueba"
        }
        var creadas = 0
        compose.setContent {
            ReciclaKidsTheme {
                RegistroScreen(
                    rol = RolAdulto.Docente, onCambiarRol = {}, docente = formulario, acudiente = FormularioAcudiente(),
                    onAtras = {}, onCrearDocente = { creadas++ }, onContinuarAcudiente = {}, onLeerPolitica = {},
                )
            }
        }
        val crear = compose.onNodeWithText("Crear cuenta de docente")

        compose.onNode(isToggleable()).assertIsOff()
        crear.assertIsNotEnabled()

        compose.onNode(isToggleable()).performScrollTo().performClick()
        crear.assertIsEnabled().performScrollTo().performClick()

        assertEquals(1, creadas)
    }

    // --- UI-03 Inicio de sesión ---

    @Test
    fun loginNoEnviaConCorreoInvalido() {
        var envios = 0
        compose.setContent {
            ReciclaKidsTheme {
                LoginScreen(onEntrar = { _, _ -> envios++ }, onOlvideContrasena = {}, onCrearCuenta = {}, onVolverInicio = {})
            }
        }

        campo("Correo").performTextInput("laura")
        campo("Contraseña").performTextInput("clave-de-prueba")
        compose.onNodeWithText("Entrar").performScrollTo().performClick()

        compose.onNodeWithText("Escribe un correo válido").assertIsDisplayed()
        assertEquals(0, envios)
    }

    @Test
    fun loginEntregaElCorreoSinEspacios() {
        var recibido: Pair<String, String>? = null
        compose.setContent {
            ReciclaKidsTheme {
                LoginScreen(
                    onEntrar = { correo, contrasena -> recibido = correo to contrasena },
                    onOlvideContrasena = {}, onCrearCuenta = {}, onVolverInicio = {},
                )
            }
        }

        campo("Correo").performTextInput(" laura.r@jardin.edu.co ")
        campo("Contraseña").performTextInput("clave-de-prueba")
        compose.onNodeWithText("Entrar").performScrollTo().performClick()

        assertEquals("laura.r@jardin.edu.co" to "clave-de-prueba", recibido)
    }

    @Test
    fun loginMuestraCredencialesInvalidasSinDecirCualFallo() {
        compose.setContent {
            ReciclaKidsTheme {
                LoginScreen(
                    onEntrar = { _, _ -> }, onOlvideContrasena = {}, onCrearCuenta = {}, onVolverInicio = {},
                    error = ErrorLogin.CredencialesInvalidas,
                )
            }
        }

        compose.onNodeWithText("Correo o contraseña incorrectos").assertIsDisplayed()
    }

    // --- UI-06 Recuperar contraseña ---

    @Test
    fun recuperarEnviaSoloConCorreoValido() {
        val enviados = mutableListOf<String>()
        compose.setContent {
            ReciclaKidsTheme { RecuperarContrasenaScreen(onAtras = {}, onEnviar = { enviados += it }) }
        }

        compose.onNodeWithText("Enviar enlace").performScrollTo().performClick()
        compose.onNodeWithText("Escribe un correo válido").assertIsDisplayed()

        campo("Correo").performTextInput("mariana.r@correo.com")
        compose.onNodeWithText("Enviar enlace").performScrollTo().performClick()

        assertEquals(listOf("mariana.r@correo.com"), enviados)
    }
}
