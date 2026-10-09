package com.reciclakids.ui.nino

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.reciclakids.R
import com.reciclakids.di.ContenedorApp
import com.reciclakids.model.AjustesNino
import com.reciclakids.model.Nino
import com.reciclakids.model.NivelesVolumen
import com.reciclakids.model.ProgresoNino
import com.reciclakids.ui.acceso.PuertaAdultosSheet
import com.reciclakids.ui.acceso.RutasAcceso
import com.reciclakids.ui.common.EstadoConexion
import com.reciclakids.ui.common.FondoSubmarino
import com.reciclakids.ui.common.caneca
import com.reciclakids.ui.common.rememberHayConexion
import com.reciclakids.ui.common.rememberLocutor
import com.reciclakids.ui.theme.ReciclaKidsColors
import com.reciclakids.viewmodel.CodigoRetoViewModel
import com.reciclakids.viewmodel.JuegoViewModel
import com.reciclakids.viewmodel.Retroalimentacion
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
    const val Resultado = "nino/resultado"
    const val InsigniaObtenida = "nino/insignia"
    const val TiempoTerminado = "nino/tiempo-terminado"

    /** Rutas sin niño elegido todavía, o donde ya no se cuenta el tiempo de juego. */
    val SinReloj = setOf(Codigo, QuienEres, TiempoTerminado)
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

        composable(RutasNino.Juego) { entrada ->
            val sesion = sesionNino(navController, entrada)
            val nino = sesion.nino
            val reto = sesion.reto
            if (nino == null || reto == null) {
                LaunchedEffect(Unit) { onSalir() }
                return@composable
            }
            val juego = viewModel(key = "juego-${reto.codigo}-${nino.id}") {
                JuegoViewModel(reto, nino, contenedor.juego, alTerminar = contenedor.programarSincronizacion)
            }
            val ajustes by contenedor.ajustes.observar().collectAsState(initial = AjustesNino())
            val alcance = rememberCoroutineScope()
            val locutor = rememberLocutor(ajustes.volumenSonidos / NivelesVolumen.toFloat())
            val haptico = LocalHapticFeedback.current
            val muyBien = stringResource(R.string.juego_muy_bien)
            val casi = stringResource(R.string.juego_casi)
            val guia = juego.residuoEnPantalla?.let { residuo ->
                stringResource(R.string.juego_guia, residuo.nombre, stringResource(residuo.categoria.caneca.etiqueta).lowercase())
            }

            LaunchedEffect(juego.retroalimentacion) {
                when (juego.retroalimentacion) {
                    Retroalimentacion.Acierto -> {
                        haptico.performHapticFeedback(HapticFeedbackType.Confirm)
                        locutor.decir(muyBien)
                    }
                    Retroalimentacion.Rebote -> locutor.decir(casi)
                    Retroalimentacion.Ninguna -> Unit
                }
            }
            LaunchedEffect(juego.resultado) {
                val resultado = juego.resultado ?: return@LaunchedEffect
                sesion.terminarReto(resultado)
                navController.navigate(RutasNino.Resultado) {
                    popUpTo(RutasNino.Juego) { inclusive = true }
                }
            }
            // El botón atrás del sistema pausa: el niño no sale del juego por accidente.
            BackHandler(enabled = !juego.pausado) { juego.pausar() }

            val estado = juego.estado
            if (estado == null) {
                FondoSubmarino(agua = ReciclaKidsColors.aguaJuego, altoArena = 90.dp, modifier = Modifier.fillMaxSize()) {}
                return@composable
            }
            Box(Modifier.fillMaxSize()) {
                JuegoScreen(
                    dificultad = reto.dificultad,
                    estado = estado,
                    residuo = juego.residuoEnPantalla,
                    retroalimentacion = juego.retroalimentacion,
                    fraccionTiempo = juego.fraccionTiempoRestante,
                    pausado = juego.pausado,
                    onClasificar = juego::clasificar,
                    onPausar = juego::pausar,
                    onGuia = { guia?.let(locutor::decir) },
                )
                if (juego.pausado) {
                    PausaDialog(
                        volumen = ajustes.volumenSonidos,
                        onVolumen = { nivel -> alcance.launch { contenedor.ajustes.guardar(ajustes.conSonidos(nivel)) } },
                        onContinuar = juego::reanudar,
                        onReiniciar = juego::reiniciar,
                        onMenu = { navController.popBackStack(RutasNino.Menu, inclusive = false) },
                    )
                }
            }
        }

        composable(RutasNino.Resultado) { entrada ->
            val sesion = sesionNino(navController, entrada)
            val resultado = sesion.ultimoResultado
            if (resultado == null) {
                LaunchedEffect(Unit) { navController.popBackStack(RutasNino.Menu, inclusive = false) }
                return@composable
            }
            val ajustes by contenedor.ajustes.observar().collectAsState(initial = AjustesNino())
            val locutor = rememberLocutor(ajustes.volumenSonidos / NivelesVolumen.toFloat())
            val anuncio = pluralStringResource(R.plurals.resultado_titulo, resultado.residuosSeparados, resultado.residuosSeparados) + " " +
                stringResource(if (resultado.sumoAlAcuario) R.string.resultado_acuario else R.string.resultado_buen_trabajo)
            val hayConexion by rememberHayConexion()
            val pendientes by contenedor.juego.intentosPendientes().collectAsState(initial = 0)
            LaunchedEffect(Unit) { locutor.decir(anuncio) }
            ResultadoScreen(
                resultado = resultado,
                onVerPremio = { navController.navigate(RutasNino.InsigniaObtenida) { launchSingleTop = true } },
                onMenu = { navController.popBackStack(RutasNino.Menu, inclusive = false) },
                guardado = when {
                    !hayConexion -> EstadoConexion.SinConexion
                    pendientes > 0 -> EstadoConexion.Sincronizando
                    else -> EstadoConexion.Sincronizado
                },
            )
        }

        composable(RutasNino.InsigniaObtenida) { entrada ->
            val sesion = sesionNino(navController, entrada)
            val nuevas = sesion.ultimoResultado?.insigniasNuevas.orEmpty()
            var indice by rememberSaveable { mutableIntStateOf(0) }
            val insignia = nuevas.getOrNull(indice)
            if (insignia == null) {
                LaunchedEffect(Unit) { navController.popBackStack(RutasNino.Menu, inclusive = false) }
                return@composable
            }
            val ajustes by contenedor.ajustes.observar().collectAsState(initial = AjustesNino())
            val locutor = rememberLocutor(ajustes.volumenSonidos / NivelesVolumen.toFloat())
            val anuncio = stringResource(R.string.insignia_nueva) + " " + insignia.nombre
            LaunchedEffect(insignia) { locutor.decir(anuncio) }
            InsigniaObtenidaScreen(
                insignia = insignia,
                // Si ganó varias, «Seguir» muestra la siguiente antes de volver al menú.
                onSeguir = {
                    if (indice < nuevas.lastIndex) indice++ else navController.popBackStack(RutasNino.Menu, inclusive = false)
                },
                onMisInsignias = {
                    navController.navigate(RutasNino.Insignias) { popUpTo(RutasNino.Menu) }
                },
            )
        }

        composable(RutasNino.Insignias) { entrada ->
            val sesion = sesionNino(navController, entrada)
            val nino = sesion.nino
            if (nino == null) {
                LaunchedEffect(Unit) { onSalir() }
                return@composable
            }
            val ganadas by contenedor.juego.insigniasGanadas(nino.id).collectAsState(initial = emptySet())
            val progreso by contenedor.progreso.observar(nino.id).collectAsState(initial = ProgresoNino(nino.id))
            val dias by contenedor.juego.diasConReto(nino.id).collectAsState(initial = 0)
            val ajustes by contenedor.ajustes.observar().collectAsState(initial = AjustesNino())
            val locutor = rememberLocutor(ajustes.volumenSonidos / NivelesVolumen.toFloat())
            val titulo = stringResource(R.string.coleccion_titulo)
            LaunchedEffect(Unit) { locutor.decir(titulo) }
            ColeccionInsigniasScreen(
                ganadas = ganadas,
                progreso = progreso,
                diasConReto = dias,
                onTocar = { locutor.decir(it.nombre) },
                onVolver = { navController.popBackStack() },
            )
        }

        composable(RutasNino.Ajustes) {
            val ajustes by contenedor.ajustes.observar().collectAsState(initial = AjustesNino())
            val alcance = rememberCoroutineScope()
            val locutor = rememberLocutor(ajustes.volumenSonidos / NivelesVolumen.toFloat())
            val titulo = stringResource(R.string.ajustes_titulo)
            var puertaVisible by rememberSaveable { mutableStateOf(false) }
            LaunchedEffect(Unit) { locutor.decir(titulo) }
            AjustesNinoScreen(
                ajustes = ajustes,
                onMusica = { nivel -> alcance.launch { contenedor.ajustes.guardar(ajustes.conMusica(nivel)) } },
                onSonidos = { nivel -> alcance.launch { contenedor.ajustes.guardar(ajustes.conSonidos(nivel)) } },
                onVolver = { navController.popBackStack() },
                onSeccionPadres = { puertaVisible = true },
            )
            if (puertaVisible) {
                PuertaAdultosSheet(
                    onAbierta = {
                        puertaVisible = false
                        // Sale del Modo Niño: al volver atrás desde el inicio de sesión se llega al selector.
                        navController.navigate(RutasAcceso.Login) { popUpTo(RutasAcceso.Selector) }
                    },
                    onCerrar = { puertaVisible = false },
                )
            }
        }

        composable(RutasNino.TiempoTerminado) {
            val locutor = rememberLocutor()
            val mensaje = stringResource(R.string.tiempo_titulo)
            LaunchedEffect(Unit) { locutor.decir(mensaje) }
            // No se puede saltar desde el Modo Niño: el botón atrás no hace nada.
            BackHandler { }
            TiempoTerminadoScreen(
                minutos = contenedor.controlParental.limiteMinutosDiarios,
                onRepetirVoz = { locutor.decir(mensaje) },
            )
        }
    }
}

/** Lleva a UI-19 desde cualquier pantalla del niño, sin dejar a dónde volver dentro del modo. */
fun NavController.irATiempoTerminado() {
    navigate(RutasNino.TiempoTerminado) {
        popUpTo(RutasNino.Grafo)
        launchSingleTop = true
    }
}

@Composable
private fun sesionNino(navController: NavController, entrada: NavBackStackEntry): SesionNinoViewModel {
    val grafo = remember(entrada) { navController.getBackStackEntry(RutasNino.Grafo) }
    return viewModel(grafo)
}
