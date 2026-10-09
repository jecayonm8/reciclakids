package com.reciclakids.ui.docente

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.reciclakids.R
import com.reciclakids.model.BibliotecaDocente
import com.reciclakids.model.DetalleNino
import com.reciclakids.model.GrupoHoy
import com.reciclakids.model.PerfilDocente
import com.reciclakids.model.ReporteSemanal
import com.reciclakids.model.Reto
import com.reciclakids.model.TableroDocente
import com.reciclakids.network.ServicioDocente
import com.reciclakids.ui.common.PantallaPendiente
import com.reciclakids.ui.theme.Duracion
import com.reciclakids.viewmodel.CargaViewModel
import com.reciclakids.viewmodel.CrearRetoViewModel
import com.reciclakids.viewmodel.FiltroReto
import com.reciclakids.viewmodel.OrigenAsistente
import com.reciclakids.viewmodel.datosCargados
import kotlinx.coroutines.launch

/** Rutas del proceso P3 · Docente (UI-21 a UI-28). */
object RutasDocente {
    const val Inicio = "docente/inicio"
    const val Biblioteca = "docente/biblioteca"
    const val Grupo = "docente/grupo"
    const val Reportes = "docente/reportes"
    const val Cuenta = "docente/cuenta"

    const val ArgOrigen = "origen"
    const val ArgReto = "reto"
    const val ArgNino = "nino"
    const val Crear = "docente/crear?$ArgOrigen={$ArgOrigen}&$ArgReto={$ArgReto}"
    const val Codigo = "docente/codigo/{$ArgReto}"
    const val Nino = "docente/nino/{$ArgNino}"
    const val EditarPerfil = "docente/editar-perfil"

    fun crear(origen: OrigenAsistente = OrigenAsistente.Nuevo, retoId: String? = null): String =
        "docente/crear?$ArgOrigen=${origen.name}" + (retoId?.let { "&$ArgReto=${Uri.encode(it)}" } ?: "")

    fun codigo(retoId: String): String = "docente/codigo/${Uri.encode(retoId)}"

    fun nino(ninoId: String): String = "docente/nino/${Uri.encode(ninoId)}"
}

/** Los cinco destinos de la NavigationBar. Asistente, código y detalle del niño van a pantalla completa. */
enum class PestanaDocente(val ruta: String, @param:StringRes val etiqueta: Int, @param:DrawableRes val icono: Int) {
    Inicio(RutasDocente.Inicio, R.string.docente_tab_inicio, R.drawable.ic_inicio),
    Biblioteca(RutasDocente.Biblioteca, R.string.docente_tab_biblioteca, R.drawable.ic_biblioteca),
    Grupo(RutasDocente.Grupo, R.string.docente_tab_grupo, R.drawable.ic_grupo),
    Reportes(RutasDocente.Reportes, R.string.docente_tab_reportes, R.drawable.ic_reportes),
    Cuenta(RutasDocente.Cuenta, R.string.docente_tab_cuenta, R.drawable.ic_cuenta),
}

/**
 * Modo Docente: su propio NavHost con la NavigationBar inferior de cinco destinos.
 *
 * UI-21 → UI-22 → UI-24 ; UI-23 → UI-22 (duplicar / editar) ; UI-25 → UI-27 ; UI-26 ; UI-28
 */
@Composable
fun ModoDocente(
    servicio: ServicioDocente,
    onCerrarSesion: () -> Unit,
    onVerPolitica: () -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val entrada by navController.currentBackStackEntryAsState()
    val destino = entrada?.destination
    val conBarra = destino == null || PestanaDocente.entries.any { it.ruta == destino.route }

    Column(modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = RutasDocente.Inicio,
            modifier = Modifier.weight(1f),
            enterTransition = { fadeIn(tween(Duracion.estandar)) },
            exitTransition = { fadeOut(tween(Duracion.estandar)) },
        ) {
            pestanas(navController, servicio, onCerrarSesion, onVerPolitica)
            pantallasCompletas(navController, servicio)
        }
        if (conBarra) BarraNavegacionDocente(destino, onPestana = { navController.irAPestana(it) })
    }
}

