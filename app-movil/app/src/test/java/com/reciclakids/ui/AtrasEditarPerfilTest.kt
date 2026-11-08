package com.reciclakids.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.reciclakids.network.DatosPerfilAcudiente
import com.reciclakids.ui.padres.EditarPerfilAcudienteScreen
import com.reciclakids.ui.theme.ReciclaKidsTheme
import com.reciclakids.viewmodel.EstadoGuardado
import com.reciclakids.viewmodel.EstadoUi
import com.reciclakids.viewmodel.FormularioPerfilAcudiente
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Hallazgo 5 de la auditoría: el botón atrás del sistema no respondía en «Editar perfil». Se
 * arma como en la app: un NavHost del modo dentro del NavHost raíz.
 */
@RunWith(RobolectricTestRunner::class)
class AtrasEditarPerfilTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val marta = DatosPerfilAcudiente("Marta Ruiz", "marta@correo.com")

    private var rutaDelModo: String? = null

    private fun montar(formulario: FormularioPerfilAcudiente) {
        compose.setContent {
            ReciclaKidsTheme {
                val raiz = rememberNavController()
                NavHost(raiz, startDestination = "padres") {
                    composable("padres") {
                        val modo = rememberNavController()
                        val entrada by modo.currentBackStackEntryAsState()
                        rutaDelModo = entrada?.destination?.route
                        NavHost(modo, startDestination = "cuenta") {
                            composable("cuenta") { Text("Cuenta") }
                            composable("editar") {
                                EditarPerfilAcudienteScreen(
                                    estado = EstadoUi.Contenido(formulario),
                                    guardado = EstadoGuardado.Editando,
                                    onGuardar = {},
                                    onReintentar = {},
                                    onVolver = { modo.popBackStack() },
                                )
                            }
                        }
                        LaunchedEffect(Unit) { modo.navigate("editar") }
                    }
                }
            }
        }
        compose.waitForIdle()
        assertEquals("editar", rutaDelModo)
    }

    private fun atrasDelSistema() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    @Test
    fun sinCambiosElAtrasDelSistemaVuelveALaCuenta() {
        montar(FormularioPerfilAcudiente(marta))

        atrasDelSistema()

        assertEquals("cuenta", rutaDelModo)
    }

    @Test
    fun conCambiosElAtrasDelSistemaPreguntaAntesDeDescartar() {
        val formulario = FormularioPerfilAcudiente(marta)
        montar(formulario)
        compose.runOnUiThread { formulario.nombre = "Marta R." }
        compose.waitForIdle()

        atrasDelSistema()

        compose.onNodeWithText("¿Descartar los cambios?").assertIsDisplayed()
        assertEquals("editar", rutaDelModo)
        compose.onNodeWithText("Descartar").performClick()
        compose.waitForIdle()
        assertEquals("cuenta", rutaDelModo)
    }
}
