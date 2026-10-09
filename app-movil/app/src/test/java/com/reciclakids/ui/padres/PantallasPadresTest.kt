package com.reciclakids.ui.padres

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.reciclakids.model.ControlParental
import com.reciclakids.model.CuentaAdulto
import com.reciclakids.model.Dificultad
import com.reciclakids.model.RolAdulto
import com.reciclakids.network.ControlesParentales
import com.reciclakids.network.DatosDemoPadres
import com.reciclakids.network.Respuesta
import com.reciclakids.network.ServicioPadresEnMemoria
import com.reciclakids.ui.theme.ReciclaKidsTheme
import com.reciclakids.viewmodel.EstadoUi
import com.reciclakids.viewmodel.EstadoVinculacion
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

/** Las pantallas del Modo Padres con los datos de la demo, el viernes 18 de septiembre de 2026. */
@RunWith(RobolectricTestRunner::class)
class PantallasPadresTest {

    @get:Rule
    val compose = createComposeRule()

    private val viernes = LocalDate.of(2026, 9, 18)
    private val servicio = ServicioPadresEnMemoria(
        CuentaAdulto("Mariana Ríos", "mariana.r@correo.com", RolAdulto.Acudiente),
        ControlesParentales(),
        latenciaMs = 0,
        reloj = Clock.fixed(viernes.atTime(18, 30).toInstant(ZoneOffset.UTC), ZoneOffset.UTC),
    )
    private val salome = DatosDemoPadres.IdSalome
    private val martin = DatosDemoPadres.IdMartin

    private fun <T> datos(consulta: suspend ServicioPadresEnMemoria.() -> Respuesta<T>): T =
        (runBlocking { servicio.consulta() } as Respuesta.Ok).dato

    private val hijos by lazy { datos { hijos() } }

    // --- UI-29 Inicio ---

