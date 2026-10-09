package com.reciclakids.ui.padres

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.reciclakids.R
import com.reciclakids.model.HijoVinculado
import com.reciclakids.model.HistorialCorreos
import com.reciclakids.model.InicioPadres
import com.reciclakids.model.LogrosHijo
import com.reciclakids.model.PerfilAcudiente
import com.reciclakids.model.PrivacidadHijo
import com.reciclakids.network.ServicioPadres
import com.reciclakids.ui.common.PantallaPendiente
import com.reciclakids.ui.docente.BarraDocente
import com.reciclakids.ui.docente.EsqueletoCarga
import com.reciclakids.ui.docente.EstadoErrorRed
import com.reciclakids.ui.docente.MarcoDocente
import com.reciclakids.ui.docente.compartirTexto
import com.reciclakids.ui.theme.Duracion
import com.reciclakids.util.fechaLarga
import com.reciclakids.viewmodel.AvisoControl
import com.reciclakids.viewmodel.CargaViewModel
import com.reciclakids.viewmodel.ControlParentalViewModel
import com.reciclakids.viewmodel.EstadoUi
import com.reciclakids.viewmodel.EstadoVinculacion
import com.reciclakids.viewmodel.ReporteHijoViewModel
import com.reciclakids.viewmodel.VincularHijoViewModel
import com.reciclakids.viewmodel.datosCargados
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Rutas del proceso P4 · Padres (UI-29 a UI-35). */
object RutasPadres {
    const val Inicio = "padres/inicio"
    const val Reporte = "padres/reporte"
    const val Avisos = "padres/avisos"
    const val Cuenta = "padres/cuenta"
    const val Logros = "padres/logros"
    const val Control = "padres/control"
    const val Privacidad = "padres/privacidad"
    const val Vincular = "padres/vincular"
    const val EditarPerfil = "padres/editar-perfil"
}

/** Lo que dura «Cambios guardados · Deshacer» en UI-33, según las notas de la Fase 5. */
const val DuracionAvisoGuardadoMs = 2_600L

/**
 * Los cuatro destinos de la NavigationBar. Logros cuelga del reporte; control parental,
 * privacidad y vinculación cuelgan de la cuenta, y la barra marca la pestaña de la que cuelgan.
 */
enum class PestanaPadres(
    val ruta: String,
    @param:StringRes val etiqueta: Int,
    @param:DrawableRes val icono: Int,
    val subrutas: Set<String> = emptySet(),
) {
    Inicio(RutasPadres.Inicio, R.string.padres_tab_inicio, R.drawable.ic_inicio),
    Reporte(RutasPadres.Reporte, R.string.padres_tab_reporte, R.drawable.ic_reportes, setOf(RutasPadres.Logros)),
    Avisos(RutasPadres.Avisos, R.string.padres_tab_avisos, R.drawable.ic_avisos),
    Cuenta(
        RutasPadres.Cuenta,
        R.string.padres_tab_cuenta,
        R.drawable.ic_cuenta,
        setOf(RutasPadres.Control, RutasPadres.Privacidad, RutasPadres.Vincular, RutasPadres.EditarPerfil),
    ),
}

/**
 * Modo Padres: su propio NavHost con la NavigationBar inferior de cuatro destinos. Todas las
 * pantallas son del hijo o hija elegido en el inicio; nunca se mezclan datos de dos niños.
 *
 * UI-29 → UI-30, UI-31, UI-32 ; UI-33 ; UI-34 ; UI-35
 */
