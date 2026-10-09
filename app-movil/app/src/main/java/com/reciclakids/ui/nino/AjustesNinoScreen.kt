package com.reciclakids.ui.nino

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reciclakids.R
import com.reciclakids.model.AjustesNino
import com.reciclakids.ui.common.FondoSubmarino
import com.reciclakids.ui.common.sombraTitular
import com.reciclakids.ui.theme.BalooDos
import com.reciclakids.ui.theme.EscalaFijaNino
import com.reciclakids.ui.theme.ReciclaKidsColors
import com.reciclakids.ui.theme.ReciclaKidsTheme

private val TurquesaMusica = Color(0xFF16A3B8)
private val FondoBotonVolumen = Color(0xFFEAF6FA)

/**
 * UI-18 Ajustes del niño: el niño controla el sonido sin ayuda, con botones grandes y bloques
 * que se llenan. La sección de padres pasa por la puerta para adultos.
 */
@Composable
fun AjustesNinoScreen(
    ajustes: AjustesNino,
    onMusica: (Int) -> Unit,
    onSonidos: (Int) -> Unit,
    onVolver: () -> Unit,
    onSeccionPadres: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EscalaFijaNino {
        FondoSubmarino(agua = ReciclaKidsColors.aguaJuego, altoArena = 60.dp, modifier = modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BotonVolverNino(onVolver)
                    Text(
                        stringResource(R.string.ajustes_titulo),
                        modifier = Modifier.semantics { heading() },
                        style = TextStyle(
                            fontFamily = BalooDos,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 32.sp,
                            color = Color.White,
                            shadow = sombraTitular(3.dp),
                        ),
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.94f), RoundedCornerShape(28.dp))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    SeccionVolumen(
                        titulo = stringResource(R.string.ajustes_musica),
                        icono = painterResource(R.drawable.ic_musica),
                        nivel = ajustes.volumenMusica,
                        onNivel = onMusica,
                        color = TurquesaMusica,
                    )
                    Box(Modifier.fillMaxWidth().height(2.dp).background(MaterialTheme.colorScheme.surfaceContainer))
                    SeccionVolumen(
                        titulo = stringResource(R.string.ajustes_sonidos),
                        icono = painterResource(R.drawable.ic_altavoz),
                        nivel = ajustes.volumenSonidos,
                        onNivel = onSonidos,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                BotonSeccionPadres(onSeccionPadres)
            }
        }
    }
}

@Composable
private fun SeccionVolumen(titulo: String, icono: Painter, nivel: Int, onNivel: (Int) -> Unit, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icono, null, Modifier.size(42.dp), tint = MaterialTheme.colorScheme.primary)
            Text(titulo, fontFamily = BalooDos, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = ReciclaKidsColors.tintaNino)
        }
        ControlVolumen(nivel = nivel, onNivel = onNivel, color = color, fondoBoton = FondoBotonVolumen)
    }
}

/** Acceso discreto a los modos adultos, igual que «Adultos» en el selector de modo. */
@Composable
private fun BotonSeccionPadres(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.72f))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(painterResource(R.drawable.ic_candado), null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            stringResource(R.string.ajustes_seccion_padres),
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun AjustesNinoScreenPreview() {
    ReciclaKidsTheme { AjustesNinoScreen(AjustesNino(), onMusica = {}, onSonidos = {}, onVolver = {}, onSeccionPadres = {}) }
}
