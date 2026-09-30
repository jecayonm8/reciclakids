package com.reciclakids.ui.juego

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.reciclakids.ui.theme.Duracion
import com.reciclakids.ui.theme.ReciclaKidsColors

/** Vaivén vertical suave de lo que flota en el agua (personajes, residuo en reposo). */
@Composable
fun Modifier.flotar(amplitud: Dp = 10.dp, periodoMs: Int = 4000): Modifier {
    val transicion = rememberInfiniteTransition(label = "flotar")
    val avance by transicion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(periodoMs / 2, easing = EaseInOut), RepeatMode.Reverse),
        label = "flotar",
    )
    return graphicsLayer { translationY = -amplitud.toPx() * avance }
}

/** Halo turquesa que late alrededor de [forma]: caneca resaltada, guía o imán. */
@Composable
fun Modifier.haloPulsante(
    forma: Shape,
    activo: Boolean = true,
    color: Color = ReciclaKidsColors.halo,
    alcance: Dp = 14.dp,
    periodoMs: Int = 1200,
): Modifier {
    if (!activo) return this
    val transicion = rememberInfiniteTransition(label = "halo")
    val avance by transicion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(periodoMs / 2, easing = EaseInOut), RepeatMode.Reverse),
        label = "halo",
    )
    return drawBehind {
        val grosor = alcance.toPx() * avance
        if (grosor <= 0f) return@drawBehind
        val contorno = Path().apply {
            when (val outline = forma.createOutline(size, layoutDirection, this@drawBehind)) {
                is Outline.Rectangle -> addRect(outline.rect)
                is Outline.Rounded -> addRoundRect(outline.roundRect)
                is Outline.Generic -> addPath(outline.path)
            }
        }
        // Solo se pinta hacia afuera: el trazo se dibuja centrado y se recorta el interior.
        clipPath(contorno, clipOp = ClipOp.Difference) {
            drawPath(contorno, color.copy(alpha = 0.7f * (1f - avance)), style = Stroke(width = grosor * 2))
        }
    }
}

/** Rebote de celebración o de «casi»: crece, se encoge un poco y vuelve a su tamaño. */
suspend fun Animatable<Float, AnimationVector1D>.rebotar(duracionMs: Int = Duracion.enfatica) {
    snapTo(1f)
    animateTo(
        targetValue = 1f,
        animationSpec = keyframes {
            durationMillis = duracionMs
            1f at 0
            1.18f at (duracionMs * 0.35f).toInt()
            0.94f at (duracionMs * 0.6f).toInt()
            1f at duracionMs
        },
    )
}
