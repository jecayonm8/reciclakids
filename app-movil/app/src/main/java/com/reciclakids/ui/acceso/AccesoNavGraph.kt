package com.reciclakids.ui.acceso

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.reciclakids.R
import com.reciclakids.model.CuentaAdulto
import com.reciclakids.network.ResultadoRegistro
import com.reciclakids.network.ResultadoSesion
import com.reciclakids.network.ServicioAcceso
import com.reciclakids.ui.common.EstadoConexion
import com.reciclakids.ui.common.PantallaPendiente
import com.reciclakids.ui.common.rememberHayConexion
import com.reciclakids.viewmodel.RegistroViewModel
import kotlinx.coroutines.launch

/** Rutas del proceso P1 · Acceso común (UI-01 a UI-07). */
object RutasAcceso {
    const val Grafo = "acceso"
    const val Bienvenida = "acceso/bienvenida"
    const val Selector = "acceso/selector"
    const val Login = "acceso/login"
    const val Registro = "acceso/registro"
    const val RegistroDatos = "acceso/registro/datos"
    const val Autorizacion = "acceso/registro/autorizacion"
    const val Recuperar = "acceso/recuperar"
    const val Politica = "acceso/politica"

    /** Pantallas que ve el niño: van en modo inmersivo. */
    val DelNino = setOf(Bienvenida, Selector)
}

private const val ClaveCuentaCreada = "cuentaCreada"

/**
 * UI-01 → UI-02 ─┬─ «¡A jugar!» → [onJugar]
 *                └─ puerta para adultos → UI-03 → [onSesionIniciada] según el rol
 * UI-03 ↔ UI-04/05 → UI-07 ; UI-03 → UI-06
 */
