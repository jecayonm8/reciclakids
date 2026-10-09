package com.reciclakids.ui.docente

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.reciclakids.model.AvisosDocente
import com.reciclakids.model.BibliotecaDocente
import com.reciclakids.model.CategoriaResiduo
import com.reciclakids.model.Confusion
import com.reciclakids.model.DetalleNino
import com.reciclakids.model.Dificultad
import com.reciclakids.model.EstadoNinoHoy
import com.reciclakids.model.EstadoReto
import com.reciclakids.model.GrupoHoy
import com.reciclakids.model.InsigniaNino
import com.reciclakids.model.NinoHoy
import com.reciclakids.model.PerfilDocente
import com.reciclakids.model.Reto
import com.reciclakids.model.TableroDocente
import com.reciclakids.ui.theme.ReciclaKidsTheme
import com.reciclakids.ui.theme.Tactil
import com.reciclakids.viewmodel.AsistenteReto
import com.reciclakids.viewmodel.EstadoPublicacion
import com.reciclakids.viewmodel.EstadoUi
import com.reciclakids.viewmodel.FiltroReto
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class PantallasDocenteTest {

    @get:Rule
    val compose = createComposeRule()

    private val martes = LocalDate.of(2026, 9, 22)
    private val lonchera = Reto(
        "reto-lonchera", "Clasificar la lonchera", setOf(CategoriaResiduo.Aprovechables, CategoriaResiduo.Organicos),
        Dificultad.Medio, martes, EstadoReto.Publicado, codigo = "4729", aciertosPromedio = 91,
    )
    private val refrigerio = Reto(
        "reto-refrigerio", "Después del refrigerio", setOf(CategoriaResiduo.Organicos),
        Dificultad.Facil, martes.plusDays(1), EstadoReto.Programado,
    )
    private val mezcla = Reto(
        "reto-mezcla", "Mezcla del día", CategoriaResiduo.entries.toSet(), Dificultad.Dificil, null, EstadoReto.Borrador,
    )

    private fun tablero(retoHoy: Reto? = lonchera) = TableroDocente(martes, "Laura Restrepo", "Jardín B", retoHoy, 18, 22, 91)

    // --- UI-21 Tablero ---

    @Test
    fun elTableroMuestraElCodigoActivoYLlevaAMostrarloAlGrupo() {
        var mostrado: String? = null
        compose.setContent {
            ReciclaKidsTheme {
                TableroDocenteScreen(
                    EstadoUi.Contenido(tablero()),
                    onReintentar = {}, onCrear = {}, onReutilizar = {},
                    onMostrarCodigo = { mostrado = it }, onVerGrupo = {},
                )
            }
        }

        compose.onNodeWithText("Hola, Laura").assertIsDisplayed()
        compose.onNodeWithContentDescription("Código del reto: 4 7 2 9").assertIsDisplayed()
        compose.onNodeWithText("Mostrar código").performScrollTo().performClick()

        assertEquals("reto-lonchera", mostrado)
    }

    @Test
    fun sinRetoHoyElTableroInvitaACrearOAReutilizar() {
        var creados = 0
        var reutilizados = 0
        compose.setContent {
            ReciclaKidsTheme {
                TableroDocenteScreen(
                    EstadoUi.Vacio(tablero(retoHoy = null)),
                    onReintentar = {}, onCrear = { creados++ }, onReutilizar = { reutilizados++ },
                    onMostrarCodigo = {}, onVerGrupo = {},
                )
            }
        }

        compose.onNodeWithText("Hoy todavía no hay reto").assertIsDisplayed()
        compose.onNodeWithText("Crear reto").performClick()
        compose.onNodeWithText("Reutilizar uno anterior").performClick()

        assertEquals(1, creados)
        assertEquals(1, reutilizados)
    }

    @Test
    fun unErrorDeRedOfreceReintentar() {
        var reintentos = 0
        compose.setContent {
            ReciclaKidsTheme {
                TableroDocenteScreen(
                    EstadoUi.Error,
                    onReintentar = { reintentos++ }, onCrear = {}, onReutilizar = {}, onMostrarCodigo = {}, onVerGrupo = {},
                )
            }
        }

        compose.onNodeWithText("No pudimos cargar esta información").assertIsDisplayed()
        compose.onNodeWithText("Reintentar").performClick()

        assertEquals(1, reintentos)
    }

    // --- UI-22 Crear reto ---

    @Composable
    private fun AsistenteDePrueba(
        inicial: AsistenteReto,
        publicacion: EstadoPublicacion = EstadoPublicacion.Ninguna,
        onPublicar: () -> Unit = {},
        onMostrarCodigo: (Reto) -> Unit = {},
    ) {
        var asistente by remember { mutableStateOf(inicial) }
        CrearRetoWizard(
            asistente = asistente,
            publicacion = publicacion,
            hojaReutilizar = false,
            previos = EstadoUi.Cargando,
            onCambio = { cambio -> asistente = cambio(asistente) },
            onAbrirReutilizar = {}, onCerrarReutilizar = {}, onReintentarPrevios = {}, onUsar = {},
            onPublicar = onPublicar, onCerrar = {}, onIrTablero = {}, onMostrarCodigo = onMostrarCodigo, onVerBiblioteca = {},
        )
    }

    @Test
    fun sinCanecasElAsistenteNoAvanza() {
        compose.setContent { ReciclaKidsTheme { AsistenteDePrueba(AsistenteReto(martes)) } }

        compose.onNodeWithText("Siguiente").assertIsEnabled()
        compose.onNodeWithText("Aprovechables").performScrollTo().performClick()
        compose.onNodeWithText("Orgánicos").performScrollTo().performClick()

        compose.onNodeWithText("Siguiente").assertIsNotEnabled()
        compose.onNodeWithText("2. Dificultad").assertIsNotEnabled()
        compose.onNodeWithText("Paso 1 de 5 · elige al menos una caneca").assertIsDisplayed()
    }

    @Test
    fun programarMuestraLosDiasYLaVistaPreviaLoResume() {
        compose.setContent { ReciclaKidsTheme { AsistenteDePrueba(AsistenteReto(martes, paso = 3)) } }

        compose.onNodeWithText("Programar").performClick()
        compose.onNodeWithText("mié 23").assertIsDisplayed()
        compose.onNodeWithText("jue 24").performScrollTo().performClick()
        compose.onNodeWithText("Siguiente").performClick()

        compose.onNodeWithText("Así lo verá el niño").assertIsDisplayed()
        compose.onNodeWithText("Programado para jue 24").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Todo listo").assertIsDisplayed()
    }

    @Test
    fun alPublicarSeMuestraElCodigoDelDiaParaElGrupo() {
        var mostrado: Reto? = null
        compose.setContent {
            ReciclaKidsTheme {
                AsistenteDePrueba(
                    AsistenteReto(martes, paso = 5),
                    publicacion = EstadoPublicacion.Publicado(lonchera),
                    onMostrarCodigo = { mostrado = it },
                )
            }
        }

        compose.onNodeWithText("¡Reto publicado!").assertIsDisplayed()
        compose.onNodeWithContentDescription("Código del reto: 4 7 2 9").assertIsDisplayed()
        compose.onNodeWithText("Mostrar al grupo").performClick()

        assertEquals(lonchera, mostrado)
    }

    @Test
    fun siFallaLaPublicacionQuedaComoBorradorYSePuedeReintentar() {
        var publicaciones = 0
        compose.setContent {
            ReciclaKidsTheme {
                AsistenteDePrueba(
                    AsistenteReto(martes, paso = 5),
                    publicacion = EstadoPublicacion.Fallida(mezcla),
                    onPublicar = { publicaciones++ },
                )
            }
        }

        compose.onNodeWithText("No se pudo publicar").assertIsDisplayed()
        compose.onNodeWithText("Ver borrador").assertIsDisplayed()
        compose.onNodeWithText("Reintentar").performClick()

        assertEquals(1, publicaciones)
    }

    // --- UI-23 Biblioteca ---

    @Composable
    private fun BibliotecaDePrueba(retos: List<Reto>, onEditar: (Reto) -> Unit = {}) {
        var filtro by remember { mutableStateOf(FiltroReto.Todos) }
        BibliotecaRetosScreen(
            estado = EstadoUi.Contenido(BibliotecaDocente(martes, retos)),
            filtro = filtro,
            onFiltro = { filtro = it },
            onReintentar = {}, onCrear = {}, onDuplicar = {}, onVerResultados = {}, onEditar = onEditar,
        )
    }

    @Test
    fun losFiltrosDeLaBibliotecaDejanSoloLosRetosQueCoinciden() {
        compose.setContent { ReciclaKidsTheme { BibliotecaDePrueba(listOf(lonchera, refrigerio, mezcla)) } }

        compose.onNodeWithText("3 de 3 retos").assertIsDisplayed()
        compose.onNodeWithText("No aprovechables").performClick()

        compose.onNodeWithText("1 de 3 retos").assertIsDisplayed()
        compose.onNodeWithText("Mezcla del día").assertIsDisplayed()
        compose.onAllNodesWithText("Clasificar la lonchera").assertCountEquals(0)
    }

    @Test
    fun unFiltroSinResultadosSeQuitaConUnToque() {
        compose.setContent { ReciclaKidsTheme { BibliotecaDePrueba(listOf(lonchera)) } }

        compose.onNodeWithText("Fácil").performClick()
        compose.onNodeWithText("Ningún reto con ese filtro").assertIsDisplayed()
        compose.onNodeWithText("Quitar filtros").performClick()

        compose.onNodeWithText("Clasificar la lonchera").assertIsDisplayed()
    }

    @Test
    fun soloSeEditanProgramadosYBorradoresYUnBorradorNoTieneResultados() {
        var editado: Reto? = null
        compose.setContent { ReciclaKidsTheme { BibliotecaDePrueba(listOf(lonchera, refrigerio, mezcla), onEditar = { editado = it }) } }

        compose.onAllNodesWithText("Editar").assertCountEquals(2)
        compose.onAllNodesWithText("Ver resultados")[0].assertIsEnabled()
        compose.onAllNodesWithText("Ver resultados")[2].assertIsNotEnabled()
        compose.onAllNodesWithText("Editar")[0].performScrollTo().performClick()

        assertEquals(refrigerio, editado)
    }

    // --- UI-24 Código publicado ---

    @Test
    fun elCodigoPublicadoMuestraLosDigitosLaVigenciaYCuantosEntraron() {
        compose.setContent {
            ReciclaKidsTheme {
                CodigoPublicadoScreen(EstadoUi.Contenido(lonchera), martes, ninosQueEntraron = 14, onVolver = {}, onReintentar = {})
            }
        }

        compose.onNodeWithText("Escriban este código").assertIsDisplayed()
        compose.onNodeWithContentDescription("Código del reto: 4 7 2 9").assertIsDisplayed()
        compose.onNodeWithText("Vence hoy a medianoche").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("14 niños ya entraron").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun unCodigoVencidoNoSeMuestra() {
        compose.setContent {
            ReciclaKidsTheme {
                CodigoPublicadoScreen(
                    EstadoUi.Contenido(lonchera.copy(fecha = martes.minusDays(1))), martes,
                    ninosQueEntraron = null, onVolver = {}, onReintentar = {},
                )
            }
        }

        compose.onNodeWithText("Este reto ya no tiene código activo").assertIsDisplayed()
        compose.onAllNodesWithText("Escriban este código").assertCountEquals(0)
    }

    // --- UI-25 Mi grupo ---

    @Test
    fun cadaFilaDelGrupoDiceElEstadoYAbreElDetalle() {
        var abierto: String? = null
        compose.setContent {
            ReciclaKidsTheme {
                MiGrupoScreen(
                    EstadoUi.Contenido(
                        GrupoHoy(
                            "Jardín B", "Clasificar la lonchera",
                            listOf(
                                NinoHoy("n01", "Salomé M.", EstadoNinoHoy.Completo(3, 8, 8)),
                                NinoHoy("n03", "Ana L.", EstadoNinoHoy.EnCurso(4)),
                                NinoHoy("n05", "Sara P.", EstadoNinoHoy.SinEmpezar),
                            ),
                        )
                    ),
                    onReintentar = {}, onNino = { abierto = it }, onIrACuenta = {},
                )
            }
        }

        compose.onNodeWithText("Mi grupo · Jardín B").assertIsDisplayed()
        compose.onNodeWithText("Completó en 3 min · 8 de 8 aciertos").assertIsDisplayed()
        compose.onNodeWithText("Va en el residuo 4").assertIsDisplayed()
        compose.onNodeWithText("Sin empezar").assertIsDisplayed()
        compose.onNodeWithText("Ana L.").performClick()

        assertEquals("n03", abierto)
    }

    // --- UI-27 Detalle de un niño ---

    @Test
    fun elDetalleDiceEnQueCanecaSeConfundeYQueRetoLeAyudaria() {
        compose.setContent {
            ReciclaKidsTheme {
                DetalleNinoScreen(
                    EstadoUi.Contenido(
                        DetalleNino(
                            id = "n02",
                            nombre = "Juan T.",
                            aciertosPorSemana = listOf(62, 71, 68, 74),
                            aciertosPorCategoria = mapOf(
                                CategoriaResiduo.Aprovechables to 94,
                                CategoriaResiduo.NoAprovechables to 61,
                                CategoriaResiduo.Organicos to 88,
                            ),
                            confusion = Confusion("el empaque metalizado", CategoriaResiduo.NoAprovechables, CategoriaResiduo.Aprovechables),
                            insignias = listOf(InsigniaNino("Amiga tortuga", true), InsigniaNino("Pulpo ordenado", false)),
                        )
                    ),
                    onVolver = {},
                    onReintentar = {},
                )
            }
        }

        compose.onNodeWithText("Juan T.").assertIsDisplayed()
        compose.onNodeWithText("+12 pts en 4 semanas").assertIsDisplayed()
        compose.onNodeWithText("Confunde el empaque metalizado con aprovechables. Un reto solo de caneca negra le ayudaría.")
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText("Insignias · 1 de 2").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Pulpo ordenado, todavía no").performScrollTo().assertIsDisplayed()
    }

    // --- UI-28 Cuenta ---

    @Composable
    private fun CuentaDePrueba(onAvisos: (AvisosDocente) -> Unit = {}, onEliminar: () -> Unit = {}, onCompartir: (String) -> Unit = {}) {
        CuentaDocenteScreen(
            estado = EstadoUi.Contenido(
                PerfilDocente("Laura Restrepo", "laura.r@jardin.edu.co", "Gotitas", "Jardín B", 22, "JB-2M91", AvisosDocente())
            ),
            onReintentar = {}, onAvisos = onAvisos, onCompartirCodigo = onCompartir, onRegenerarCodigo = {},
            onEditarPerfil = {}, onVerPolitica = {}, onCerrarSesion = {}, onEliminarDatos = onEliminar,
        )
    }

    @Test
    fun eliminarLosDatosDelGrupoPideConfirmacion() {
        var eliminaciones = 0
        compose.setContent { ReciclaKidsTheme { CuentaDePrueba(onEliminar = { eliminaciones++ }) } }

        compose.onNodeWithText("Eliminar datos del grupo").performScrollTo().performClick()
        compose.onNodeWithText("¿Eliminar los datos del grupo?").assertIsDisplayed()
        compose.onNodeWithText("Cancelar").performClick()
        assertEquals(0, eliminaciones)

        compose.onNodeWithText("Eliminar datos del grupo").performScrollTo().performClick()
        compose.onNodeWithText("Sí, eliminar").performClick()
        assertEquals(1, eliminaciones)
    }

    @Test
    fun cadaAvisoSeCambiaTocandoSuFilaYElCodigoSeComparte() {
        var avisos: AvisosDocente? = null
        var compartido: String? = null
        compose.setContent { ReciclaKidsTheme { CuentaDePrueba(onAvisos = { avisos = it }, onCompartir = { compartido = it }) } }

        compose.onNodeWithText("Avisarme de niños sin jugar").performScrollTo().performClick()
        compose.onNodeWithText("Compartir").performScrollTo().performClick()

        assertEquals(AvisosDocente(ninosSinJugar = true), avisos)
        assertEquals("JB-2M91", compartido)
    }

    @Test
    fun losBotonesDeLosModosAdultosMidenAlMenos48Dp() {
        compose.setContent { ReciclaKidsTheme { CuentaDePrueba() } }

        listOf("Editar perfil", "Compartir", "Regenerar", "Ver política", "Cerrar sesión").forEach {
            compose.onNodeWithText(it).performScrollTo().assertHeightIsAtLeast(Tactil.minimoAdulto)
        }
    }
}
