package com.reciclakids.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reciclakids.R
import com.reciclakids.ui.theme.BalooDos
import com.reciclakids.ui.theme.Nunito
import com.reciclakids.ui.theme.ReciclaKidsColors
import com.reciclakids.ui.theme.ReciclaKidsTheme

private val FormaTarjeta = RoundedCornerShape(20.dp)

/**
 * Insignia de la colección. La pendiente se muestra como silueta punteada y [nota] dice
 * cuánto falta («Faltan 2 retos»): nunca tachada ni en gris «triste».
 */
@Composable
fun TarjetaInsignia(
    nombre: String,
    nota: String,
    ganada: Boolean,
    modifier: Modifier = Modifier,
    ilustracion: Painter? = null,
) {
    val descripcion = stringResource(
        if (ganada) R.string.insignia_ganada_descripcion else R.string.insignia_pendiente_descripcion,
        nombre,
        nota,
    )
    Column(
        modifier = modifier
            .then(
                if (ganada) {
                    Modifier
                        .background(Color.White.copy(alpha = 0.95f), FormaTarjeta)
                        .border(3.dp, ReciclaKidsColors.insigniaBorde, FormaTarjeta)
                } else {
                    Modifier
                        .background(Color.White.copy(alpha = 0.35f), FormaTarjeta)
                        .bordePunteado(Color.White.copy(alpha = 0.7f))
                }
            )
            .padding(12.dp)
            .clearAndSetSemantics { contentDescription = descripcion },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        when {
            !ganada -> Box(
                Modifier
                    .size(82.dp)
                    .background(Color.White.copy(alpha = 0.35f), CircleShape)
                    .border(3.dp, Color.White.copy(alpha = 0.6f), CircleShape)
            )
            ilustracion != null -> Image(ilustracion, contentDescription = null, modifier = Modifier.size(82.dp).clip(CircleShape))
            else -> MarcadorIlustracion("ilustración", Modifier.size(82.dp), estilo = EstiloMarcador.Insignia)
        }
        Text(
            text = nombre,
            fontFamily = BalooDos,
            fontWeight = FontWeight.Bold,
            fontSize = 19.sp,
            lineHeight = 21.sp,
            textAlign = TextAlign.Center,
            color = if (ganada) ReciclaKidsColors.tintaNino else Color.White.copy(alpha = 0.95f),
        )
        Text(
            text = nota,
            fontFamily = Nunito,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            textAlign = TextAlign.Center,
            color = if (ganada) MaterialTheme.colorScheme.tertiary else Color.White.copy(alpha = 0.88f),
        )
    }
}

private fun Modifier.bordePunteado(color: Color): Modifier = drawBehind {
    val grosor = 3.dp.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(grosor / 2, grosor / 2),
        size = Size(size.width - grosor, size.height - grosor),
        cornerRadius = CornerRadius(20.dp.toPx() - grosor / 2),
        style = Stroke(grosor, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))),
    )
}

@Preview(widthDp = 360)
@Composable
private fun TarjetaInsigniaPreview() {
    ReciclaKidsTheme {
        FondoSubmarino(agua = ReciclaKidsColors.acuarioNivel3Claro) {
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TarjetaInsignia("Amiga tortuga", "Sin errores", ganada = true, modifier = Modifier.weight(1f))
                TarjetaInsignia("Pulpo ordenado", "Faltan 2 retos", ganada = false, modifier = Modifier.weight(1f))
            }
        }
    }
}
