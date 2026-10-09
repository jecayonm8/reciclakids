package com.reciclakids.ui.nino

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reciclakids.R
import com.reciclakids.model.CatalogoResiduos
import com.reciclakids.model.CategoriaResiduo
import com.reciclakids.model.Dificultad
import com.reciclakids.model.EstadoPartida
import com.reciclakids.model.Residuo
import com.reciclakids.ui.common.Alga
import com.reciclakids.ui.common.BotonCircularNino
import com.reciclakids.ui.common.Caneca
import com.reciclakids.ui.common.EstadoCaneca
import com.reciclakids.ui.common.EstadoResiduo
import com.reciclakids.ui.common.EstiloMarcador
import com.reciclakids.ui.common.FondoSubmarino
import com.reciclakids.ui.common.IndicadorRacha
import com.reciclakids.ui.common.MarcadorIlustracion
import com.reciclakids.ui.common.ObjetoResiduo
import com.reciclakids.ui.common.TipoCaneca
import com.reciclakids.ui.common.caneca
import com.reciclakids.ui.common.categoria
import com.reciclakids.ui.common.rebotar
import com.reciclakids.ui.common.sombraTitular
import com.reciclakids.ui.theme.BalooDos
import com.reciclakids.ui.theme.Duracion
import com.reciclakids.ui.theme.EscalaFijaNino
import com.reciclakids.ui.theme.ReciclaKidsColors
import com.reciclakids.ui.theme.ReciclaKidsTheme
import com.reciclakids.ui.theme.ResorteRebote
import com.reciclakids.ui.theme.Tactil
import com.reciclakids.viewmodel.Retroalimentacion
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.hypot

/** Tiempo sin tocar nada tras el cual la caneca correcta se vuelve guía. */
const val InactividadGuiaMs = 5_000L

/** Lado del residuo en pantalla. */
private val LadoResiduo = 132.dp

/** Distancia del borde superior al residuo en reposo, como en el prototipo de 360 × 800. */
private val AlturaResiduo = 230.dp

/**
 * UI-12 Juego y UI-13 Microinteracciones. Un residuo a la vez y las canecas abajo, al alcance del
 * pulgar. El arrastre es tolerante: si se suelta a menos de 110 dp del centro de una caneca, entra;
 * si no, vuelve flotando y no cuenta como error. Tocar una caneca evalúa igual que arrastrar.
 *
 * [onGuia] avisa cuando, tras 5 s sin tocar, la caneca correcta empieza a hacer de guía.
 */