    @Test
    fun elInicioSeLeeEnCincoSegundosYLlevaAlReporte() {
        var reporte = 0
        var logros = 0
        compose.setContent {
            ReciclaKidsTheme {
                InicioPadresScreen(
                    hijo = hijos[0],
                    hijos = hijos,
                    estado = EstadoUi.Contenido(datos { inicio(salome) }),
                    onElegirHijo = {}, onReintentar = {},
                    onVerReporte = { reporte++ }, onVerLogros = { logros++ }, onControlParental = {},
                )
            }
        }

        compose.onNodeWithText("Esta semana · 14 – 18 de septiembre").assertIsDisplayed()
        compose.onNodeWithContentDescription("4 de 5 retos completados esta semana").assertIsDisplayed()
        compose.onNodeWithText("83\u00A0%").assertIsDisplayed()
        compose.onNodeWithText("Amiga tortuga").assertIsDisplayed()
        compose.onNodeWithText("Último logro · ayer").assertIsDisplayed()
        compose.onNodeWithText("Esta semana mejoró separando orgánicos: pasó de 67\u00A0% a 88\u00A0% de aciertos.").performScrollTo().assertIsDisplayed()

        compose.onNodeWithText("Ver el reporte completo").performScrollTo().performClick()
        compose.onNodeWithText("Logros · 4 de 8").performScrollTo().performClick()
        assertEquals(1, reporte)
        assertEquals(1, logros)
        compose.onNodeWithText("Solo ves la información de Salomé M.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun conDosHijosElSelectorCambiaDeHijo() {
        var elegido: String? = null
        compose.setContent {
            ReciclaKidsTheme {
                InicioPadresScreen(
                    hijo = hijos[0],
                    hijos = hijos,
                    estado = EstadoUi.Contenido(datos { inicio(salome) }),
                    onElegirHijo = { elegido = it.id }, onReintentar = {},
                    onVerReporte = {}, onVerLogros = {}, onControlParental = {},
                )
            }
        }

        compose.onNodeWithText("Salomé M.").performClick()
        compose.onNodeWithText("Martín M.").performClick()

        assertEquals(martin, elegido)
    }

    @Test
    fun unHijoQueTodaviaNoJuegaInvitaARevisarLosCorreos() {
        var control = 0
        compose.setContent {
            ReciclaKidsTheme {
                InicioPadresScreen(
                    hijo = hijos[1],
                    hijos = hijos,
                    estado = EstadoUi.Vacio(datos { inicio(martin) }),
                    onElegirHijo = {}, onReintentar = {},
                    onVerReporte = {}, onVerLogros = {}, onControlParental = { control++ },
                )
            }
        }

        compose.onNodeWithText("Todavía no hay reportes").assertIsDisplayed()
        compose.onNodeWithText("Revisar preferencias de correo").performScrollTo().performClick()
        assertEquals(1, control)
    }

    @Test
    fun unErrorDeRedOfreceReintentar() {
        var reintentos = 0
        compose.setContent {
            ReciclaKidsTheme {
                InicioPadresScreen(
                    hijo = hijos[0], hijos = hijos, estado = EstadoUi.Error,
                    onElegirHijo = {}, onReintentar = { reintentos++ },
                    onVerReporte = {}, onVerLogros = {}, onControlParental = {},
                )
            }
        }

        compose.onNodeWithText("No pudimos cargar la información").assertIsDisplayed()
        compose.onNodeWithText("Reintentar").performClick()
        assertEquals(1, reintentos)
    }

    // --- UI-30 Reporte semanal ---

    @Test
    fun elReporteExplicaLaSemanaYCambiaDeSemana() {
        var semana: LocalDate? = null
        var logros = 0
        compose.setContent {
            ReciclaKidsTheme {
                ReporteSemanalScreen(
                    estado = EstadoUi.Contenido(datos { reporte(salome, null) }),
                    cambiandoSemana = false,
                    onSemana = { semana = it },
                    onReintentar = {}, onVolver = {}, onVerLogros = { logros++ },
                )
            }
        }

        compose.onNodeWithText("Esta semana").assertIsSelected()
        compose.onNodeWithText("+11 pts").assertIsDisplayed()
        compose.onNodeWithText("No aprovechables").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("1 h 12 min").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Promedio de 14 min al día, por debajo del límite de 20 min que definiste.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Pulpo ordenado").performScrollTo().assertIsDisplayed()

        compose.onNodeWithText("7–11 sep").performClick()
        compose.onNodeWithText("Ver todas").performScrollTo().performClick()
        assertEquals(LocalDate.of(2026, 9, 7), semana)
        assertEquals(1, logros)
    }

    @Test
    fun laFraseDelReporteDiceQueResiduoConfundeYComoPracticar() {
        compose.setContent {
            ReciclaKidsTheme {
                ReporteSemanalScreen(
                    estado = EstadoUi.Contenido(datos { reporte(salome, null) }),
                    cambiandoSemana = false, onSemana = {}, onReintentar = {}, onVolver = {}, onVerLogros = {},
                )
            }
        }

        compose.onNodeWithText(
            "Esta semana mejoró separando orgánicos: pasó de 67\u00A0% a 88\u00A0% de aciertos. " +
                "Todavía confunde el empaque metalizado: en casa pueden mirar juntos qué envolturas no se reciclan.",
        ).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun sinSemanasJugadasElReporteLoExplica() {
        compose.setContent {
            ReciclaKidsTheme {
                ReporteSemanalScreen(
                    estado = EstadoUi.Vacio(datos { reporte(martin, null) }),
                    cambiandoSemana = false, onSemana = {}, onReintentar = {}, onVolver = {}, onVerLogros = {},
                )
            }
        }

        compose.onNodeWithText("Aún no hay semanas para comparar").assertIsDisplayed()
    }

    // --- UI-31 Logros e insignias ---

    @Test
    fun losLogrosDicenCuandoSeGanaronYCuantoFalta() {
        compose.setContent {
            ReciclaKidsTheme {
                LogrosHijoScreen(estado = EstadoUi.Contenido(datos { logros(salome) }), onReintentar = {}, onVolver = {})
            }
        }

        compose.onNodeWithText("Logros de Salomé M.").assertIsDisplayed()
        compose.onNodeWithText("4 de 8").assertIsDisplayed()
        compose.onNodeWithContentDescription("Amiga tortuga, ganada. Ganada ayer").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Agua cristalina, por descubrir. Faltan 2 retos").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Coral feliz, por descubrir. Racha de 10").performScrollTo().assertIsDisplayed()
    }

    // --- UI-32 Notificaciones ---

    @Test
    fun elHistorialMuestraCadaCorreoConSuTipo() {
        compose.setContent {
            ReciclaKidsTheme {
                NotificacionesScreen(
                    estado = EstadoUi.Contenido(datos { correos(salome) }),
                    onReintentar = {}, onVolver = {}, onPreferencias = {},
                )
            }
        }

        compose.onNodeWithText("Esta semana").assertIsDisplayed()
        // El más reciente primero: el reporte de hoy a las 5:00 p. m.
        compose.onAllNodesWithText("Reporte semanal listo").onFirst().assertIsDisplayed()
        compose.onNodeWithText("4 de 5 retos · 83\u00A0% de aciertos").assertIsDisplayed()
        compose.onNodeWithText("Nuevo logro: Amiga tortuga").assertIsDisplayed()
        compose.onNodeWithText("ayer, 6:10\u00A0p. m. · enviado a mariana.r@correo.com").assertIsDisplayed()
    }

    @Test
    fun sinCorreosElHistorialLlevaALasPreferencias() {
        var preferencias = 0
        compose.setContent {
            ReciclaKidsTheme {
                NotificacionesScreen(
                    estado = EstadoUi.Vacio(datos { correos(martin) }),
                    onReintentar = {}, onVolver = {}, onPreferencias = { preferencias++ },
                )
            }
        }

        compose.onNodeWithText("Sin correos por ahora").assertIsDisplayed()
        compose.onNodeWithText("Cambiar preferencias de correo").performClick()
        assertEquals(1, preferencias)
    }

    // --- UI-33 Control parental ---

    @Test
    fun elControlParentalCambiaDificultadTiempoYCorreos() {
        var control by mutableStateOf(ControlParental())
        compose.setContent {
            ReciclaKidsTheme {
                ControlParentalScreen(
                    nombreHijo = "Salomé M.",
                    estado = EstadoUi.Contenido(control),
                    onCambio = { control = it },
                    onReintentar = {}, onVolver = {},
                    snackbar = remember { SnackbarHostState() },
                )
            }
        }

        compose.onNodeWithText("Fácil").performClick()
        compose.onNodeWithContentDescription("Más tiempo").performScrollTo().performClick()
        compose.onNodeWithText("25 min").assertIsDisplayed()
        compose.onNodeWithText("Avisarme de cada logro").performScrollTo().performClick()

        assertEquals(ControlParental(limiteMinutosDiarios = 25, dificultadSugerida = Dificultad.Facil, correosLogro = false), control)
    }

    @Test
    fun elTiempoNoBajaDeCincoMinutosYLosBotonesMiden48Dp() {
        compose.setContent {
            ReciclaKidsTheme {
                ControlParentalScreen(
                    nombreHijo = "Salomé M.",
                    estado = EstadoUi.Contenido(ControlParental(limiteMinutosDiarios = 5)),
                    onCambio = {}, onReintentar = {}, onVolver = {},
                    snackbar = remember { SnackbarHostState() },
                )
            }
        }

        compose.onNodeWithContentDescription("Menos tiempo").performScrollTo().assertIsNotEnabled().assertHeightIsAtLeast(48.dp)
        compose.onNodeWithContentDescription("Más tiempo").assertIsEnabled().assertHeightIsAtLeast(48.dp)
    }

    // --- UI-34 Privacidad y datos del menor ---

    @Test
    fun eliminarLosDatosPideUnaCasillaExplicitaAntesDeEnviar() {
        var solicitudes = 0
        compose.setContent {
            ReciclaKidsTheme {
                PrivacidadDatosScreen(
                    estado = EstadoUi.Contenido(datos { privacidad(salome) }),
                    enviando = false,
                    onDescargar = {}, onVerPolitica = {},
                    onSolicitarEliminacion = { solicitudes++ },
                    onCancelarSolicitud = {}, onReintentar = {}, onVolver = {},
                    snackbar = remember { SnackbarHostState() },
                )
            }
        }

        compose.onNodeWithText("El 21 de agosto de 2026 por Mariana Ríos · Ley 1581 de 2012").assertIsDisplayed()
        compose.onNodeWithText("13 registros").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Solicitar eliminación de datos").performScrollTo().performClick()

        compose.onNodeWithText("¿Eliminar los datos de Salomé M.?").assertIsDisplayed()
        compose.onNodeWithText("Enviar solicitud").assertIsNotEnabled()
        compose.onNodeWithText(
            "Entiendo que esta acción borra de forma permanente los datos de Salomé M. y retiro la autorización de tratamiento.",
        ).performClick()
        compose.onNodeWithText("Enviar solicitud").assertIsEnabled().performClick()

        assertEquals(1, solicitudes)
    }

    @Test
    fun unaSolicitudPendienteSeVeYSePuedeCancelar() {
        var canceladas = 0
        val pendiente = datos { privacidad(salome) }.copy(solicitudEliminacion = viernes)
        compose.setContent {
            ReciclaKidsTheme {
                PrivacidadDatosScreen(
                    estado = EstadoUi.Contenido(pendiente),
                    enviando = false,
                    onDescargar = {}, onVerPolitica = {}, onSolicitarEliminacion = {},
                    onCancelarSolicitud = { canceladas++ },
                    onReintentar = {}, onVolver = {},
                    snackbar = remember { SnackbarHostState() },
                )
            }
        }

        compose.onNodeWithText("Solicitud enviada").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Cancelar solicitud").performScrollTo().performClick()
        assertEquals(1, canceladas)
    }

    // --- UI-35 Cuenta y vincular ---

    @Test
    fun laCuentaListaLosHijosYLlevaAVincularOtro() {
        var vincular = 0
        var control = 0
        compose.setContent {
            ReciclaKidsTheme {
                CuentaPadresScreen(
                    estado = EstadoUi.Contenido(datos { perfil() }),
                    onReintentar = {}, onEditarPerfil = {}, onCambiarContrasena = {},
                    onControlParental = { control++ }, onPrivacidad = {},
                    onVincular = { vincular++ }, onCerrarSesion = {},
                    snackbar = remember { SnackbarHostState() },
                )
            }
        }

        compose.onNodeWithText("mariana.r@correo.com").assertIsDisplayed()
        compose.onNodeWithText("Control parental").performClick()
        compose.onNodeWithText("Martín M.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Vincular otro hijo o hija").performScrollTo().performClick()

        assertEquals(1, control)
        assertEquals(1, vincular)
    }

    @Test
    fun vincularSoloSeActivaConElCodigoCompletoYAvisaSiNoExiste() {
        var codigo by mutableStateOf("JB-2M")
        var estado: EstadoVinculacion by mutableStateOf(EstadoVinculacion.Editando)
        compose.setContent {
            ReciclaKidsTheme {
                VincularHijoScreen(codigo = codigo, estado = estado, onCodigo = {}, onVincular = {}, onVolver = {})
            }
        }

        compose.onNodeWithText("Vincular").assertIsNotEnabled()
        codigo = "ZZ-0000"
        estado = EstadoVinculacion.CodigoInvalido
        compose.onNodeWithText("Vincular").assertIsEnabled()
        compose.onNodeWithText("Ese código no existe o ya se usó. Pídele uno nuevo a la docente.").assertIsDisplayed()
    }
}
