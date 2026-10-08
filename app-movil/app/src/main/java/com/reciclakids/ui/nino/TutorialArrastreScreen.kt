package com.reciclakids.ui.nino

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reciclakids.R
import com.reciclakids.ui.common.BotonNino
import com.reciclakids.ui.common.Caneca
import com.reciclakids.ui.common.EstadoCaneca
import com.reciclakids.ui.common.EstiloMarcador
import com.reciclakids.ui.common.FondoSubmarino
import com.reciclakids.ui.common.MarcadorIlustracion
import com.reciclakids.ui.common.TipoCaneca
import com.reciclakids.ui.common.VarianteBotonNino
import com.reciclakids.ui.theme.BalooDos
import com.reciclakids.ui.theme.EscalaFijaNino
import com.reciclakids.ui.theme.ReciclaKidsColors
import com.reciclakids.ui.theme.ReciclaKidsTheme

/** Ciclo de la demostración: la mano baja el residuo hasta la caneca cada 4 s. */
private const val CicloTutorialMs = 4000

/** Recorrido vertical de la mano y el residuo, de arriba hasta la caneca. */
private val Recorrido = 250.dp

/**
 * UI-11 Tutorial de primera vez. Enseña el gesto de arrastrar sin una sola palabra escrita
 * que haga falta leer: una mano baja el residuo hasta su caneca, en bucle.
 */
@Composable
fun TutorialArrastreScreen(onEntendido: () -> Unit, onRepetirVoz: () -> Unit, modifier: Modifier = Modifier) {
    val transicion = rememberInfiniteTransition(label = "tutorial")
    val descenso by transicion.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = CicloTutorialMs
                0f at 0
                0f at 480
                1f at 2200
                1f at 2840
                0f at CicloTutorialMs
            },
            RepeatMode.Restart,
        ),
        label = "descenso",
    )
    val opacidadResiduo by transicion.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = CicloTutorialMs
                1f at 2200
                0f at 2800
                0f at 2840
                1f at CicloTutorialMs
            },
            RepeatMode.Restart,
        ),
        label = "opacidadResiduo",
    )
    val presion by transicion.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = CicloTutorialMs
                1f at 0
                0.88f at 480
                0.88f at 2200
                1f at 2800
                1f at CicloTutorialMs
            },
            RepeatMode.Restart,
        ),
        label = "presion",
    )
    val opacidadMano by transicion.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = CicloTutorialMs
                1f at 2800
                0.25f at CicloTutorialMs
            },
            RepeatMode.Restart,
        ),
        label = "opacidadMano",
    )

    EscalaFijaNino {
        FondoSubmarino(agua = AguaJuegoClara, altoArena = 70.dp, modifier = modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.93f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.tutorial_titulo),
                        modifier = Modifier.weight(1f).semantics { heading() },
                        fontFamily = BalooDos,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 26.sp,
                        lineHeight = 30.sp,
                        color = ReciclaKidsColors.tintaNino,
                    )
                    BotonRepetirVoz(onRepetirVoz, tamano = 64.dp)
                }
                Box(Modifier.weight(1f).fillMaxWidth().clearAndSetSemantics { }) {
                    MarcadorIlustracion(
                        etiqueta = "botella\nplástica",
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 20.dp)
                            .graphicsLayer {
                                translationY = Recorrido.toPx() * descenso
                                alpha = opacidadResiduo
                            }
                            .size(120.dp)
                            .border(3.dp, Color.White, RoundedCornerShape(28.dp)),
                        forma = RoundedCornerShape(28.dp),
                        estilo = EstiloMarcador.Residuo,
                    )
                    MarcadorIlustracion(
                        etiqueta = "mano\ngesto",
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 34.dp)
                            .graphicsLayer {
                                translationY = Recorrido.toPx() * descenso
                                scaleX = presion
                                scaleY = presion
                                alpha = opacidadMano
                            }
                            .size(92.dp)
                            .background(Color.White.copy(alpha = 0.55f), CircleShape)
                            .border(4.dp, Color.White, CircleShape),
                    )
                    Caneca(
                        tipo = TipoCaneca.Blanca,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp),
                        estado = EstadoCaneca.Brillante,
                        ancho = 120.dp,
                    )
                }
                BotonNino(
                    texto = stringResource(R.string.tutorial_entendido),
                    onClick = onEntendido,
                    modifier = Modifier.fillMaxWidth(),
                    icono = painterResource(R.drawable.ic_check),
                    variante = VarianteBotonNino.Confirmar,
                    estiloTexto = BotonNinoGrande.copy(fontSize = 30.sp),
                    tamanoIcono = 36.dp,
                    iconoAlFinal = true,
                )
            }
        }
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun TutorialArrastreScreenPreview() {
    ReciclaKidsTheme { TutorialArrastreScreen(onEntendido = {}, onRepetirVoz = {}) }
}
