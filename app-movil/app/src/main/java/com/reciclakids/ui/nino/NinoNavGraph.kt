package com.reciclakids.ui.nino

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
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
import com.reciclakids.model.Nino
import com.reciclakids.network.ServicioRetos
import com.reciclakids.ui.common.PantallaPendiente
import com.reciclakids.ui.common.rememberLocutor
import com.reciclakids.viewmodel.CodigoRetoViewModel
import com.reciclakids.viewmodel.SesionNinoViewModel
import kotlinx.coroutines.delay

/** Rutas del proceso P2 · Juego del niño. */
object RutasNino {
    const val Grafo = "nino"
    const val Codigo = "nino/codigo"
    const val QuienEres = "nino/quien-eres"
    const val Menu = "nino/menu"
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
    servicioRetos: ServicioRetos,
    onSalir: () -> Unit,
) {
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

        composable(RutasNino.Menu) {
            PantallaPendiente(stringResource(R.string.pendiente_menu), onVolver = onSalir)
        }
    }
}

@Composable
private fun sesionNino(navController: NavController, entrada: NavBackStackEntry): SesionNinoViewModel {
    val grafo = remember(entrada) { navController.getBackStackEntry(RutasNino.Grafo) }
    return viewModel(grafo)
}
