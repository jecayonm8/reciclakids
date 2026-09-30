package com.reciclakids.ui.juego

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reciclakids.R
import com.reciclakids.ui.theme.BalooDos
import com.reciclakids.ui.theme.Duracion
import com.reciclakids.ui.theme.ReciclaKidsColors
import com.reciclakids.ui.theme.ReciclaKidsTheme

/**
 * Código de colores colombiano (Resolución 2184 de 2019). Cada caneca se distingue siempre
 * por color + ícono + rótulo + personaje, nunca solo por color.
 */
enum class TipoCaneca(
    @param:StringRes val etiqueta: Int,
    @param:StringRes val descripcion: Int,
    @param:DrawableRes val icono: Int,
    val fondo: Color,
    val borde: Color,
    val personaje: TipoPersonaje,
) {
    Blanca(
        R.string.caneca_blanca, R.string.caneca_blanca_descripcion, R.drawable.ic_caneca_reciclaje,
        ReciclaKidsColors.canecaBlanca, ReciclaKidsColors.canecaBlancaBorde, TipoPersonaje.Pez,
    ),
    Negra(
        R.string.caneca_negra, R.string.caneca_negra_descripcion, R.drawable.ic_caneca_no_aprovechable,
        ReciclaKidsColors.canecaNegra, ReciclaKidsColors.canecaNegra, TipoPersonaje.Pulpo,
    ),
    Verde(
        R.string.caneca_verde, R.string.caneca_verde_descripcion, R.drawable.ic_caneca_organico,
        ReciclaKidsColors.canecaVerde, ReciclaKidsColors.canecaVerde, TipoPersonaje.Tortuga,
    ),
}

enum class EstadoCaneca {
    Normal,

    /** Hay un objeto dentro del radio del imán: escala 1,06 y halo. */
    Resaltada,

    /** Pista tras 5 s sin tocar o tras un error: halo y flecha en bucle. Nunca bloquea el juego. */
    Guia,
}

private val FormaTapa = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomEnd = 5.dp, bottomStart = 5.dp)
private val FormaCuerpo = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomEnd = 22.dp, bottomStart = 22.dp)

/**
 * Caneca del juego. [ancho] es el del cuerpo: 98 dp con tres canecas (Medio y Difícil) y
 * 120 dp con dos (Fácil). Tocarla con el residuo visible evalúa igual que arrastrar hasta ella.
 */
@Composable
fun Caneca(
    tipo: TipoCaneca,
    modifier: Modifier = Modifier,
    estado: EstadoCaneca = EstadoCaneca.Normal,
    ancho: Dp = 98.dp,
    onClick: (() -> Unit)? = null,
) {
    val escala by animateFloatAsState(
        targetValue = if (estado == EstadoCaneca.Resaltada) 1.06f else 1f,
        animationSpec = tween(120, easing = EaseOut),
        label = "escalaCaneca",
    )
    val encendida = estado != EstadoCaneca.Normal
    val descripcion = stringResource(tipo.descripcion)

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
            }
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                }
            )
            .semantics { contentDescription = descripcion },
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy((-7).dp)) {
                Box(
                    Modifier
                        .size(ancho - 18.dp, 26.dp)
                        .background(tipo.fondo, FormaTapa)
                        .border(3.dp, tipo.borde, FormaTapa)
                )
                Box(
                    modifier = Modifier
                        .size(ancho, 110.dp)
                        .haloPulsante(FormaCuerpo, activo = encendida)
                        .background(tipo.fondo, FormaCuerpo)
                        .border(3.dp, if (encendida) ReciclaKidsColors.halo else tipo.borde, FormaCuerpo),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(painterResource(tipo.icono), contentDescription = null, modifier = Modifier.size(46.dp))
                }
            }
            Text(
                text = stringResource(tipo.etiqueta),
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.88f), CircleShape)
                    .padding(horizontal = 10.dp, vertical = 2.dp)
                    .clearAndSetSemantics { },
                fontFamily = BalooDos,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                lineHeight = 22.sp,
                color = ReciclaKidsColors.tintaSobreAgua,
            )
        }
        if (estado == EstadoCaneca.Guia) FlechaGuia(Modifier.offset(y = (-46).dp))
    }
}

@Composable
private fun FlechaGuia(modifier: Modifier = Modifier) {
    val transicion = rememberInfiniteTransition(label = "guia")
    val avance by transicion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(Duracion.guiaCiclo / 2, easing = EaseInOut), RepeatMode.Reverse),
        label = "guia",
    )
    Icon(
        painter = painterResource(R.drawable.ic_flecha_abajo),
        contentDescription = null,
        modifier = modifier
            .size(40.dp)
            .graphicsLayer {
                translationY = 10.dp.toPx() * avance
                rotationZ = -3f + 6f * avance
            },
        tint = MaterialTheme.colorScheme.primary,
    )
}

@Preview
@Composable
private fun CanecasPreview() {
    ReciclaKidsTheme {
        FondoSubmarino(agua = ReciclaKidsColors.aguaJuego, altoArena = 90.dp) {
            Row(Modifier.padding(top = 56.dp, bottom = 18.dp, start = 12.dp, end = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Caneca(TipoCaneca.Blanca, estado = EstadoCaneca.Guia)
                Caneca(TipoCaneca.Negra)
                Caneca(TipoCaneca.Verde, estado = EstadoCaneca.Resaltada)
            }
        }
    }
}
