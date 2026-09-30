package com.reciclakids.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reciclakids.R
import com.reciclakids.ui.theme.Duracion
import com.reciclakids.ui.theme.ReciclaKidsColors
import com.reciclakids.ui.theme.ReciclaKidsLightColors
import com.reciclakids.ui.theme.ReciclaKidsTheme
import com.reciclakids.ui.theme.Tactil
import com.reciclakids.ui.theme.TipografiaNino

enum class VarianteBotonNino(val fondo: Color, val sombra: Color, val contenido: Color, val icono: Color) {
    /** Acción principal de la pantalla: «¡A jugar!», «Reto diario», «Ver premio». */
    Jugar(ReciclaKidsColors.botonJugar, ReciclaKidsColors.botonJugarSombra, ReciclaKidsColors.onBotonJugar, ReciclaKidsColors.onBotonJugar),

    /** Avanzar o confirmar: «¡Ya entendí!», «Continuar», «Seguir». */
    Confirmar(ReciclaKidsColors.botonConfirmar, ReciclaKidsColors.botonConfirmarSombra, Color.White, Color.White),

    /** Acciones de apoyo: «Menú», «Insignias», «Ajustes». */
    Secundario(Color.White, ReciclaKidsColors.botonSecundarioSombra, ReciclaKidsColors.tintaNino, ReciclaKidsLightColors.primary),
}

/**
 * Botón del Modo Niño: cara de color con una sombra inferior sólida que se hunde al presionar.
 * La sombra se pinta [profundidad] por debajo de los límites del botón, así que el padre debe
 * dejar ese espacio libre. [alto] nunca debe bajar de 72 dp.
 */
@Composable
fun BotonNino(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variante: VarianteBotonNino = VarianteBotonNino.Jugar,
    alto: Dp = 84.dp,
    forma: Shape = CircleShape,
    profundidad: Dp = 8.dp,
    content: @Composable () -> Unit,
) {
    val fuente = remember { MutableInteractionSource() }
    val presionado by fuente.collectIsPressedAsState()
    val hundimiento by animateDpAsState(
        targetValue = if (presionado) profundidad / 2 else 0.dp,
        animationSpec = tween(Duracion.tacto, easing = LinearEasing),
        label = "hundimiento",
    )
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = Tactil.minimoNino)
            .height(alto)
            .clickable(fuente, indication = null, role = Role.Button, onClick = onClick)
            .drawBehind {
                val contorno = forma.createOutline(size, layoutDirection, this)
                translate(top = profundidad.toPx()) { drawOutline(contorno, variante.sombra) }
            }
            .offset { IntOffset(0, hundimiento.roundToPx()) }
            .background(variante.fondo, forma)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides variante.contenido, content = content)
    }
}

/** Botón del Modo Niño con ícono y texto en fila. El ícono va antes del texto salvo [iconoAlFinal]. */
@Composable
fun BotonNino(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icono: Painter? = null,
    variante: VarianteBotonNino = VarianteBotonNino.Jugar,
    alto: Dp = 84.dp,
    forma: Shape = CircleShape,
    profundidad: Dp = 8.dp,
    estiloTexto: TextStyle = TipografiaNino.boton,
    tamanoIcono: Dp = 40.dp,
    iconoAlFinal: Boolean = false,
) {
    BotonNino(onClick, modifier, variante, alto, forma, profundidad) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            val icon = @Composable {
                if (icono != null) Icon(icono, contentDescription = null, modifier = Modifier.size(tamanoIcono), tint = variante.icono)
            }
            if (!iconoAlFinal) icon()
            Text(texto, style = estiloTexto, maxLines = 1)
            if (iconoAlFinal) icon()
        }
    }
}

/**
 * Botón redondo del Modo Niño para controles sin texto: repetir voz, pausa, volver.
 * Con [conBorde] usa el contorno primario del botón de voz.
 */
@Composable
fun BotonCircularNino(
    icono: Painter,
    descripcion: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tamano: Dp = Tactil.minimoNino,
    tamanoIcono: Dp = 34.dp,
    conBorde: Boolean = false,
    tinta: Color = MaterialTheme.colorScheme.primary,
) {
    val fuente = remember { MutableInteractionSource() }
    val presionado by fuente.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (presionado) 0.94f else 1f,
        animationSpec = tween(Duracion.tacto, easing = LinearEasing),
        label = "escalaBoton",
    )
    Box(
        modifier = modifier
            .size(tamano)
            .clickable(fuente, indication = null, role = Role.Button, onClick = onClick)
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
            }
            .background(if (conBorde) Color.White else ReciclaKidsColors.panelNino, CircleShape)
            .then(if (conBorde) Modifier.border(3.dp, tinta, CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icono, contentDescription = descripcion, modifier = Modifier.size(tamanoIcono), tint = tinta)
    }
}

@Preview(widthDp = 360)
@Composable
private fun BotonesNinoPreview() {
    ReciclaKidsTheme {
        FondoSubmarino(agua = ReciclaKidsColors.aguaJuego) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                BotonNino(
                    texto = "¡A jugar!",
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    icono = painterResource(R.drawable.ic_play),
                    alto = 130.dp,
                    forma = RoundedCornerShape(44.dp),
                    profundidad = 10.dp,
                    estiloTexto = TipografiaNino.boton.copy(fontSize = 44.sp, lineHeight = 48.sp, fontWeight = FontWeight.ExtraBold),
                    tamanoIcono = 56.dp,
                )
                BotonNino(
                    texto = "¡Ya entendí!",
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    icono = painterResource(R.drawable.ic_check),
                    variante = VarianteBotonNino.Confirmar,
                    iconoAlFinal = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    BotonNino("Menú", onClick = {}, variante = VarianteBotonNino.Secundario, modifier = Modifier.weight(1f))
                    BotonCircularNino(painterResource(R.drawable.ic_altavoz), "Repetir", onClick = {}, conBorde = true)
                }
            }
        }
    }
}
