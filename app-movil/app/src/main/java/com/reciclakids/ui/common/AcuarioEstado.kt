package com.reciclakids.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.reciclakids.ui.theme.ReciclaKidsColors
import com.reciclakids.ui.theme.ReciclaKidsTheme

/**
 * Fondo de agua con franja de arena que comparten las pantallas del Modo Niño.
 * [decoracion] queda detrás de [content] (algas, burbujas, contaminación).
 */
@Composable
fun FondoSubmarino(
    agua: Brush,
    modifier: Modifier = Modifier,
    altoArena: Dp = 70.dp,
    colorArena: Color = ReciclaKidsColors.arena,
    contentAlignment: Alignment = Alignment.Center,
    decoracion: @Composable BoxScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.background(agua), contentAlignment = contentAlignment) {
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(altoArena)
                .background(colorArena)
        )
        Box(Modifier.matchParentSize()) { decoracion() }
        content()
    }
}

@Composable
fun Alga(alto: Dp, modifier: Modifier = Modifier, ancho: Dp = 22.dp, color: Color = ReciclaKidsColors.algaSana) {
    Box(modifier.size(ancho, alto).background(color, RoundedCornerShape(12.dp)))
}

/** Burbuja que sube 200 dp desde donde se coloca, aparece y se desvanece, en bucle. */
@Composable
fun Burbuja(
    modifier: Modifier = Modifier,
    tamano: Dp = 14.dp,
    periodoMs: Int = 4000,
    retrasoMs: Int = 0,
    opacidad: Float = 0.7f,
) {
    val transicion = rememberInfiniteTransition(label = "burbuja")
    val avance by transicion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(periodoMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(retrasoMs),
        ),
        label = "burbuja",
    )
    Box(
        modifier
            .size(tamano)
            .graphicsLayer {
                translationY = -200.dp.toPx() * avance
                val escala = 0.6f + 0.4f * avance
                scaleX = escala
                scaleY = escala
                alpha = if (avance < 0.25f) avance / 0.25f else (1f - avance) / 0.75f
            }
            .background(Color.White.copy(alpha = opacidad), CircleShape)
    )
}

/**
 * El acuario es el marcador de progreso del niño: cuatro niveles de limpieza que se leen
 * sin texto. Nunca retrocede dentro de una sesión.
 *
 * Nivel 1 turbio (0–25 %), 2 opaco (26–50 %), 3 claro (51–80 %), 4 limpio (81–100 %).
 */
@Composable
fun AcuarioEstado(
    nivel: Int,
    modifier: Modifier = Modifier,
    altoArena: Dp = 70.dp,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val nivelAcotado = nivel.coerceIn(1, 4)
    val agua = when (nivelAcotado) {
        1 -> ReciclaKidsColors.acuarioNivel1Turbio
        2 -> ReciclaKidsColors.acuarioNivel2Opaco
        3 -> ReciclaKidsColors.acuarioNivel3Claro
        else -> ReciclaKidsColors.acuarioNivel4Limpio
    }
    val sucio = nivelAcotado <= 2
    FondoSubmarino(
        agua = agua,
        modifier = modifier,
        altoArena = altoArena,
        colorArena = if (sucio) ReciclaKidsColors.arenaSucia else ReciclaKidsColors.arena,
        decoracion = {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val ancho = maxWidth
                val alto = maxHeight
                if (sucio) {
                    Contaminante(30.dp, 18.dp, 8.dp, ReciclaKidsColors.contaminacion, Modifier.offset(ancho * 0.14f, alto * 0.24f))
                    Contaminante(22.dp, 22.dp, 6.dp, Color(0xFF4A4236), Modifier.offset(ancho * 0.82f - 22.dp, alto * 0.38f))
                    Contaminante(26.dp, 14.dp, 8.dp, Color(0xFF5A5142), Modifier.offset(ancho * 0.38f, alto - altoArena - 54.dp))
                } else {
                    // Las algas y las burbujas nacen 8 dp dentro de la arena.
                    val suelo = altoArena - 8.dp
                    Alga(120.dp, Modifier.offset(ancho * 0.05f, alto - suelo - 120.dp))
                    Alga(90.dp, Modifier.offset(ancho * 0.93f - 18.dp, alto - suelo - 90.dp), ancho = 18.dp)
                    Burbuja(Modifier.offset(ancho * 0.26f, alto - altoArena - 14.dp))
                    Burbuja(
                        Modifier.offset(ancho * 0.70f - 10.dp, alto - altoArena - 10.dp),
                        tamano = 10.dp, periodoMs = 5000, retrasoMs = 1000, opacidad = 0.6f,
                    )
                }
            }
        },
        content = content,
    )
}

@Composable
private fun Contaminante(ancho: Dp, alto: Dp, radio: Dp, color: Color, modifier: Modifier) {
    Box(modifier.size(ancho, alto).background(color.copy(alpha = 0.8f), RoundedCornerShape(radio)))
}

@Preview(widthDp = 360, heightDp = 300)
@Composable
private fun AcuarioNivel1Preview() {
    ReciclaKidsTheme { AcuarioEstado(nivel = 1, modifier = Modifier.fillMaxSize()) }
}

@Preview(widthDp = 360, heightDp = 300)
@Composable
private fun AcuarioNivel4Preview() {
    ReciclaKidsTheme { AcuarioEstado(nivel = 4, modifier = Modifier.fillMaxSize()) }
}
