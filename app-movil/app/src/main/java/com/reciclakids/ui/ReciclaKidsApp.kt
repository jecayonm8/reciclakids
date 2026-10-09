package com.reciclakids.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.reciclakids.R
import com.reciclakids.di.ContenedorApp
import com.reciclakids.model.RolAdulto
import com.reciclakids.ui.acceso.RutasAcceso
import com.reciclakids.ui.acceso.accesoGraph
import com.reciclakids.ui.common.ModoInmersivo
import com.reciclakids.ui.common.PantallaPendiente
import com.reciclakids.ui.nino.RutasNino
import com.reciclakids.ui.nino.VigilanteTiempoJuego
import com.reciclakids.ui.nino.esDelNino
import com.reciclakids.ui.nino.irATiempoTerminado
import com.reciclakids.ui.nino.ninoGraph
import com.reciclakids.ui.theme.Duracion
import com.reciclakids.viewmodel.SesionNinoViewModel

/** Destinos de los modos adultos. Cada uno tendrá su propio grafo; por ahora son provisionales. */
object Rutas {
    const val Docente = "docente"
    const val Padres = "padres"
}

/**
 * Raíz de la app: un solo NavHost con el grafo común de acceso y un destino por modo.
 * El rol de la cuenta decide si la sesión abre Docente o Padres.
 */
@Composable
fun ReciclaKidsApp(
    contenedor: ContenedorApp,
    navController: NavHostController = rememberNavController(),
) {
    val entradaActual by navController.currentBackStackEntryAsState()
    val ruta = entradaActual?.destination?.route
    ModoInmersivo(activo = ruta == null || ruta in RutasAcceso.DelNino || ruta.esDelNino())

    NavHost(
        navController = navController,
        startDestination = RutasAcceso.Grafo,
        enterTransition = { fadeIn(tween(Duracion.estandar)) },
        exitTransition = { fadeOut(tween(Duracion.estandar)) },
    ) {
        accesoGraph(
            navController = navController,
            servicio = contenedor.servicioAcceso,
            onJugar = { navController.navigate(RutasNino.Grafo) { launchSingleTop = true } },
            onSesionIniciada = { cuenta ->
                val destino = when (cuenta.rol) {
                    RolAdulto.Docente -> Rutas.Docente
                    RolAdulto.Acudiente -> Rutas.Padres
                }
                // Al volver desde un modo adulto se llega al selector, no al inicio de sesión.
                navController.navigate(destino) { popUpTo(RutasAcceso.Selector) }
            },
        )
        val volverAlSelector = { navController.popBackStack(RutasAcceso.Selector, inclusive = false) }
        ninoGraph(navController, contenedor, onSalir = { volverAlSelector() })
        composable(Rutas.Docente) {
            PantallaPendiente(stringResource(R.string.pendiente_docente), onVolver = { volverAlSelector() })
        }
        composable(Rutas.Padres) {
            PantallaPendiente(stringResource(R.string.pendiente_padres), onVolver = { volverAlSelector() })
        }
    }

    // UI-19 intercepta cualquier ruta del niño cuando se acaba el tiempo del control parental.
    if (ruta != null && ruta.esDelNino() && ruta !in RutasNino.SinReloj) {
        val grafo = remember(entradaActual) { runCatching { navController.getBackStackEntry(RutasNino.Grafo) }.getOrNull() }
        if (grafo != null) {
            val sesion: SesionNinoViewModel = viewModel(grafo)
            sesion.nino?.let { nino ->
                VigilanteTiempoJuego(
                    nino = nino,
                    tiempo = contenedor.tiempo,
                    control = contenedor.controlParental,
                    onAgotado = { navController.irATiempoTerminado() },
                )
            }
        }
    }
}
