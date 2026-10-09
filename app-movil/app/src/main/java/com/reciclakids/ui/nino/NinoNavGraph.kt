package com.reciclakids.ui.nino

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.reciclakids.R
import com.reciclakids.di.ContenedorApp
import com.reciclakids.model.Nino
import com.reciclakids.ui.common.PantallaPendiente
import com.reciclakids.ui.common.rememberHayConexion
import com.reciclakids.ui.common.rememberLocutor
import com.reciclakids.viewmodel.CodigoRetoViewModel
import com.reciclakids.viewmodel.SesionNinoViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Rutas del proceso P2 · Juego del niño. */
object RutasNino {
    const val Grafo = "nino"
    const val Codigo = "nino/codigo"
    const val QuienEres = "nino/quien-eres"
    const val Menu = "nino/menu"
    const val Tutorial = "nino/tutorial"
    const val Juego = "nino/juego"
    const val Insignias = "nino/insignias"
    const val Ajustes = "nino/ajustes"
}

/** Todo lo que ve el niño va en modo inmersivo. */
fun String.esDelNino(): Boolean = this == RutasNino.Grafo || startsWith("${RutasNino.Grafo}/")

/** Espera tras el código correcto para que el niño vea el borde verde y escuche «¡Ese es!». */
private const val EsperaCodigoCorrectoMs = 900L

/** Espera tras tocar un avatar para que se vea el borde ámbar antes de avanzar. */
private const val EsperaAvatarMs = 350L

/**
 * UI-08 → UI-09 → UI-10. Una vez aceptado el código y elegido el avatar, volver atrás lleva al
 * selector de modo: el niño nunca llega a los modos adultos sin pasar por la puerta.
 */
fun NavGraphBuilder.ninoGraph(
    navController: NavController,
    contenedor: ContenedorApp,
    onSalir: () -> Unit,
) {
    val servicioRetos = contenedor.servicioRetos
    navigation(startDestination = RutasNino.Codigo, route = RutasNino.Grafo) {
        composable(RutasNino.Codigo) { entrada ->
            val sesion = sesionNino(navController, entrada)
            val codigo = viewModel { CodigoRetoViewModel(servicioRetos) }
            val locutor = rememberLocutor()
            val mensaje = stringResource(codigo.estado.mensaje())
            LaunchedEffect(mensaje) { locutor.decir(mensaje) }
            LaunchedEffect(codigo.reto) {
                val reto = codigo.reto ?: return@LaunchedEffect
                delay(EsperaCodigoCorrectoMs)
                sesion.abrirReto(reto)
                navController.navigate(RutasNino.QuienEres) {
                    popUpTo(RutasNino.Codigo) { inclusive = true }
                }
            }
            CodigoRetoScreen(
                codigo = codigo.codigo,
                estado = codigo.estado,
                onDigito = { digito ->
                    locutor.decir(digito.toString())
                    codigo.escribir(digito)
                },
                onBorrar = codigo::borrar,
                onConfirmar = codigo::confirmar,
                onRepetirVoz = { locutor.decir(mensaje) },
            )
        }

        composable(RutasNino.QuienEres) { entrada ->
            val sesion = sesionNino(navController, entrada)
            val reto = sesion.reto
            val locutor = rememberLocutor()
            val titulo = stringResource(R.string.quien_eres_titulo)
            var elegido by rememberSaveable { mutableStateOf<String?>(null) }
            val ninos by produceState<List<Nino>?>(initialValue = null, reto) {
                value = reto?.let { servicioRetos.ninosDelGrupo(it) }
            }
            LaunchedEffect(Unit) { locutor.decir(titulo) }
            LaunchedEffect(elegido, ninos) {
                val nino = ninos?.firstOrNull { it.id == elegido } ?: return@LaunchedEffect
                delay(EsperaAvatarMs)
                sesion.elegirNino(nino)
                navController.navigate(RutasNino.Menu) {
                    popUpTo(RutasNino.QuienEres) { inclusive = true }
                }
            }
            SeleccionAvatarScreen(
                ninos = ninos,
                elegido = elegido,
                onElegir = { nino ->
                    if (elegido == null) {
                        locutor.decir(nino.nombre)
                        elegido = nino.id
                    }
                },
                onRepetirVoz = { locutor.decir(titulo) },
            )
        }

        composable(RutasNino.Menu) { entrada ->
            val sesion = sesionNino(navController, entrada)
            val nino = sesion.nino
            val reto = sesion.reto
            if (nino == null || reto == null) {
                // La sesión vive en memoria: si el sistema cerró la app, el niño vuelve a entrar.
                LaunchedEffect(Unit) { onSalir() }
                return@composable
            }
            val progreso by contenedor.progreso.observar(nino.id).collectAsState(initial = null)
            val hayConexion by rememberHayConexion()
            val locutor = rememberLocutor()
            val saludo = stringResource(R.string.menu_saludo, nino.nombre)
            LaunchedEffect(Unit) { locutor.decir(saludo) }
            MenuAcuarioScreen(
                nombre = nino.nombre,
                nivel = progreso?.nivelAcuario ?: 1,
                retoPendiente = progreso?.completo(reto) == false,
                hayConexion = hayConexion,
                onRetoDiario = {
                    val siguiente = if (progreso?.tutorialVisto == true) RutasNino.Juego else RutasNino.Tutorial
                    navController.navigate(siguiente) { launchSingleTop = true }
                },
                onContinuar = { navController.navigate(RutasNino.Juego) { launchSingleTop = true } },
                onInsignias = { navController.navigate(RutasNino.Insignias) { launchSingleTop = true } },
                onAjustes = { navController.navigate(RutasNino.Ajustes) { launchSingleTop = true } },
            )
        }

        composable(RutasNino.Tutorial) { entrada ->
            val sesion = sesionNino(navController, entrada)
            val alcance = rememberCoroutineScope()
            val locutor = rememberLocutor()
            val instruccion = stringResource(R.string.tutorial_titulo)
            LaunchedEffect(Unit) { locutor.decir(instruccion) }
            TutorialArrastreScreen(
                onEntendido = {
                    alcance.launch {
                        sesion.nino?.let { contenedor.progreso.marcarTutorialVisto(it.id) }
                        navController.navigate(RutasNino.Juego) {
                            popUpTo(RutasNino.Tutorial) { inclusive = true }
                        }
                    }
                },
                onRepetirVoz = { locutor.decir(instruccion) },
            )
        }

        pendiente(navController, RutasNino.Juego, R.string.pendiente_juego)
        pendiente(navController, RutasNino.Insignias, R.string.pendiente_insignias)
        pendiente(navController, RutasNino.Ajustes, R.string.pendiente_ajustes)
    }
}

/** Destino provisional mientras llega su pantalla: vuelve al menú. */
private fun NavGraphBuilder.pendiente(navController: NavController, ruta: String, @StringRes descripcion: Int) {
    composable(ruta) {
        PantallaPendiente(
            descripcion = stringResource(descripcion),
            onVolver = { navController.popBackStack() },
            etiquetaVolver = stringResource(R.string.comun_atras),
        )
    }
}

@Composable
private fun sesionNino(navController: NavController, entrada: NavBackStackEntry): SesionNinoViewModel {
    val grafo = remember(entrada) { navController.getBackStackEntry(RutasNino.Grafo) }
    return viewModel(grafo)
}