@Composable
fun ModoPadres(
    servicio: ServicioPadres,
    onCerrarSesion: () -> Unit,
    onVerPolitica: () -> Unit,
    onCambiarContrasena: () -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val hijos = viewModel { CargaViewModel<List<HijoVinculado>>(fuente = { servicio.hijos() }) }
    LaunchedEffect(hijos) { hijos.cargar() }
    // Estados que el grafo lee al componer cada pantalla: el grafo se arma una sola vez, así que
    // no puede guardar el hijo elegido por valor.
    val elegido = rememberSaveable { mutableStateOf<String?>(null) }
    val avisoCuenta = remember { mutableStateOf<String?>(null) }
    val entrada by navController.currentBackStackEntryAsState()

    Column(modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            when (hijos.estado) {
                EstadoUi.Cargando -> MarcoDocente(barra = { BarraDocente(stringResource(R.string.padres_tab_inicio)) }) {
                    EsqueletoCarga(Modifier.padding(it))
                }
                EstadoUi.Error -> MarcoDocente(barra = { BarraDocente(stringResource(R.string.padres_tab_inicio)) }) {
                    EstadoErrorRed(
                        onReintentar = { hijos.cargar() },
                        modifier = Modifier.padding(it),
                        titulo = stringResource(R.string.padres_error_titulo),
                        texto = stringResource(R.string.padres_error_texto),
                    )
                }
                is EstadoUi.Vacio, is EstadoUi.Contenido -> NavHost(
                    navController = navController,
                    startDestination = RutasPadres.Inicio,
                    enterTransition = { fadeIn(tween(Duracion.estandar)) },
                    exitTransition = { fadeOut(tween(Duracion.estandar)) },
                ) {
                    pestanas(navController, servicio, hijos, elegido, avisoCuenta, onCerrarSesion, onCambiarContrasena)
                    subpantallas(navController, servicio, hijos, elegido, avisoCuenta, onVerPolitica)
                }
            }
        }
        if (hijos.estado.datosCargados != null) {
            BarraNavegacionPadres(entrada?.destination?.route, onPestana = { navController.irAPestana(it) })
        }
    }
}

/** El hijo o hija elegido; el primero mientras no se elija otro. */
private fun hijoElegido(hijos: CargaViewModel<List<HijoVinculado>>, elegido: MutableState<String?>): HijoVinculado? {
    val lista = hijos.estado.datosCargados.orEmpty()
    return lista.firstOrNull { it.id == elegido.value } ?: lista.firstOrNull()
}

private fun NavGraphBuilder.pestanas(
    navController: NavHostController,
    servicio: ServicioPadres,
    hijos: CargaViewModel<List<HijoVinculado>>,
    elegido: MutableState<String?>,
    avisoCuenta: MutableState<String?>,
    onCerrarSesion: () -> Unit,
    onCambiarContrasena: () -> Unit,
) {
    composable(RutasPadres.Inicio) {
        val hijo = hijoElegido(hijos, elegido) ?: return@composable
        val vm = viewModel(key = "inicio/${hijo.id}") {
            CargaViewModel<InicioPadres>(fuente = { servicio.inicio(hijo.id) }, esVacio = { it.sinDatos })
        }
        LaunchedEffect(vm) { vm.cargar() }
        InicioPadresScreen(
            hijo = hijo,
            hijos = hijos.estado.datosCargados.orEmpty(),
            estado = vm.estado,
            onElegirHijo = { elegido.value = it.id },
            onReintentar = { vm.cargar() },
            onVerReporte = { navController.irAPestana(PestanaPadres.Reporte) },
            onVerLogros = { navController.navigate(RutasPadres.Logros) },
            onControlParental = { navController.navigate(RutasPadres.Control) },
        )
    }

    composable(RutasPadres.Reporte) {
        val hijo = hijoElegido(hijos, elegido) ?: return@composable
        val vm = viewModel(key = "reporte/${hijo.id}") { ReporteHijoViewModel(servicio, hijo.id) }
        LaunchedEffect(vm) { vm.cargar() }
        ReporteSemanalScreen(
            estado = vm.estado,
            cambiandoSemana = vm.cambiandoSemana,
            onSemana = vm::elegirSemana,
            onReintentar = { vm.cargar() },
            onVolver = { navController.irAPestana(PestanaPadres.Inicio) },
            onVerLogros = { navController.navigate(RutasPadres.Logros) },
        )
    }

    composable(RutasPadres.Avisos) {
        val hijo = hijoElegido(hijos, elegido) ?: return@composable
        val vm = viewModel(key = "avisos/${hijo.id}") {
            CargaViewModel<HistorialCorreos>(fuente = { servicio.correos(hijo.id) }, esVacio = { it.correos.isEmpty() })
        }
        LaunchedEffect(vm) { vm.cargar() }
        NotificacionesScreen(
            estado = vm.estado,
            onReintentar = { vm.cargar() },
            onVolver = { navController.irAPestana(PestanaPadres.Inicio) },
            onPreferencias = { navController.navigate(RutasPadres.Control) },
        )
    }

    composable(RutasPadres.Cuenta) {
        val vm = viewModel { CargaViewModel<PerfilAcudiente>(fuente = { servicio.perfil() }) }
        LaunchedEffect(vm) { vm.cargar() }
        val snackbar = remember { SnackbarHostState() }
        // Lo deja la pantalla de vinculación al volver: «Listo: ya ves el progreso de Emilio R.».
        LaunchedEffect(avisoCuenta.value) {
            val aviso = avisoCuenta.value ?: return@LaunchedEffect
            avisoCuenta.value = null
            snackbar.showSnackbar(aviso)
        }
        CuentaPadresScreen(
            estado = vm.estado,
            onReintentar = { vm.cargar() },
            onEditarPerfil = { navController.navigate(RutasPadres.EditarPerfil) },
            onCambiarContrasena = onCambiarContrasena,
            onControlParental = { navController.navigate(RutasPadres.Control) },
            onPrivacidad = { navController.navigate(RutasPadres.Privacidad) },
            onVincular = { navController.navigate(RutasPadres.Vincular) },
            onCerrarSesion = onCerrarSesion,
            snackbar = snackbar,
        )
    }
}

