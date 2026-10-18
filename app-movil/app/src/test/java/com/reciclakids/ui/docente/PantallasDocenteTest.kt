package com.reciclakids.ui.docente

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
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
import com.reciclakids.model.NinoAgregado
import com.reciclakids.model.NinoHoy
import com.reciclakids.model.PerfilDocente
import com.reciclakids.model.RetoDocente
import com.reciclakids.model.TableroDocente
import com.reciclakids.network.DatosPerfilDocente
import com.reciclakids.ui.theme.ReciclaKidsTheme
import com.reciclakids.ui.theme.Tactil
import com.reciclakids.viewmodel.AsistenteReto
import com.reciclakids.viewmodel.EstadoAgregarNino
import com.reciclakids.viewmodel.EstadoGuardado
import com.reciclakids.viewmodel.EstadoPublicacion
import com.reciclakids.viewmodel.EstadoUi
import com.reciclakids.viewmodel.FiltroReto
import com.reciclakids.viewmodel.FormularioNino
import com.reciclakids.viewmodel.FormularioPerfilDocente
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
    private val lonchera = RetoDocente(
        "reto-lonchera", "Clasificar la lonchera", setOf(CategoriaResiduo.Aprovechable, CategoriaResiduo.Organico),
        Dificultad.Medio, martes, EstadoReto.Publicado, codigo = "4729", aciertosPromedio = 91,
    )
    private val refrigerio = RetoDocente(
        "reto-refrigerio", "Después del refrigerio", setOf(CategoriaResiduo.Organico),
        Dificultad.Facil, martes.plusDays(1), EstadoReto.Programado,
    )
    private val mezcla = RetoDocente(
        "reto-mezcla", "Mezcla del día", CategoriaResiduo.entries.toSet(), Dificultad.Dificil, null, EstadoReto.Borrador,
    )

    private fun tablero(retoHoy: RetoDocente? = lonchera) = TableroDocente(martes, "Laura Restrepo", "Jardín B", retoHoy, 18, 22, 91)

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
        // La dificultad ya no habla de canecas: esas las elige la docente.
        compose.onNodeWithText("Medio · sin tiempo · 6 residuos").assertIsDisplayed()
        // El FAB extendido no expone su texto: TalkBack lee la etiqueta del ícono.
        compose.onNodeWithContentDescription("Crear reto").assertHasClickAction()
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
        onMostrarCodigo: (RetoDocente) -> Unit = {},
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
        compose.onNodeWithText("Paso 1 de 5 · elige al menos una caneca").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun laDificultadDiceResiduosYTiempoPeroNoCanecas() {
        compose.setContent { ReciclaKidsTheme { AsistenteDePrueba(AsistenteReto(martes, paso = 2)) } }

        compose.onNodeWithText("4 residuos, sin tiempo. Ideal para empezar.").assertIsDisplayed()
        compose.onNodeWithText("6 residuos, sin tiempo. El más usado.").assertIsDisplayed()
        compose.onNodeWithText("8 residuos, con tiempo.").performScrollTo().assertIsDisplayed()
        compose.onAllNodes(hasText("canecas", substring = true)).assertCountEquals(0)
    }

    @Test
    fun laVistaPreviaMuestraLasCanecasElegidasYNoLasDeLaDificultad() {
        val elegidas = setOf(CategoriaResiduo.NoAprovechable, CategoriaResiduo.Organico)
        compose.setContent {
            ReciclaKidsTheme { AsistenteDePrueba(AsistenteReto(martes, paso = 4, categorias = elegidas, dificultad = Dificultad.Facil)) }
        }

        compose.onNodeWithContentDescription(
            "Vista previa del juego con No aprovechables + Orgánicos y un residuo de ejemplo: cáscara de banano",
        ).assertIsDisplayed()
        // Sin aprovechables no puede salir la botella: no tendría caneca.
        compose.onAllNodes(hasContentDescription("botella", substring = true)).assertCountEquals(0)
        compose.onNodeWithText("Fácil · sin tiempo").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("4 residuos").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun conUnaSolaCanecaLaVistaPreviaMuestraLasTresComoElJuego() {
        compose.setContent {
            ReciclaKidsTheme { AsistenteDePrueba(AsistenteReto(martes, paso = 4, categorias = setOf(CategoriaResiduo.Organico))) }
        }

        compose.onNodeWithContentDescription(
            "Vista previa del juego con las tres canecas y un residuo de ejemplo: cáscara de banano",
        ).assertIsDisplayed()
        compose.onNodeWithText("Orgánicos").performScrollTo().assertIsDisplayed()
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
        var mostrado: RetoDocente? = null
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
    private fun BibliotecaDePrueba(retos: List<RetoDocente>, onEditar: (RetoDocente) -> Unit = {}) {
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
    fun alCambiarDeFiltroLaListaEmpiezaDesdeElPrimerReto() {
        val aprovechables = (1..8).map {
            RetoDocente("r$it", "Reto $it", setOf(CategoriaResiduo.Aprovechable), Dificultad.Facil, martes, EstadoReto.Publicado)
        }
        compose.setContent { ReciclaKidsTheme { BibliotecaDePrueba(listOf(refrigerio) + aprovechables) } }
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(aprovechables.size)

        compose.onNodeWithText("Aprovechables").performClick()

        compose.onNodeWithText("Reto 1").assertIsDisplayed()
    }

    @Test
    fun soloSeEditanProgramadosYBorradoresYUnBorradorNoTieneResultados() {
        var editado: RetoDocente? = null
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
        var agregar = 0
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
                    onReintentar = {}, onNino = { abierto = it }, onAgregarNino = { agregar++ },
                )
            }
        }

        compose.onNodeWithText("Mi grupo · Jardín B").assertIsDisplayed()
        compose.onNodeWithText("Completó en 3 min · 8 de 8 aciertos").assertIsDisplayed()
        compose.onNodeWithText("Va en el residuo 4").assertIsDisplayed()
        compose.onNodeWithText("Sin empezar").assertIsDisplayed()
        compose.onNodeWithText("Ana L.").performClick()
        // El FAB extendido no expone su texto: TalkBack lee la etiqueta del ícono.
        compose.onNodeWithContentDescription("Agregar niño").performClick()

        assertEquals("n03", abierto)
        assertEquals(1, agregar)
    }

    @Test
    fun elGrupoVacioInvitaAAgregarAlPrimerNino() {
        var agregar = 0
        compose.setContent {
            ReciclaKidsTheme {
                MiGrupoScreen(
                    EstadoUi.Vacio(GrupoHoy("Jardín B", null, emptyList())),
                    onReintentar = {}, onNino = {}, onAgregarNino = { agregar++ },
                )
            }
        }

        compose.onNodeWithText("Aún no hay niños en el grupo").assertIsDisplayed()
        compose.onNodeWithText("Agregar el primer niño").performScrollTo().performClick()

        assertEquals(1, agregar)
    }

    // --- Agregar un niño (desde UI-25) ---

    @Test
    fun agregarUnNinoSoloPideSuNombreYUnaLetraDelApellido() {
        var guardados = 0
        val formulario = FormularioNino()
        compose.setContent {
            ReciclaKidsTheme {
                AgregarNinoScreen(
                    formulario, EstadoAgregarNino.Editando,
                    onGuardar = { guardados++ }, onCompartir = {}, onOtro = {}, onVolver = {},
                )
            }
        }
        val agregar = compose.onNodeWithText("Agregar al grupo")

        agregar.assertIsNotEnabled()
        campo("Nombre").performTextInput("Pedro")
        // Aunque se escriba el apellido completo, solo queda la inicial.
        campo("Inicial del apellido").performTextInput("quintero")

        assertEquals("Q", formulario.inicial)
        compose.onNodeWithText("Así aparecerá en la app: Pedro Q.").assertIsDisplayed()
        agregar.performScrollTo().assertIsEnabled().performClick()
        assertEquals(1, guardados)
    }

    @Test
    fun alAgregarloAparecenSuCodigoYLaOpcionDeCompartirlo() {
        val pedro = NinoAgregado("nino-11", "Pedro Q.", "JB-7KQ4")
        var compartido: NinoAgregado? = null
        var otros = 0
        compose.setContent {
            ReciclaKidsTheme {
                AgregarNinoScreen(
                    FormularioNino(), EstadoAgregarNino.Agregado(pedro),
                    onGuardar = {}, onCompartir = { compartido = it }, onOtro = { otros++ }, onVolver = {},
                )
            }
        }

        compose.onNodeWithText("Pedro Q. ya está en tu grupo").assertIsDisplayed()
        compose.onNodeWithContentDescription("Código de vinculación: J B - 7 K Q 4").assertIsDisplayed()
        compose.onNodeWithText("Compartir código").performScrollTo().performClick()
        compose.onNodeWithText("Agregar otro niño").performScrollTo().performClick()

        assertEquals(pedro, compartido)
        assertEquals(1, otros)
    }

    @Test
    fun agregarUnNinoSinConexionAvisaYDejaReintentar() {
        val formulario = FormularioNino().apply {
            nombre = "Pedro"
            escribirInicial("Q")
        }
        compose.setContent {
            ReciclaKidsTheme {
                AgregarNinoScreen(formulario, EstadoAgregarNino.SinConexion, onGuardar = {}, onCompartir = {}, onOtro = {}, onVolver = {})
            }
        }

        compose.onNodeWithText("No pudimos conectarnos. Revisa tu internet e intenta de nuevo.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Agregar al grupo").performScrollTo().assertIsEnabled()
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
                                CategoriaResiduo.Aprovechable to 94,
                                CategoriaResiduo.NoAprovechable to 61,
                                CategoriaResiduo.Organico to 88,
                            ),
                            confusion = Confusion("el empaque metalizado", CategoriaResiduo.NoAprovechable, CategoriaResiduo.Aprovechable),
                            insignias = listOf(InsigniaNino("Amiga tortuga", true), InsigniaNino("Pulpo ordenado", false)),
                            codigoVinculacion = "JB-4QX7",
                            acudienteVinculado = true,
                        )
                    ),
                    onVolver = {},
                    onReintentar = {},
                    onCompartirCodigo = {},
                    onRegenerarCodigo = {},
                    onQuitar = {},
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
        compose.onNodeWithText("Acudiente vinculado").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun lasInsigniasDelDetalleLlenanLaTarjetaEnColumnasIguales() {
        val insignias = listOf("Amiga tortuga", "Rápido como pez", "Racha de 5", "Pulpo ordenado", "Guardián del mar")
            .mapIndexed { i, nombre -> InsigniaNino(nombre, ganada = i < 3) }
        compose.setContent {
            ReciclaKidsTheme {
                DetalleNinoScreen(
                    EstadoUi.Contenido(DetalleNino("n02", "Juan T.", listOf(62, 74), emptyMap(), null, insignias, "JB-4QX7", true)),
                    onVolver = {},
                    onReintentar = {},
                    onCompartirCodigo = {},
                    onRegenerarCodigo = {},
                    onQuitar = {},
                )
            }
        }

        val ancho = compose.onRoot().getUnclippedBoundsInRoot().width
        val cajas = insignias.map { compose.onNode(hasContentDescription(it.nombre, substring = true)).getUnclippedBoundsInRoot() }
        val filas = cajas.groupBy { it.top }.values.toList()
        // A 360 dp caben dos por fila; la última queda sola pero no se estira.
        assertEquals(listOf(2, 2, 1), filas.map { it.size })
        cajas.forEach { assertEquals(cajas.first().width.value, it.width.value, 0.5f) }
        filas.forEach { fila -> fila.forEach { assertEquals(fila.first().height.value, it.height.value, 0.5f) } }
        // La fila completa llega al borde derecho de la tarjeta: el margen es igual a ambos lados.
        assertEquals(filas.first().first().left.value, (ancho - filas.first().last().right).value, 0.5f)
    }

    private val pedro = DetalleNino("nino-11", "Pedro Q.", emptyList(), emptyMap(), null, emptyList(), "JB-7KQ4", acudienteVinculado = false)

    private fun montarDetalle(
        onCompartir: (DetalleNino) -> Unit = {},
        onRegenerar: () -> Unit = {},
        onQuitar: () -> Unit = {},
    ) {
        compose.setContent {
            ReciclaKidsTheme {
                DetalleNinoScreen(
                    EstadoUi.Vacio(pedro),
                    onVolver = {},
                    onReintentar = {},
                    onCompartirCodigo = onCompartir,
                    onRegenerarCodigo = onRegenerar,
                    onQuitar = onQuitar,
                )
            }
        }
    }

    @Test
    fun unNinoRecienAgregadoMuestraSuCodigoAunqueNoHayaJugado() {
        var compartido: DetalleNino? = null
        var regenerados = 0
        montarDetalle(onCompartir = { compartido = it }, onRegenerar = { regenerados++ })

        compose.onNodeWithText("Todavía no hay resultados").assertIsDisplayed()
        compose.onNodeWithText("Sin vincular").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("JB-7KQ4").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Compartir").performScrollTo().performClick()
        assertEquals(pedro, compartido)

        // Regenerar invalida el código que ya se compartió: pide confirmación.
        compose.onNodeWithText("Regenerar").performScrollTo().performClick()
        compose.onNodeWithText("¿Generar un código nuevo para Pedro Q.?").assertIsDisplayed()
        compose.onNodeWithText("Cancelar").performClick()
        assertEquals(0, regenerados)
        compose.onNodeWithText("Regenerar").performScrollTo().performClick()
        compose.onNodeWithText("Generar código").performClick()
        assertEquals(1, regenerados)
    }

    @Test
    fun quitarUnNinoDelGrupoPideConfirmacion() {
        var quitados = 0
        montarDetalle(onQuitar = { quitados++ })

        compose.onNodeWithText("Quitar del grupo").performScrollTo().performClick()
        compose.onNodeWithText("¿Quitar a Pedro Q. del grupo?").assertIsDisplayed()
        compose.onNodeWithText("Cancelar").performClick()
        assertEquals(0, quitados)

        compose.onNodeWithText("Quitar del grupo").performScrollTo().performClick()
        compose.onNodeWithText("Sí, quitar").performClick()
        assertEquals(1, quitados)
    }

    // --- UI-28 Cuenta ---

    @Composable
    private fun CuentaDePrueba(onAvisos: (AvisosDocente) -> Unit = {}, onEliminar: () -> Unit = {}, onIrAGrupo: () -> Unit = {}) {
        CuentaDocenteScreen(
            estado = EstadoUi.Contenido(
                PerfilDocente("Laura Restrepo", "laura.r@jardin.edu.co", "Gotitas", "Jardín B", 22, AvisosDocente())
            ),
            onReintentar = {}, onAvisos = onAvisos, onIrAGrupo = onIrAGrupo,
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
    fun cadaAvisoSeCambiaTocandoSuFilaYLosCodigosEstanEnMiGrupo() {
        var avisos: AvisosDocente? = null
        var irAGrupo = 0
        compose.setContent { ReciclaKidsTheme { CuentaDePrueba(onAvisos = { avisos = it }, onIrAGrupo = { irAGrupo++ }) } }

        compose.onNodeWithText("Avisarme de niños sin jugar").performScrollTo().performClick()
        // Ya no hay un código único del grupo: cada niño tiene el suyo.
        compose.onAllNodesWithText("Compartir").assertCountEquals(0)
        compose.onNodeWithText("Lo encuentras en Mi grupo", substring = true).performScrollTo().performClick()

        assertEquals(AvisosDocente(ninosSinJugar = true), avisos)
        assertEquals(1, irAGrupo)
    }

    @Test
    fun losBotonesDeLosModosAdultosMidenAlMenos48Dp() {
        compose.setContent { ReciclaKidsTheme { CuentaDePrueba() } }

        listOf("Editar perfil", "Ver política", "Cerrar sesión").forEach {
            compose.onNodeWithText(it).performScrollTo().assertHeightIsAtLeast(Tactil.minimoAdulto)
        }
    }

    // --- Editar perfil (desde UI-28) ---

    private val laura = DatosPerfilDocente("Laura Restrepo", "laura.r@jardin.edu.co", "Gotitas", "Jardín B")

    private fun campo(etiqueta: String) = compose.onNode(hasSetTextAction() and hasText(etiqueta))

    private fun montarEdicion(
        formulario: FormularioPerfilDocente = FormularioPerfilDocente(laura),
        guardado: EstadoGuardado = EstadoGuardado.Editando,
        onGuardar: () -> Unit = {},
        onVolver: () -> Unit = {},
    ) {
        compose.setContent {
            ReciclaKidsTheme {
                EditarPerfilDocenteScreen(
                    EstadoUi.Contenido(formulario), guardado, onGuardar = onGuardar, onReintentar = {}, onVolver = onVolver,
                )
            }
        }
    }

    @Test
    fun guardarElPerfilSoloSeHabilitaConCambiosYSinErrores() {
        var guardados = 0
        montarEdicion(onGuardar = { guardados++ })
        val guardar = compose.onNodeWithText("Guardar cambios")

        guardar.performScrollTo().assertIsNotEnabled()
        campo("Grupo a cargo").performScrollTo().performTextReplacement("Jardín C")
        guardar.performScrollTo().assertIsEnabled()

        // Las mismas reglas del registro, a la vista mientras se escribe.
        campo("Jardín infantil").performScrollTo().performTextClearance()
        compose.onNodeWithText("Completa este campo").assertIsDisplayed()
        guardar.performScrollTo().assertIsNotEnabled()
        campo("Jardín infantil").performTextInput("Gotitas")
        campo("Correo institucional").performScrollTo().performTextReplacement("laura.r@jardin")
        compose.onNodeWithText("Escribe un correo válido").assertIsDisplayed()
        guardar.performScrollTo().assertIsNotEnabled()

        campo("Correo institucional").performTextReplacement("laura.r@jardin.edu.co")
        guardar.performScrollTo().performClick()
        assertEquals(1, guardados)
    }

    @Test
    fun elCorreoDeOtraCuentaSeMarcaHastaQueSeEscribeOtro() {
        val formulario = FormularioPerfilDocente(laura).apply { correo = "ana@jardin.edu.co" }
        montarEdicion(formulario, guardado = EstadoGuardado.CorreoEnUso("ana@jardin.edu.co"))

        compose.onNodeWithText("Ya existe una cuenta con ese correo").assertIsDisplayed()
        compose.onNodeWithText("Guardar cambios").performScrollTo().assertIsNotEnabled()

        campo("Correo institucional").performTextReplacement("laura.restrepo@jardin.edu.co")
        compose.onAllNodesWithText("Ya existe una cuenta con ese correo").assertCountEquals(0)
        compose.onNodeWithText("Guardar cambios").performScrollTo().assertIsEnabled()
    }

    @Test
    fun sinConexionAvisaYSePuedeVolverAGuardar() {
        var guardados = 0
        val formulario = FormularioPerfilDocente(laura).apply { nombre = "Laura Gómez" }
        montarEdicion(formulario, guardado = EstadoGuardado.SinConexion, onGuardar = { guardados++ })

        compose.onNodeWithText("No pudimos conectarnos. Revisa tu internet e intenta de nuevo.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Guardar cambios").performScrollTo().performClick()

        assertEquals(1, guardados)
    }

    @Test
    fun mientrasSeGuardaElFormularioNoSeToca() {
        val formulario = FormularioPerfilDocente(laura).apply { nombre = "Laura Gómez" }
        montarEdicion(formulario, guardado = EstadoGuardado.Guardando)

        // Deshabilitados ya no aceptan texto, así que se buscan solo por su etiqueta.
        compose.onNodeWithText("Nombre y apellido").assertIsNotEnabled()
        compose.onNodeWithText("Grupo a cargo").assertIsNotEnabled()
        // El botón muestra el progreso en lugar del texto.
        compose.onAllNodesWithText("Guardar cambios").assertCountEquals(0)
    }

    @Test
    fun salirConCambiosSinGuardarPideConfirmacion() {
        var salidas = 0
        montarEdicion(onVolver = { salidas++ })

        // Sin cambios sale directo.
        compose.onNodeWithContentDescription("Volver").performClick()
        assertEquals(1, salidas)

        campo("Nombre y apellido").performTextReplacement("Laura Gómez")
        compose.onNodeWithContentDescription("Volver").performClick()
        compose.onNodeWithText("¿Descartar los cambios?").assertIsDisplayed()
        compose.onNodeWithText("Seguir editando").performClick()
        assertEquals(1, salidas)
        campo("Nombre y apellido").assert(hasText("Laura Gómez"))

        compose.onNodeWithContentDescription("Volver").performClick()
        compose.onNodeWithText("Descartar").performClick()
        assertEquals(2, salidas)
    }

    @Test
    fun editarPerfilCargaYSiNoHayRedOfreceReintentar() {
        var reintentos = 0
        var estado by mutableStateOf<EstadoUi<FormularioPerfilDocente>>(EstadoUi.Cargando)
        compose.setContent {
            ReciclaKidsTheme {
                EditarPerfilDocenteScreen(estado, EstadoGuardado.Editando, onGuardar = {}, onReintentar = { reintentos++ }, onVolver = {})
            }
        }

        compose.onNodeWithContentDescription("Cargando…").assertIsDisplayed()
        estado = EstadoUi.Error
        compose.onNodeWithText("Reintentar").performScrollTo().performClick()
        assertEquals(1, reintentos)
    }
}
