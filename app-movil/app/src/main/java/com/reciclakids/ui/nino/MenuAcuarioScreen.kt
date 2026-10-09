package com.reciclakids.ui.nino

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reciclakids.R
import com.reciclakids.ui.common.AcuarioEstado
import com.reciclakids.ui.common.BotonNino
import com.reciclakids.ui.common.EstiloMarcador
import com.reciclakids.ui.common.MarcadorIlustracion
import com.reciclakids.ui.common.VarianteBotonNino
import com.reciclakids.ui.common.flotar
import com.reciclakids.ui.theme.BalooDos
import com.reciclakids.ui.theme.EscalaFijaNino
import com.reciclakids.ui.theme.ReciclaKidsColors
import com.reciclakids.ui.theme.ReciclaKidsTheme

/** Qué personajes ya viven en el acuario según su nivel. Marcadores hasta tener las ilustraciones. */
private fun personajesDelNivel(nivel: Int) = when (nivel) {
    1 -> "agua turbia\nsin vida"
    2 -> "pez\naparece"
    3 -> "pez y\ntortuga"
    else -> "pez, tortuga\ny pulpo"
}

/**
 * UI-10 Menú · acuario. El progreso se muestra como el estado del mundo, sin números:
 * el acuario se limpia a medida que el niño completa retos.
 */
@Composable
fun MenuAcuarioScreen(
    nombre: String,
    nivel: Int,
    retoPendiente: Boolean,
    hayConexion: Boolean,
    onRetoDiario: () -> Unit,
    onContinuar: () -> Unit,
    onInsignias: () -> Unit,
    onAjustes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EscalaFijaNino {
        AcuarioEstado(nivel = nivel, modifier = modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .background(ReciclaKidsColors.panelNino, CircleShape)
                            .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        MarcadorIlustracion("avatar", Modifier.size(52.dp), estilo = EstiloMarcador.Avatar)
                        Text(
                            text = nombre,
                            fontFamily = BalooDos,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = ReciclaKidsColors.tintaNino,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    PastillaConexion(hayConexion)
                }
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    MarcadorIlustracion(personajesDelNivel(nivel), Modifier.flotar().size(150.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box {
                        BotonNino(
                            onClick = onRetoDiario,
                            modifier = Modifier.fillMaxWidth(),
                            alto = 120.dp,
                            forma = RoundedCornerShape(32.dp),
                            profundidad = 10.dp,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Icon(painterResource(R.drawable.ic_estrella), null, Modifier.size(52.dp), tint = ReciclaKidsColors.onBotonJugar)
                                Text(
                                    stringResource(R.string.menu_reto_diario),
                                    fontFamily = BalooDos,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 32.sp,
                                )
                            }
                        }
                        if (retoPendiente) InsigniaRetoPendiente(Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-12).dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        BotonMenu(
                            texto = stringResource(R.string.menu_continuar),
                            icono = painterResource(R.drawable.ic_play),
                            tinta = MaterialTheme.colorScheme.primary,
                            onClick = onContinuar,
                            modifier = Modifier.weight(1f),
                        )
                        BotonMenu(
                            texto = stringResource(R.string.menu_insignias),
                            icono = painterResource(R.drawable.ic_insignia_dorada),
                            tinta = null,
                            onClick = onInsignias,
                            modifier = Modifier.weight(1f),
                        )
                        val ajustes = stringResource(R.string.menu_ajustes)
                        BotonNino(
                            onClick = onAjustes,
                            modifier = Modifier.width(96.dp).clearAndSetSemantics { contentDescription = ajustes },
                            variante = VarianteBotonNino.Secundario,
                            alto = 96.dp,
                            forma = RoundedCornerShape(28.dp),
                        ) {
                            Icon(painterResource(R.drawable.ic_ajustes), null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BotonMenu(texto: String, icono: Painter, tinta: Color?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    BotonNino(
        onClick = onClick,
        modifier = modifier,
        variante = VarianteBotonNino.Secundario,
        alto = 96.dp,
        forma = RoundedCornerShape(28.dp),
        rellenoHorizontal = 4.dp,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (tinta != null) {
                Icon(icono, null, Modifier.size(38.dp), tint = tinta)
            } else {
                Image(icono, null, Modifier.size(38.dp))
            }
            // Se encoge en teléfonos de 320 dp para que «Continuar» quepa en una línea.
            BasicText(
                text = texto,
                style = TextStyle(fontFamily = BalooDos, fontWeight = FontWeight.Bold, lineHeight = 24.sp, color = LocalContentColor.current),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 16.sp, maxFontSize = 22.sp),
            )
        }
    }
}

/** Burbuja roja con el número de retos nuevos; rebota para llamar la atención sin texto. */
@Composable
private fun InsigniaRetoPendiente(modifier: Modifier = Modifier) {
    val descripcion = stringResource(R.string.menu_reto_pendiente)
    val transicion = rememberInfiniteTransition(label = "retoPendiente")
    val escala by transicion.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 1800
                1f at 0
                1.18f at 630
                0.94f at 1080
                1f at 1800
            },
            RepeatMode.Restart,
        ),
        label = "retoPendiente",
    )
    val sombra = Color(0xFFA83F22)
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
            }
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .drawBehind { drawCircle(sombra, center = center + Offset(0f, 4.dp.toPx())) }
            .background(Color(0xFFE2603C), CircleShape)
            .clearAndSetSemantics { contentDescription = descripcion },
        contentAlignment = Alignment.Center,
    ) {
        Text("1", fontFamily = BalooDos, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, color = Color.White)
    }
}

@Composable
private fun PastillaConexion(hayConexion: Boolean) {
    val descripcion = stringResource(if (hayConexion) R.string.conexion_conectado else R.string.conexion_guardado_aqui)
    Box(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.9f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clearAndSetSemantics { contentDescription = descripcion },
    ) {
        Icon(
            painterResource(if (hayConexion) R.drawable.ic_wifi else R.drawable.ic_wifi_off),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun MenuAcuarioScreenPreview() {
    ReciclaKidsTheme {
        MenuAcuarioScreen("Salomé M.", nivel = 2, retoPendiente = true, hayConexion = false, {}, {}, {}, {})
    }
}