@Composable
fun JuegoScreen(
    dificultad: Dificultad,
    estado: EstadoPartida,
    residuo: Residuo?,
    retroalimentacion: Retroalimentacion,
    fraccionTiempo: Float?,
    pausado: Boolean,
    onClasificar: (CategoriaResiduo) -> Unit,
    onPausar: () -> Unit,
    onGuia: (Residuo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val canecas = dificultad.categorias.map { it.caneca }.sortedBy { it.ordinal }
    val dosCanecas = canecas.size == 2
    val radioIman = with(LocalDensity.current) { Tactil.radioIman.toPx() }
    val alcance = rememberCoroutineScope()
    val clasificar by rememberUpdatedState(onClasificar)

    // Centros de las canecas y del residuo en reposo, en coordenadas de la raíz.
    val centros = remember { mutableStateMapOf<TipoCaneca, Offset>() }
    var centroResiduo by remember { mutableStateOf(Offset.Zero) }
    // Mientras el dedo arrastra, el residuo lo sigue sin animación; al soltar, «regreso» lo anima.
    var arrastre by remember { mutableStateOf(Offset.Zero) }
    val regreso = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var arrastrando by remember { mutableStateOf(false) }
    var iman by remember { mutableStateOf<TipoCaneca?>(null) }
    var interacciones by remember { mutableIntStateOf(0) }
    var guia by remember { mutableStateOf(false) }
    val celebracion = remember { Animatable(1f) }

    val habilitado = residuo != null && retroalimentacion == Retroalimentacion.Ninguna && !pausado

    fun canecaCercana(desplazamiento: Offset): TipoCaneca? {
        val punto = centroResiduo + desplazamiento
        return centros.entries
            .map { (tipo, centro) -> tipo to hypot(punto.x - centro.x, punto.y - centro.y) }
            .filter { it.second < radioIman }
            .minByOrNull { it.second }
            ?.first
    }

    // Un residuo nuevo empieza en su sitio.
    LaunchedEffect(residuo) {
        arrastre = Offset.Zero
        regreso.snapTo(Offset.Zero)
    }
    // Tras un error el residuo vuelve flotando con resorte; nunca cuenta como castigo.
    LaunchedEffect(retroalimentacion) {
        when (retroalimentacion) {
            Retroalimentacion.Rebote -> regreso.animateTo(Offset.Zero, ResorteOffset)
            Retroalimentacion.Acierto -> celebracion.rebotar(Duracion.limiteFeedback)
            Retroalimentacion.Ninguna -> Unit
        }
    }
    // Ayuda progresiva: a los 5 s sin tocar, la caneca correcta hace de guía.
    LaunchedEffect(residuo, interacciones, retroalimentacion, pausado) {
        guia = false
        if (residuo == null || pausado || retroalimentacion != Retroalimentacion.Ninguna) return@LaunchedEffect
        delay(InactividadGuiaMs)
        guia = true
        onGuia(residuo)
    }

    EscalaFijaNino {
        FondoSubmarino(
            agua = ReciclaKidsColors.aguaJuego,
            altoArena = 90.dp,
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.TopStart,
            decoracion = {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    Alga(90.dp, Modifier.offset(maxWidth * 0.03f, maxHeight - 170.dp).graphicsLayer { alpha = 0.9f }, ancho = 18.dp)
                    Alga(70.dp, Modifier.offset(maxWidth * 0.96f - 14.dp, maxHeight - 150.dp).graphicsLayer { alpha = 0.9f }, ancho = 14.dp)
                }
            },
        ) {
            Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                MarcadorJuego(
                    estado = estado,
                    fraccionTiempo = fraccionTiempo,
                    onPausar = onPausar,
                    modifier = Modifier.align(Alignment.TopCenter).padding(14.dp),
                )

                // Residuo: se arrastra con el dedo; el imán decide a qué caneca entra.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = AlturaResiduo)
                        .size(LadoResiduo)
                        .onGloballyPositioned { centroResiduo = it.boundsInRoot().center },
                ) {
                    if (residuo != null) {
                        ObjetoResiduo(
                            nombre = residuo.nombre,
                            estado = when {
                                arrastrando -> EstadoResiduo.Arrastrando
                                retroalimentacion == Retroalimentacion.Rebote -> EstadoResiduo.Rebote
                                else -> EstadoResiduo.Reposo
                            },
                            enIman = iman != null,
                            tamano = LadoResiduo,
                            modifier = Modifier
                                .graphicsLayer {
                                    val desplazamiento = if (arrastrando) arrastre else regreso.value
                                    translationX = desplazamiento.x
                                    translationY = desplazamiento.y
                                    alpha = if (retroalimentacion == Retroalimentacion.Acierto) 0f else 1f
                                }
                                .pointerInput(residuo, habilitado) {
                                    if (!habilitado) return@pointerInput
                                    detectDragGestures(
                                        onDragStart = {
                                            arrastre = regreso.value
                                            arrastrando = true
                                            interacciones++
                                        },
                                        onDrag = { cambio, delta ->
                                            cambio.consume()
                                            arrastre += delta
                                            iman = canecaCercana(arrastre)
                                        },
                                        onDragEnd = {
                                            val destino = iman
                                            iman = null
                                            alcance.launch(start = CoroutineStart.UNDISPATCHED) {
                                                regreso.snapTo(arrastre)
                                                arrastrando = false
                                                if (destino != null) {
                                                    val centro = centros.getValue(destino)
                                                    regreso.animateTo(centro - centroResiduo, tween(Duracion.rapida))
                                                    clasificar(destino.categoria)
                                                } else {
                                                    // Soltar lejos no es un error: vuelve a su sitio con resorte.
                                                    regreso.animateTo(Offset.Zero, ResorteOffset)
                                                }
                                            }
                                        },
                                        onDragCancel = {
                                            iman = null
                                            alcance.launch(start = CoroutineStart.UNDISPATCHED) {
                                                regreso.snapTo(arrastre)
                                                arrastrando = false
                                                regreso.animateTo(Offset.Zero, ResorteOffset)
                                            }
                                        },
                                    )
                                },
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(if (dosCanecas) 24.dp else 10.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    canecas.forEach { tipo ->
                        val correcta = residuo?.categoria?.caneca == tipo
                        Caneca(
                            tipo = tipo,
                            ancho = if (dosCanecas) 120.dp else 98.dp,
                            estado = when {
                                iman == tipo -> EstadoCaneca.Resaltada
                                correcta && (retroalimentacion == Retroalimentacion.Rebote || estado.erroresResiduoActual >= 2) ->
                                    EstadoCaneca.Brillante
                                correcta && guia -> EstadoCaneca.Guia
                                else -> EstadoCaneca.Normal
                            },
                            onClick = {
                                if (habilitado) {
                                    interacciones++
                                    clasificar(tipo.categoria)
                                }
                            },
                            modifier = Modifier.onGloballyPositioned { centros[tipo] = it.boundsInRoot().center },
                        )
                    }
                }

                if (retroalimentacion == Retroalimentacion.Acierto) {
                    CelebracionAcierto(Modifier.align(Alignment.Center).graphicsLayer {
                        scaleX = celebracion.value
                        scaleY = celebracion.value
                    })
                }
                if (retroalimentacion == Retroalimentacion.Rebote) {
                    MensajeCasi(Modifier.align(Alignment.TopCenter).padding(top = 170.dp, start = 14.dp, end = 14.dp))
                }
            }
        }
    }
}

private val ResorteOffset = spring(dampingRatio = ResorteRebote.dampingRatio, stiffness = ResorteRebote.stiffness, visibilityThreshold = Offset(0.5f, 0.5f))

/** Barra superior: pausa, avance sin números, racha, puntos y, en difícil, el temporizador. */
@Composable
private fun MarcadorJuego(
    estado: EstadoPartida,
    fraccionTiempo: Float?,
    onPausar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BotonCircularNino(
                icono = painterResource(R.drawable.ic_pausa),
                descripcion = stringResource(R.string.juego_pausa),
                onClick = onPausar,
                tamanoIcono = 32.dp,
            )
            val avance = pluralStringResource(R.plurals.juego_avance, estado.residuos.size, estado.aciertos, estado.residuos.size)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(72.dp)
                    .background(ReciclaKidsColors.panelNino, CircleShape)
                    .padding(horizontal = 14.dp)
                    .clearAndSetSemantics { contentDescription = avance },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                repeat(estado.residuos.size) { i ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(22.dp)
                            .background(if (i < estado.aciertos) ReciclaKidsColors.algaSana else Color(0xFFDCE7EB), RoundedCornerShape(11.dp))
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IndicadorRacha(estado.racha)
            PastillaPuntos(estado.puntaje)
            if (fraccionTiempo != null) {
                Spacer(Modifier.weight(1f))
                Temporizador(fraccionTiempo)
            }
        }
    }
}