private fun NavGraphBuilder.pestanas(
    navController: NavHostController,
    servicio: ServicioDocente,
    onCerrarSesion: () -> Unit,
    onVerPolitica: () -> Unit,
) {
    composable(RutasDocente.Inicio) {
        val vm = viewModel { CargaViewModel<TableroDocente>(fuente = { servicio.tablero() }, esVacio = { it.retoHoy == null }) }
        LaunchedEffect(vm) { vm.cargar() }
        TableroDocenteScreen(
            estado = vm.estado,
            onReintentar = { vm.cargar() },
            onCrear = { navController.navigate(RutasDocente.crear()) },
            onReutilizar = { navController.navigate(RutasDocente.crear(OrigenAsistente.Reutilizar)) },
            onMostrarCodigo = { navController.navigate(RutasDocente.codigo(it)) },
            onVerGrupo = { navController.irAPestana(PestanaDocente.Grupo) },
        )
    }

    composable(RutasDocente.Biblioteca) {
        val vm = viewModel { CargaViewModel<BibliotecaDocente>(fuente = { servicio.biblioteca() }, esVacio = { it.retos.isEmpty() }) }
        LaunchedEffect(vm) { vm.cargar() }
        var filtro by rememberSaveable { mutableStateOf(FiltroReto.Todos) }
        BibliotecaRetosScreen(
            estado = vm.estado,
            filtro = filtro,
            onFiltro = { filtro = it },
            onReintentar = { vm.cargar() },
            onCrear = { navController.navigate(RutasDocente.crear()) },
            onDuplicar = { navController.navigate(RutasDocente.crear(OrigenAsistente.Duplicar, it.id)) },
            onVerResultados = { navController.irAPestana(PestanaDocente.Reportes) },
            onEditar = { navController.navigate(RutasDocente.crear(OrigenAsistente.Editar, it.id)) },
        )
    }

    composable(RutasDocente.Grupo) {
        val vm = viewModel { CargaViewModel<GrupoHoy>(fuente = { servicio.grupoHoy() }, esVacio = { it.ninos.isEmpty() }) }
        LaunchedEffect(vm) { vm.cargar() }
        MiGrupoScreen(
            estado = vm.estado,
            onReintentar = { vm.cargar() },
            onNino = { navController.navigate(RutasDocente.nino(it)) },
            onIrACuenta = { navController.irAPestana(PestanaDocente.Cuenta) },
        )
    }

    composable(RutasDocente.Reportes) {
        val vm = viewModel { CargaViewModel<ReporteSemanal>(fuente = { servicio.reporteSemanal() }, esVacio = { it.sinDatos }) }
        LaunchedEffect(vm) { vm.cargar() }
        val snackbar = remember { SnackbarHostState() }
        val alcance = rememberCoroutineScope()
        val textoEnviado = stringResource(R.string.reporte_enviado)
        val textoSinConexion = stringResource(R.string.docente_error_accion)
        var enviando by remember { mutableStateOf(false) }
        ReporteGrupalScreen(
            estado = vm.estado,
            onEnviar = {
                enviando = true
                alcance.launch {
                    val enviado = servicio.enviarReporteAcudientes()
                    enviando = false
                    snackbar.showSnackbar(if (enviado) textoEnviado else textoSinConexion)
                }
            },
            onReintentar = { vm.cargar() },
            onCrear = { navController.navigate(RutasDocente.crear()) },
            enviando = enviando,
            snackbar = snackbar,
        )
    }

    composable(RutasDocente.Cuenta) {
        val vm = viewModel { CargaViewModel<PerfilDocente>(fuente = { servicio.perfil() }) }
        LaunchedEffect(vm) { vm.cargar() }
        val snackbar = remember { SnackbarHostState() }
        val alcance = rememberCoroutineScope()
        val contexto = LocalContext.current
        val textoSinConexion = stringResource(R.string.docente_error_accion)
        val textoErrorAvisos = stringResource(R.string.cuenta_avisos_error)
        val textoEliminado = stringResource(R.string.cuenta_eliminado)
        // Plantillas con %1$s: los valores se conocen solo cuando responde el servicio.
        val plantillaRegenerado = stringResource(R.string.cuenta_regenerado)
        val plantillaCompartir = stringResource(R.string.cuenta_compartir_texto)
        CuentaDocenteScreen(
            estado = vm.estado,
            onReintentar = { vm.cargar() },
            onAvisos = { nuevos ->
                vm.estado.datosCargados?.let { anterior ->
                    // Cambio optimista: si no se guarda, el interruptor vuelve a su lugar.
                    vm.mostrar(anterior.copy(avisos = nuevos))
                    alcance.launch {
                        if (!servicio.guardarAvisos(nuevos)) {
                            vm.mostrar(anterior)
                            snackbar.showSnackbar(textoErrorAvisos)
                        }
                    }
                }
            },
            onCompartirCodigo = { codigo ->
                val grupo = vm.estado.datosCargados?.grupo.orEmpty()
                compartirTexto(contexto, plantillaCompartir.format(grupo, codigo))
            },
            onRegenerarCodigo = {
                alcance.launch {
                    val nuevo = servicio.regenerarCodigoVinculacion()
                    if (nuevo != null) {
                        vm.estado.datosCargados?.let { vm.mostrar(it.copy(codigoVinculacion = nuevo)) }
                        snackbar.showSnackbar(plantillaRegenerado.format(nuevo))
                    } else {
                        snackbar.showSnackbar(textoSinConexion)
                    }
                }
            },
            onEditarPerfil = { navController.navigate(RutasDocente.EditarPerfil) },
            onVerPolitica = onVerPolitica,
            onCerrarSesion = onCerrarSesion,
            onEliminarDatos = {
                alcance.launch {
                    val eliminado = servicio.eliminarDatosGrupo()
                    snackbar.showSnackbar(if (eliminado) textoEliminado else textoSinConexion)
                }
            },
            snackbar = snackbar,
        )
    }
}

