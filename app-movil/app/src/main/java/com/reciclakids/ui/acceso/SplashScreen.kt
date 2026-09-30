package com.reciclakids.ui.acceso

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reciclakids.R
import com.reciclakids.ui.juego.Burbuja
import com.reciclakids.ui.juego.FondoSubmarino
import com.reciclakids.ui.juego.MarcadorIlustracion
import com.reciclakids.ui.juego.flotar
import com.reciclakids.ui.theme.BalooDos
import com.reciclakids.ui.theme.EscalaFijaNino
import com.reciclakids.ui.theme.Nunito
import com.reciclakids.ui.theme.ReciclaKidsColors
import com.reciclakids.ui.theme.ReciclaKidsTheme
import kotlinx.coroutines.delay

private val AguaSplash = Brush.verticalGradient(
    0f to Color(0xFF8CE8F2),
    0.55f to Color(0xFF17A2C4),
    1f to Color(0xFF0C5C73),
)

/** Sombra dura bajo los titulares blancos que van directo sobre el agua. */
@Composable
internal fun sombraTitular(desplazamiento: Dp = 4.dp): Shadow {
    val px = with(LocalDensity.current) { desplazamiento.toPx() }
    return Shadow(color = Color(0x5904323F), offset = Offset(0f, px), blurRadius = 0.5f)
}

/**
 * UI-01 Bienvenida. Da tiempo de carga sin pantalla muerta y fija el mundo submarino.
 * Avanza sola tras [esperaMs]; cualquier toque salta al selector.
 */
@Composable
fun SplashScreen(onContinuar: () -> Unit, modifier: Modifier = Modifier, esperaMs: Long = 2500) {
    val continuar by rememberUpdatedState(onContinuar)
    var yaContinuo by remember { mutableStateOf(false) }
    val avanzar = {
        if (!yaContinuo) {
            yaContinuo = true
            continuar()
        }
    }
    LaunchedEffect(Unit) {
        delay(esperaMs)
        avanzar()
    }
    val descripcion = stringResource(R.string.splash_descripcion)

    EscalaFijaNino {
        FondoSubmarino(
            agua = AguaSplash,
            altoArena = 80.dp,
            modifier = modifier
                .fillMaxSize()
                .clickable(remember { MutableInteractionSource() }, indication = null, onClick = avanzar)
                .clearAndSetSemantics { contentDescription = descripcion },
            decoracion = {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    Burbuja(Modifier.offset(maxWidth * 0.14f, maxHeight - 20.dp), tamano = 20.dp, opacidad = 0.55f)
                    Burbuja(
                        Modifier.offset(maxWidth * 0.44f, maxHeight - 13.dp),
                        tamano = 13.dp, periodoMs = 5000, retrasoMs = 800, opacidad = 0.5f,
                    )
                    Burbuja(
                        Modifier.offset(maxWidth * 0.82f - 16.dp, maxHeight - 16.dp),
                        tamano = 16.dp, periodoMs = 4600, retrasoMs = 1600, opacidad = 0.5f,
                    )
                }
            },
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
                MarcadorIlustracion("logo\nReciclaKids", Modifier.flotar().size(200.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = TextStyle(
                        fontFamily = BalooDos,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 52.sp,
                        lineHeight = 56.sp,
                        color = Color.White,
                        shadow = sombraTitular(),
                    ),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(0.9f, 0.55f, 0.3f).forEach { opacidad ->
                        Box(Modifier.size(14.dp).background(Color.White.copy(alpha = opacidad), CircleShape))
                    }
                }
                Text(
                    text = stringResource(R.string.splash_cargando),
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.88f), CircleShape)
                        .padding(horizontal = 22.dp, vertical = 10.dp),
                    fontFamily = Nunito,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = ReciclaKidsColors.tintaSobreAgua,
                )
            }
        }
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun SplashScreenPreview() {
    ReciclaKidsTheme { SplashScreen(onContinuar = {}) }
}
