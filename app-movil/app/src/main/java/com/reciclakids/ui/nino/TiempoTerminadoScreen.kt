package com.reciclakids.ui.nino

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reciclakids.R
import com.reciclakids.ui.common.Alga
import com.reciclakids.ui.common.BotonCircularNino
import com.reciclakids.ui.common.EstiloMarcador
import com.reciclakids.ui.common.FondoSubmarino
import com.reciclakids.ui.common.MarcadorIlustracion
import com.reciclakids.ui.common.flotar
import com.reciclakids.ui.theme.BalooDos
import com.reciclakids.ui.theme.EscalaFijaNino
import com.reciclakids.ui.theme.ReciclaKidsColors
import com.reciclakids.ui.theme.ReciclaKidsTheme

private val AguaNocturna = Brush.verticalGradient(listOf(Color(0xFF2C6C86), Color(0xFF0A3A4B)))
private val AlgaDormida = Color(0xFF1F6B47)

/**
 * UI-19 Tiempo de juego terminado. Lo impone el control parental y no se puede saltar desde el
 * Modo Niño: cierra la sesión con una razón amable, no con un bloqueo. Solo repite el audio.
 */
@Composable
fun TiempoTerminadoScreen(minutos: Int, onRepetirVoz: () -> Unit, modifier: Modifier = Modifier) {
    EscalaFijaNino {
        FondoSubmarino(
            agua = AguaNocturna,
            altoArena = 90.dp,
            colorArena = ReciclaKidsColors.arenaSucia,
            modifier = modifier.fillMaxSize(),
            decoracion = {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    Alga(110.dp, Modifier.offset(maxWidth * 0.08f, maxHeight - 190.dp), ancho = 20.dp, color = AlgaDormida)
                    Alga(86.dp, Modifier.offset(maxWidth * 0.90f - 16.dp, maxHeight - 166.dp), ancho = 16.dp, color = AlgaDormida)
                }
            },
        ) {
            Column(
                modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterVertically),
            ) {
                MarcadorIlustracion(
                    "peces\ndormidos",
                    Modifier.flotar(periodoMs = 5000).size(200.dp),
                    estilo = EstiloMarcador.Noche,
                )
                Text(
                    stringResource(R.string.tiempo_titulo),
                    modifier = Modifier.semantics { heading() },
                    fontFamily = BalooDos,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 38.sp,
                    lineHeight = 44.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Row(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.9f), CircleShape)
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(painterResource(R.drawable.ic_reloj), null, Modifier.size(26.dp), tint = ReciclaKidsColors.tintaNino)
                    BasicText(
                        pluralStringResource(R.plurals.tiempo_jugado, minutos, minutos),
                        style = TextStyle(fontFamily = BalooDos, fontWeight = FontWeight.Bold, color = ReciclaKidsColors.tintaNino),
                        maxLines = 1,
                        autoSize = TextAutoSize.StepBased(minFontSize = 15.sp, maxFontSize = 20.sp),
                    )
                }
                BotonCircularNino(
                    icono = painterResource(R.drawable.ic_altavoz),
                    descripcion = stringResource(R.string.nino_repetir_voz),
                    onClick = onRepetirVoz,
                    tamano = 84.dp,
                    tamanoIcono = 40.dp,
                    tinta = ReciclaKidsColors.tintaNino,
                )
                Text(
                    stringResource(R.string.tiempo_nota),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = Color.White.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun TiempoTerminadoScreenPreview() {
    ReciclaKidsTheme { TiempoTerminadoScreen(minutos = 20, onRepetirVoz = {}) }
}