private fun NavGraphBuilder.subpantallas(
    navController: NavHostController,
    servicio: ServicioPadres,
    hijos: CargaViewModel<List<HijoVinculado>>,
    elegido: MutableState<String?>,
    avisoCuenta: MutableState<String?>,
    onVerPolitica: () -> Unit,
) {
    composable(RutasPadres.Logros) {
        val hijo = hijoElegido(hijos, elegido) ?: return@composable
        val vm = viewModel(key = "logros/${hijo.id}") { CargaViewModel<LogrosHijo>(fuente = { servicio.logros(hijo.id) }) }
        LaunchedEffect(vm) { vm.cargar() }
        LogrosHijoScreen(
            estado = vm.estado,
            onReintentar = { vm.cargar() },
            onVolver = { navController.popBackStack() },
        )
    }

    composable(RutasPadres.Control) {
        val hijo = hijoElegido(hijos, elegido) ?: return@composable
        val vm = viewModel(key = "control/${hijo.id}") { ControlParentalViewModel(servicio, hijo.id) }
        LaunchedEffect(vm) { vm.cargar() }
        val snackbar = remember { SnackbarHostState() }
        val textoGuardado = stringResource(R.string.control_guardado)
        val textoDeshacer = stringResource(R.string.control_deshacer)
        val textoNoGuardado = stringResource(R.string.cuenta_avisos_error)
        // Cada aviso nuevo reemplaza al anterior; «Deshacer» restaura lo que había antes del cambio.
        LaunchedEffect(vm.numeroAviso) {
            val aviso = vm.aviso ?: return@LaunchedEffect
            vm.avisoAtendido()
            when (aviso) {
                is AvisoControl.Guardado -> {
                    val resultado = withTimeoutOrNull(DuracionAvisoGuardadoMs) {
                        snackbar.showSnackbar(textoGuardado, actionLabel = textoDeshacer, duration = SnackbarDuration.Indefinite)
                    }
                    if (resultado == SnackbarResult.ActionPerformed) vm.deshacer(aviso.anterior)
                }
                AvisoControl.NoGuardado -> snackbar.showSnackbar(textoNoGuardado)
            }
        }
        ControlParentalScreen(
            nombreHijo = hijo.nombre,
            estado = vm.estado,
            onCambio = vm::cambiar,
            onReintentar = { vm.cargar() },
            onVolver = { navController.popBackStack() },
            snackbar = snackbar,
        )
    }

    composable(RutasPadres.Privacidad) {
        val hijo = hijoElegido(hijos, elegido) ?: return@composable
        val vm = viewModel(key = "privacidad/${hijo.id}") { CargaViewModel<PrivacidadHijo>(fuente = { servicio.privacidad(hijo.id) }) }
        LaunchedEffect(vm) { vm.cargar() }
        val snackbar = remember { SnackbarHostState() }
        val alcance = rememberCoroutineScope()
        val contexto = LocalContext.current
        var enviando by remember { mutableStateOf(false) }
        val textoSinConexion = stringResource(R.string.docente_error_accion)
        val textoCancelada = stringResource(R.string.privacidad_solicitud_cancelada)
        val plantillaExportar = stringResource(R.string.privacidad_exportar_texto)
        val registros = vm.estado.datosCargados?.let { pluralStringResource(R.plurals.privacidad_registros, it.registros, it.registros) }.orEmpty()
        val insignias = vm.estado.datosCargados?.let { pluralStringResource(R.plurals.privacidad_insignias, it.insignias, it.insignias) }.orEmpty()
        PrivacidadDatosScreen(
            estado = vm.estado,
            enviando = enviando,
            onDescargar = { datos ->
                val texto = plantillaExportar.format(
                    datos.hijo.nombre,
                    datos.hijo.grupo,
                    datos.hijo.avatar,
                    registros,
                    insignias,
                    fechaLarga(datos.autorizadaEl),
                    datos.autorizadaPor,
                )
                compartirTexto(contexto, texto)
            },
            onVerPolitica = onVerPolitica,
            onSolicitarEliminacion = {
                enviando = true
                alcance.launch {
                    val fecha = servicio.solicitarEliminacion(hijo.id)
                    enviando = false
                    val actual = vm.estado.datosCargados
                    if (fecha != null && actual != null) {
                        vm.mostrar(actual.copy(solicitudEliminacion = fecha))
                    } else {
                        snackbar.showSnackbar(textoSinConexion)
                    }
                }
            },
            onCancelarSolicitud = {
                enviando = true
                alcance.launch {
                    val cancelada = servicio.cancelarSolicitudEliminacion(hijo.id)
                    enviando = false
                    val actual = vm.estado.datosCargados
                    if (cancelada && actual != null) {
                        vm.mostrar(actual.copy(solicitudEliminacion = null))
                        snackbar.showSnackbar(textoCancelada)
                    } else {
                        snackbar.showSnackbar(textoSinConexion)
                    }
                }
            },
            onReintentar = { vm.cargar() },
            onVolver = { navController.popBackStack() },
            snackbar = snackbar,
        )
    }

    composable(RutasPadres.Vincular) {
        val vm = viewModel { VincularHijoViewModel(servicio) }
        val plantillaVinculado = stringResource(R.string.cuenta_vinculado)
        val plantillaYaEstaba = stringResource(R.string.cuenta_ya_vinculado)
        LaunchedEffect(vm.estado) {
            val listo = vm.estado as? EstadoVinculacion.Listo ?: return@LaunchedEffect
            avisoCuenta.value = (if (listo.yaEstaba) plantillaYaEstaba else plantillaVinculado).format(listo.hijo.nombre)
            hijos.cargar()
            navController.popBackStack()
        }
        VincularHijoScreen(
            codigo = vm.codigo,
            estado = vm.estado,
            onCodigo = vm::escribir,
            onVincular = vm::vincular,
            onVolver = { navController.popBackStack() },
        )
    }

    composable(RutasPadres.EditarPerfil) {
        PantallaPendiente(
            descripcion = stringResource(R.string.pendiente_editar_perfil),
            onVolver = { navController.popBackStack() },
            etiquetaVolver = stringResource(R.string.comun_atras),
        )
    }
}

@Composable
private fun BarraNavegacionPadres(ruta: String?, onPestana: (PestanaPadres) -> Unit) {
    val esquema = MaterialTheme.colorScheme
    NavigationBar(containerColor = esquema.surfaceContainerLow) {
        PestanaPadres.entries.forEach { pestana ->
            NavigationBarItem(
                selected = ruta == pestana.ruta || ruta in pestana.subrutas || (ruta == null && pestana == PestanaPadres.Inicio),
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
private fun NavController.irAPestana(pestana: PestanaPadres) {
    navigate(pestana.ruta) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