private fun NavGraphBuilder.pantallasCompletas(navController: NavHostController, servicio: ServicioDocente) {
    composable(
        RutasDocente.Crear,
        arguments = listOf(
            navArgument(RutasDocente.ArgOrigen) {
                type = NavType.StringType
                defaultValue = OrigenAsistente.Nuevo.name
            },
            navArgument(RutasDocente.ArgReto) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
        ),
    ) { entrada ->
        val nombreOrigen = entrada.arguments?.getString(RutasDocente.ArgOrigen)
        val origen = OrigenAsistente.entries.firstOrNull { it.name == nombreOrigen } ?: OrigenAsistente.Nuevo
        val idReto = entrada.arguments?.getString(RutasDocente.ArgReto)
        val vm = viewModel { CrearRetoViewModel(servicio, origen, idReto) }
        CrearRetoWizard(
            asistente = vm.asistente,
            publicacion = vm.publicacion,
            hojaReutilizar = vm.hojaReutilizar,
            previos = vm.previos,
            onCambio = vm::actualizar,
            onAbrirReutilizar = vm::abrirReutilizar,
            onCerrarReutilizar = vm::cerrarReutilizar,
            onReintentarPrevios = vm::cargarPrevios,
            onUsar = vm::usar,
            onPublicar = vm::publicar,
            onCerrar = { navController.popBackStack() },
            // Se sale del asistente sin guardar su estado: si no, volvería a abrirse al restaurar una pestaña.
            onIrTablero = { navController.popBackStack(RutasDocente.Inicio, inclusive = false) },
            onMostrarCodigo = { reto -> navController.navigate(RutasDocente.codigo(reto.id)) { popUpTo(RutasDocente.Inicio) } },
            onVerBiblioteca = {
                navController.popBackStack()
                navController.irAPestana(PestanaDocente.Biblioteca)
            },
            editando = vm.editando,
            origen = vm.origenPendiente,
            onReintentarOrigen = vm::cargarOrigen,
            onCerrarResultado = vm::cerrarResultado,
        )
    }

    composable(RutasDocente.Codigo, arguments = listOf(navArgument(RutasDocente.ArgReto) { type = NavType.StringType })) { entrada ->
        val id = entrada.arguments?.getString(RutasDocente.ArgReto).orEmpty()
        val hoy = remember { servicio.hoy() }
        val vm = viewModel { CargaViewModel<Reto?>(fuente = { servicio.reto(id) }, esVacio = { it?.codigoActivo(hoy) == null }) }
        LaunchedEffect(vm) { vm.cargar() }
        val entraron by remember(id) { servicio.ninosQueEntraron(id) }.collectAsState(initial = null)
        CodigoPublicadoScreen(
            estado = vm.estado,
            hoy = hoy,
            ninosQueEntraron = entraron,
            onVolver = { navController.popBackStack() },
            onReintentar = { vm.cargar() },
        )
    }

    composable(RutasDocente.Nino, arguments = listOf(navArgument(RutasDocente.ArgNino) { type = NavType.StringType })) { entrada ->
        val id = entrada.arguments?.getString(RutasDocente.ArgNino).orEmpty()
        val vm = viewModel {
            CargaViewModel<DetalleNino?>(fuente = { servicio.detalleNino(id) }, esVacio = { it == null || it.sinResultados })
        }
        LaunchedEffect(vm) { vm.cargar() }
        DetalleNinoScreen(
            estado = vm.estado,
            onVolver = { navController.popBackStack() },
            onReintentar = { vm.cargar() },
        )
    }

    composable(RutasDocente.EditarPerfil) {
        PantallaPendiente(
            descripcion = stringResource(R.string.pendiente_editar_perfil),
            onVolver = { navController.popBackStack() },
            etiquetaVolver = stringResource(R.string.comun_atras),
        )
    }
}

@Composable
private fun BarraNavegacionDocente(destino: NavDestination?, onPestana: (PestanaDocente) -> Unit) {
    val esquema = MaterialTheme.colorScheme
    NavigationBar(containerColor = esquema.surfaceContainerLow) {
        PestanaDocente.entries.forEach { pestana ->
            NavigationBarItem(
                selected = destino?.hierarchy?.any { it.route == pestana.ruta } == true,
                onClick = { onPestana(pestana) },
                icon = { Icon(painterResource(pestana.icono), contentDescription = null) },
                label = { Text(stringResource(pestana.etiqueta)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = esquema.onPrimaryContainer,
                    selectedTextColor = esquema.onPrimaryContainer,
                    indicatorColor = esquema.primaryContainer,
                    unselectedIconColor = esquema.onSurfaceVariant,
                    unselectedTextColor = esquema.onSurfaceVariant,
                ),
            )
        }
    }
}

/** Cambio de pestaña estándar: cada pestaña conserva su estado y no se apilan. */
private fun NavController.irAPestana(pestana: PestanaDocente) {
    navigate(pestana.ruta) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun compartirTexto(contexto: Context, texto: String) {
    val envio = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, texto)
    }
    contexto.startActivity(Intent.createChooser(envio, null))
}
