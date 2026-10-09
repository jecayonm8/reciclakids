package com.reciclakids.ui.nino

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reciclakids.R
import com.reciclakids.model.Insignia
import com.reciclakids.model.ResultadoReto
import com.reciclakids.ui.common.Alga
import com.reciclakids.ui.common.BotonNino
import com.reciclakids.ui.common.Burbuja
import com.reciclakids.ui.common.EstadoConexion
import com.reciclakids.ui.common.FondoSubmarino
import com.reciclakids.ui.common.IndicadorSinConexion
import com.reciclakids.ui.common.VarianteBotonNino
import com.reciclakids.ui.common.sombraTitular
import com.reciclakids.ui.theme.BalooDos
import com.reciclakids.ui.theme.EscalaFijaNino
import com.reciclakids.ui.theme.ReciclaKidsColors
import com.reciclakids.ui.theme.ReciclaKidsTheme

private val AguaResultado = Brush.verticalGradient(listOf(Color(0xFF8CE8F2), Color(0xFF17A2C4)))

/** Separación entre la entrada de cada estrella. */
private const val EscalonEstrellasMs = 150

/**
 * UI-15 Resultado. Todo reto termina en logro: se celebra lo hecho, no lo fallado. Los errores
 * solo llegan al reporte de la docente, nunca al niño.
 */
@Composable
fun ResultadoScreen(
    resultado: ResultadoReto,
    onVerPremio: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EscalaFijaNino {
        FondoSubmarino(
            agua = AguaResultado,
            altoArena = 100.dp,
            modifier = modifier.fillMaxSize(),
            decoracion = {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    Alga(120.dp, Modifier.offset(maxWidth * 0.06f, maxHeight - 210.dp))
                    Alga(96.dp, Modifier.offset(maxWidth * 0.92f - 18.dp, maxHeight - 186.dp), ancho = 18.dp)
                    Burbuja(Modifier.offset(maxWidth * 0.28f, maxHeight - 114.dp), periodoMs = 3400)
                }
            },
        ) {
            Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
                ) {
                    Estrellas(resultado.estrellas)
                    Text(
                        text = pluralStringResource(R.plurals.resultado_titulo, resultado.residuosSeparados, resultado.residuosSeparados),
                        style = TextStyle(
                            fontFamily = BalooDos,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 40.sp,
                            lineHeight = 46.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            shadow = sombraTitular(),
                        ),
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Pastilla(
                            stringResource(if (resultado.sumoAlAcuario) R.string.resultado_acuario else R.string.resultado_buen_trabajo),
                            ReciclaKidsColors.tintaNino,
                        )
                        if (resultado.rachaMaxima >= 2) {
                            Pastilla(stringResource(R.string.resultado_racha, resultado.rachaMaxima), ReciclaKidsColors.botonConfirmar)
                        }
                    }
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        if (resultado.insigniasNuevas.isNotEmpty()) {
                            BotonNino(
                                texto = stringResource(R.string.resultado_ver_premio),
                                onClick = onVerPremio,
                                modifier = Modifier.fillMaxWidth(),
                                icono = painterResource(R.drawable.ic_insignia),
                                alto = 96.dp,
                                estiloTexto = BotonNinoGrande.copy(fontSize = 30.sp),
                            )
                        }
                        BotonNino(
                            texto = stringResource(R.string.resultado_menu),
                            onClick = onMenu,
                            modifier = Modifier.fillMaxWidth(),
                            icono = painterResource(R.drawable.ic_casa),
                            variante = VarianteBotonNino.Secundario,
                            estiloTexto = BotonNinoGrande.copy(fontSize = 26.sp, fontWeight = FontWeight.Bold),
                            tamanoIcono = 36.dp,
                        )
                    }
                }
                IndicadorSinConexion(EstadoConexion.Sincronizado, Modifier.align(Alignment.BottomEnd).padding(14.dp))
            }
        }
    }
}

@Composable
private fun Estrellas(ganadas: Int) {
    val descripcion = stringResource(R.string.resultado_estrellas, ganadas)
    Row(
        modifier = Modifier.clearAndSetSemantics { contentDescription = descripcion },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        listOf(74.dp, 104.dp, 74.dp).forEachIndexed { i, tamano ->
            Estrella(encendida = i < ganadas, tamano = tamano, retrasoMs = i * EscalonEstrellasMs)
        }
    }
}

/** Entra girando y crece de más antes de asentarse, una tras otra. */
@Composable
private fun Estrella(encendida: Boolean, tamano: Dp, retrasoMs: Int) {
    val escala = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        escala.animateTo(
            1f,
            keyframes {
                durationMillis = 600 + retrasoMs
                0f at retrasoMs
                1.2f at retrasoMs + 360
                1f at durationMillis
            },
        )
    }
    Image(
        painter = painterResource(if (encendida) R.drawable.ic_estrella_dorada else R.drawable.ic_estrella_apagada),
        contentDescription = null,
        modifier = Modifier
            .size(tamano)
            .graphicsLayer {
                scaleX = escala.value
                scaleY = escala.value
                rotationZ = -40f * (1f - escala.value.coerceAtMost(1f))
            },
    )
}

@Composable
private fun Pastilla(texto: String, color: Color) {
    Text(
        text = texto,
        modifier = Modifier
            .background(ReciclaKidsColors.panelNino, CircleShape)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        fontFamily = BalooDos,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        color = color,
    )
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun ResultadoScreenPreview() {
    ReciclaKidsTheme {
        ResultadoScreen(
            resultado = ResultadoReto(8, errores = 1, rachaMaxima = 5, sumoAlAcuario = true, insigniasNuevas = listOf(Insignia.RachaDeCinco)),
            onVerPremio = {},
            onMenu = {},
        )
    }
}
