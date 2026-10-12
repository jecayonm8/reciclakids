package com.reciclakids.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sqrt

/** Paleta del rayado según dónde se apoya el marcador. */
enum class EstiloMarcador(val claro: Color, val oscuro: Color, val texto: Color) {
    SobreAgua(Color(0x66FFFFFF), Color(0x2EFFFFFF), Color(0xFF04323F)),
    Residuo(Color(0xFFF2F7F9), Color(0xFFFFFFFF), Color(0xFF5F6F73)),
    Avatar(Color(0xFFDCEFF3), Color(0xFFF2F9FA), Color(0xFF5F6F73)),
    Insignia(Color(0xFFFFF3D6), Color(0xFFFFE9B8), Color(0xFF7A6330)),
    Adulto(Color(0xFFE4EFF2), Color(0xFFF2F8F9), Color(0xFF5F6F73)),
    Noche(Color(0x38FFFFFF), Color(0x1AFFFFFF), Color(0xFFCFE6EC)),
}

/** Rayado diagonal de los marcadores de ilustración del prototipo. */
fun Modifier.rayado(claro: Color, oscuro: Color, paso: Dp = 10.dp): Modifier = drawBehind {
    val tramo = paso.toPx() * 2 / sqrt(2f)
    drawRect(
        Brush.linearGradient(
            0f to claro, 0.5f to claro, 0.5f to oscuro, 1f to oscuro,
            start = Offset.Zero,
            end = Offset(tramo, tramo),
            tileMode = TileMode.Repeated,
        )
    )
}

/**
 * Marcador provisional de una ilustración que todavía no entrega el ilustrador
 * (personajes, residuos, avatares, insignias, logo). Ocupa el tamaño y la posición
 * finales; se reemplaza por el asset cuando exista.
 */
@Composable
fun MarcadorIlustracion(
    etiqueta: String,
    modifier: Modifier = Modifier,
    forma: Shape = CircleShape,
    estilo: EstiloMarcador = EstiloMarcador.SobreAgua,
) {
    Box(
        modifier = modifier
            .clip(forma)
            .rayado(estilo.claro, estilo.oscuro),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = etiqueta,
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = estilo.texto,
                textAlign = TextAlign.Center,
            ),
        )
    }
}