@Composable
private fun PastillaPuntos(puntaje: Int) {
    val descripcion = pluralStringResource(R.plurals.juego_puntos, puntaje, puntaje)
    Row(
        modifier = Modifier
            .height(56.dp)
            .background(ReciclaKidsColors.panelNino, CircleShape)
            .padding(horizontal = 16.dp)
            .clearAndSetSemantics { contentDescription = descripcion },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Image(painterResource(R.drawable.ic_estrella_dorada), null, Modifier.size(24.dp))
        Text(puntaje.toString(), fontFamily = BalooDos, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = ReciclaKidsColors.tintaNino)
    }
}

/** Temporizador circular sin números: el anillo turquesa se vacía con el tiempo. */
@Composable
private fun Temporizador(fraccion: Float) {
    val descripcion = stringResource(R.string.juego_tiempo)
    val turquesa = Color(0xFF16A3B8)
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(ReciclaKidsColors.panelNino, CircleShape)
            .clearAndSetSemantics { contentDescription = descripcion },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(44.dp)) {
            val grosor = 7.dp.toPx()
            val lado = size.width - grosor
            val esquina = Offset(grosor / 2, grosor / 2)
            drawArc(Color(0xFFDCE7EB), 0f, 360f, false, esquina, Size(lado, lado), style = Stroke(grosor))
            drawArc(turquesa, -90f, 360f * fraccion, false, esquina, Size(lado, lado), style = Stroke(grosor))
        }
        Icon(painterResource(R.drawable.ic_reloj), null, Modifier.size(22.dp), tint = turquesa)
    }
}

@Composable
private fun CelebracionAcierto(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier.size(150.dp).background(Color(0xEBB6F0C6), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_check), null, Modifier.size(78.dp), tint = ReciclaKidsColors.botonConfirmarSombra)
        }
        Text(
            stringResource(R.string.juego_muy_bien),
            fontFamily = BalooDos,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 40.sp,
            color = Color.White,
            style = TextStyle(shadow = sombraTitular()),
        )
    }
}

@Composable
private fun MensajeCasi(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.94f), RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MarcadorIlustracion("pulpo", Modifier.size(52.dp), estilo = EstiloMarcador.Avatar)
        Text(
            stringResource(R.string.juego_casi),
            fontFamily = BalooDos,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 26.sp,
            color = ReciclaKidsColors.tintaNino,
        )
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun JuegoScreenPreview() {
    ReciclaKidsTheme {
        JuegoScreen(
            dificultad = Dificultad.Dificil,
            estado = EstadoPartida(CatalogoResiduos.todos, indice = 3, aciertos = 3, racha = 3, puntaje = 60),
            residuo = CatalogoResiduos.lata,
            retroalimentacion = Retroalimentacion.Ninguna,
            fraccionTiempo = 0.62f,
            pausado = false,
            onClasificar = {},
            onPausar = {},
            onGuia = {},
        )
    }
}