fun NavGraphBuilder.accesoGraph(
    navController: NavController,
    servicio: ServicioAcceso,
    onJugar: () -> Unit,
    onSesionIniciada: (CuentaAdulto) -> Unit,
) {
    navigation(startDestination = RutasAcceso.Bienvenida, route = RutasAcceso.Grafo) {
        composable(RutasAcceso.Bienvenida) {
            SplashScreen(
                onContinuar = {
                    navController.navigate(RutasAcceso.Selector) {
                        popUpTo(RutasAcceso.Bienvenida) { inclusive = true }
                    }
                },
            )
        }

        composable(RutasAcceso.Selector) {
            val hayConexion by rememberHayConexion()
            SelectorModoScreen(
                onJugar = onJugar,
                onPuertaAbierta = { navController.navigate(RutasAcceso.Login) { launchSingleTop = true } },
                conexion = if (hayConexion) EstadoConexion.Conectado else EstadoConexion.SinConexion,
            )
        }

        composable(RutasAcceso.Login) { entrada ->
            val alcance = rememberCoroutineScope()
            var cargando by remember { mutableStateOf(false) }
            var error by remember { mutableStateOf<ErrorLogin?>(null) }
            val cuentaCreada by entrada.savedStateHandle.getStateFlow(ClaveCuentaCreada, false).collectAsState()
            LoginScreen(
                onEntrar = { correo, contrasena ->
                    cargando = true
                    error = null
                    entrada.savedStateHandle[ClaveCuentaCreada] = false
                    alcance.launch {
                        when (val resultado = servicio.iniciarSesion(correo, contrasena)) {
                            is ResultadoSesion.Iniciada -> onSesionIniciada(resultado.cuenta)
                            ResultadoSesion.CredencialesInvalidas -> error = ErrorLogin.CredencialesInvalidas
                            ResultadoSesion.SinConexion -> error = ErrorLogin.SinConexion
                        }
                        cargando = false
                    }
                },
                onOlvideContrasena = { navController.navigate(RutasAcceso.Recuperar) },
                onCrearCuenta = { navController.navigate(RutasAcceso.Registro) },
                onVolverInicio = { navController.popBackStack(RutasAcceso.Selector, inclusive = false) },
                cargando = cargando,
                error = error,
                cuentaRecienCreada = cuentaCreada,
            )
        }

        navigation(startDestination = RutasAcceso.RegistroDatos, route = RutasAcceso.Registro) {
            composable(RutasAcceso.RegistroDatos) { entrada ->
                val registro = registroViewModel(navController, entrada)
                val alcance = rememberCoroutineScope()
                var cargando by remember { mutableStateOf(false) }
                var error by remember { mutableStateOf<ErrorRegistro?>(null) }
                RegistroScreen(
                    rol = registro.rol,
                    onCambiarRol = {
                        registro.rol = it
                        error = null
                    },
                    docente = registro.docente,
                    acudiente = registro.acudiente,
                    onAtras = { navController.popBackStack() },
                    onCrearDocente = {
                        cargando = true
                        error = null
                        alcance.launch {
                            error = servicio.registrarDocente(registro.docente.aRegistro()).comoError()
                            cargando = false
                            if (error == null) navController.volverALoginConCuentaCreada()
                        }
                    },
                    onContinuarAcudiente = { navController.navigate(RutasAcceso.Autorizacion) },
                    onLeerPolitica = { navController.navigate(RutasAcceso.Politica) },
                    cargando = cargando,
                    error = error,
                )
            }

            composable(RutasAcceso.Autorizacion) { entrada ->
                val registro = registroViewModel(navController, entrada)
                val alcance = rememberCoroutineScope()
                var cargando by remember { mutableStateOf(false) }
                var error by remember { mutableStateOf<ErrorRegistro?>(null) }
                AutorizacionDatosMenorScreen(
                    onAtras = { navController.popBackStack() },
                    onCrearCuenta = {
                        cargando = true
                        error = null
                        alcance.launch {
                            error = servicio.registrarAcudiente(registro.acudiente.aRegistro()).comoError()
                            cargando = false
                            if (error == null) navController.volverALoginConCuentaCreada()
                        }
                    },
                    onLeerPolitica = { navController.navigate(RutasAcceso.Politica) },
                    cargando = cargando,
                    error = error,
                )
            }
        }

        composable(RutasAcceso.Recuperar) {
            val alcance = rememberCoroutineScope()
            var enviando by remember { mutableStateOf(false) }
            var enviado by remember { mutableStateOf(false) }
            var sinConexion by remember { mutableStateOf(false) }
            RecuperarContrasenaScreen(
                onAtras = { navController.popBackStack() },
                onEnviar = { correo ->
                    enviando = true
                    sinConexion = false
                    alcance.launch {
                        enviado = servicio.enviarEnlaceRecuperacion(correo)
                        sinConexion = !enviado
                        enviando = false
                    }
                },
                enviando = enviando,
                enviado = enviado,
                sinConexion = sinConexion,
            )
        }

        composable(RutasAcceso.Politica) {
            PantallaPendiente(
                descripcion = stringResource(R.string.pendiente_politica),
                onVolver = { navController.popBackStack() },
                etiquetaVolver = stringResource(R.string.comun_atras),
            )
        }
    }
}

@Composable
private fun registroViewModel(navController: NavController, entrada: NavBackStackEntry): RegistroViewModel {
    val grafo = remember(entrada) { navController.getBackStackEntry(RutasAcceso.Registro) }
    return viewModel(grafo)
}

private fun ResultadoRegistro.comoError(): ErrorRegistro? = when (this) {
    ResultadoRegistro.Creada -> null
    ResultadoRegistro.CorreoEnUso -> ErrorRegistro.CorreoEnUso
    ResultadoRegistro.SinConexion -> ErrorRegistro.SinConexion
}

private fun NavController.volverALoginConCuentaCreada() {
    getBackStackEntry(RutasAcceso.Login).savedStateHandle[ClaveCuentaCreada] = true
    popBackStack(RutasAcceso.Login, inclusive = false)
}
