package com.reciclakids.ui.juego

import androidx.annotation.StringRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reciclakids.R
import com.reciclakids.ui.theme.Nunito
import com.reciclakids.ui.theme.ReciclaKidsColors
import com.reciclakids.ui.theme.ReciclaKidsTheme

enum class EstadoConexion(@param:StringRes val texto: Int) {
    Conectado(R.string.conexion_conectado),

    /** El juego sigue igual: todo se guarda en el teléfono. */
    SinConexion(R.string.conexion_guardado_aqui),
    Sincronizando(R.string.conexion_guardando),
    Sincronizado(R.string.conexion_guardado),
}

/**
 * Pastilla discreta del Modo Niño para la esquina superior derecha. Sin alertas, sin rojo y
 * sin bloquear el juego: el niño no necesita entenderla.
 */
@Composable
fun IndicadorSinConexion(estado: EstadoConexion, modifier: Modifier = Modifier) {
    val gris = MaterialTheme.colorScheme.secondary
    Row(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.9f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        when (estado) {
            EstadoConexion.Conectado ->
                Icon(painterResource(R.drawable.ic_wifi), contentDescription = null, modifier = Modifier.size(18.dp), tint = gris)
            EstadoConexion.SinConexion ->
                Icon(painterResource(R.drawable.ic_wifi_off), contentDescription = null, modifier = Modifier.size(18.dp), tint = gris)
            EstadoConexion.Sincronizando -> Girador()
            EstadoConexion.Sincronizado ->
                Icon(
                    painterResource(R.drawable.ic_check), contentDescription = null,
                    modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.tertiary,
                )
        }
        Text(
            text = stringResource(estado.texto),
            fontFamily = Nunito,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun Girador() {
    val transicion = rememberInfiniteTransition(label = "girador")
    val giro by transicion.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
        label = "girador",
    )
    Canvas(Modifier.size(16.dp).graphicsLayer { rotationZ = giro }) {
        val grosor = 2.dp.toPx()
        drawArc(
            color = Color(0xFF16A3B8),
            startAngle = 0f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = Offset(grosor / 2, grosor / 2),
            size = Size(size.width - grosor, size.height - grosor),
            style = Stroke(grosor, cap = StrokeCap.Butt),
        )
    }
}

@Preview
@Composable
private fun IndicadorSinConexionPreview() {
    ReciclaKidsTheme {
        FondoSubmarino(agua = ReciclaKidsColors.aguaJuego) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EstadoConexion.entries.forEach { IndicadorSinConexion(it) }
            }
        }
    }
}
