package com.reciclakids.ui.nino

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.reciclakids.model.Insignia
import com.reciclakids.ui.common.BotonNino
import com.reciclakids.ui.common.Burbuja
import com.reciclakids.ui.common.EstiloMarcador
import com.reciclakids.ui.common.MarcadorIlustracion
import com.reciclakids.ui.common.VarianteBotonNino
import com.reciclakids.ui.common.sombraTitular
import com.reciclakids.ui.theme.BalooDos
import com.reciclakids.ui.theme.EscalaFijaNino
import com.reciclakids.ui.theme.Nunito
import com.reciclakids.ui.theme.ReciclaKidsColors
import com.reciclakids.ui.theme.ReciclaKidsTheme
import kotlin.math.hypot

/** Luz que sale de la medalla: degradado radial centrado al 40 % del alto, hasta la esquina más lejana. */
private fun Modifier.fondoInsignia(): Modifier = drawBehind {
    val centro = Offset(size.width / 2, size.height * 0.4f)
    val radio = hypot(maxOf(centro.x, size.width - centro.x), maxOf(centro.y, size.height - centro.y))
    drawRect(
        Brush.radialGradient(
            0f to Color(0xFFB5F1F8),
            0.55f to Color(0xFF17A2C4),
            1f to Color(0xFF0A4A5E),
            center = centro,
            radius = radio,
        )
    )
}

/** UI-16 Insignia obtenida. Convierte el esfuerzo en un objeto que se colecciona. */
@Composable
fun InsigniaObtenidaScreen(
    insignia: Insignia,
    onSeguir: () -> Unit,
    onMisInsignias: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val transicion = rememberInfiniteTransition(label = "brillo")
    val brillo by transicion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = EaseInOut), RepeatMode.Reverse),
        label = "brillo",
    )
    val entrada = remember(insignia) { Animatable(0f) }
    LaunchedEffect(insignia) {
        entrada.animateTo(
            1f,
            keyframes {
                durationMillis = 700
                0f at 0
                1.2f at 420
                1f at 700
            },
        )
    }

    EscalaFijaNino {
        BoxWithConstraints(
            modifier = modifier.fillMaxSize().fondoInsignia(),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(420.dp)
                    .graphicsLayer {
                        alpha = 0.35f + 0.65f * brillo
                        scaleX = 1f + 0.06f * brillo
                        scaleY = 1f + 0.06f * brillo
                    }
                    .background(Color.White.copy(alpha = 0.28f), CircleShape)
            )
            Burbuja(Modifier.align(Alignment.BottomStart).offset(x = maxWidth * 0.18f), periodoMs = 3600)
            Burbuja(Modifier.align(Alignment.BottomEnd).offset(x = -(maxWidth * 0.22f)), tamano = 10.dp, periodoMs = 4200, retrasoMs = 1000)

            Column(
                modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
            ) {
                Text(
                    stringResource(R.string.insignia_nueva),
                    modifier = Modifier.semantics { heading() },
                    style = TextStyle(
                        fontFamily = BalooDos,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 36.sp,
                        color = Color.White,
                        shadow = sombraTitular(),
                    ),
                )
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = entrada.value
                            scaleY = entrada.value
                        }
                        .size(230.dp)
                        .shadow(12.dp, CircleShape)
                        .background(Color.White, CircleShape)
                        .border(8.dp, ReciclaKidsColors.insigniaBorde, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    MarcadorIlustracion("ilustración\n${insignia.nombre.lowercase()}", Modifier.size(170.dp), estilo = EstiloMarcador.Insignia)
                }
                Text(
                    insignia.nombre,
                    style = TextStyle(
                        fontFamily = BalooDos,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 42.sp,
                        lineHeight = 46.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        shadow = sombraTitular(),
                    ),
                )
                Text(
                    insignia.logro,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.9f), CircleShape)
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    fontFamily = Nunito,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = ReciclaKidsColors.tintaSobreAgua,
                    textAlign = TextAlign.Center,
                )
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    BotonNino(
                        texto = stringResource(R.string.insignia_seguir),
                        onClick = onSeguir,
                        modifier = Modifier.fillMaxWidth(),
                        variante = VarianteBotonNino.Confirmar,
                        estiloTexto = BotonNinoGrande.copy(fontSize = 28.sp, fontWeight = FontWeight.Bold),
                    )
                    BotonNino(
                        texto = stringResource(R.string.insignia_mis_insignias),
                        onClick = onMisInsignias,
                        modifier = Modifier.fillMaxWidth(),
                        variante = VarianteBotonNino.Secundario,
                        alto = 72.dp,
                        estiloTexto = BotonNinoGrande.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                    )
                }
            }
        }
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun InsigniaObtenidaScreenPreview() {
    ReciclaKidsTheme { InsigniaObtenidaScreen(Insignia.AmigaTortuga, onSeguir = {}, onMisInsignias = {}) }
}
