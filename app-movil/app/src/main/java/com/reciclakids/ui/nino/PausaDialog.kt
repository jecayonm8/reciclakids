package com.reciclakids.ui.nino

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reciclakids.R
import com.reciclakids.model.NivelesVolumen
import com.reciclakids.ui.common.BotonNino
import com.reciclakids.ui.common.VarianteBotonNino
import com.reciclakids.ui.theme.BalooDos
import com.reciclakids.ui.theme.EscalaFijaNino
import com.reciclakids.ui.theme.ReciclaKidsColors
import com.reciclakids.ui.theme.ReciclaKidsTheme

/**
 * UI-14 Pausa. Interrumpe sin perder nada: continuar, reiniciar el reto, volver al menú y
 * ajustar el volumen. Se dibuja sobre el juego, que queda atenuado detrás.
 */
@Composable
fun PausaDialog(
    volumen: Int,
    onVolumen: (Int) -> Unit,
    onContinuar: () -> Unit,
    onReiniciar: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EscalaFijaNino {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0x8C04323F))
                // Bloquea los toques al juego que queda detrás.
                .clickable(remember { MutableInteractionSource() }, indication = null, onClick = {})
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(32.dp))
                    .background(Color.White, RoundedCornerShape(32.dp))
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text(
                    stringResource(R.string.juego_pausa),
                    modifier = Modifier.semantics { heading() },
                    fontFamily = BalooDos,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 38.sp,
                    color = ReciclaKidsColors.tintaNino,
                )
                BotonNino(
                    texto = stringResource(R.string.pausa_continuar),
                    onClick = onContinuar,
                    modifier = Modifier.fillMaxWidth(),
                    icono = painterResource(R.drawable.ic_play),
                    variante = VarianteBotonNino.Confirmar,
                    alto = 96.dp,
                    forma = RoundedCornerShape(28.dp),
                    estiloTexto = BotonNinoGrande.copy(fontSize = 28.sp, fontWeight = FontWeight.Bold),
                    tamanoIcono = 42.dp,
                )
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BotonContorno(stringResource(R.string.pausa_reiniciar), painterResource(R.drawable.ic_reiniciar), onReiniciar)
                    BotonContorno(stringResource(R.string.pausa_menu), painterResource(R.drawable.ic_casa), onMenu)
                }
                ControlVolumen(volumen, onVolumen, tamanoBoton = 64.dp, modifier = Modifier.background(Color(0xFFEAF6FA), CircleShape).padding(10.dp))
            }
        }
    }
}

@Composable
private fun RowScope.BotonContorno(texto: String, icono: Painter, onClick: () -> Unit) {
    val forma = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .weight(1f)
            .height(86.dp)
            .clip(forma)
            .background(Color.White)
            .border(3.dp, MaterialTheme.colorScheme.primary, forma)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        Icon(icono, null, Modifier.size(34.dp), tint = MaterialTheme.colorScheme.primary)
        Text(texto, fontFamily = BalooDos, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = ReciclaKidsColors.tintaNino)
    }
}

/**
 * Volumen con botones − y + y bloques que se llenan de izquierda a derecha, sin números.
 * Se comparte con los ajustes del niño (UI-18).
 */
@Composable
fun ControlVolumen(
    nivel: Int,
    onNivel: (Int) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    tamanoBoton: Dp = 72.dp,
    fondoBoton: Color = Color.White,
) {
    val descripcion = stringResource(R.string.volumen_descripcion, nivel, NivelesVolumen)
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BotonVolumen("–", stringResource(R.string.volumen_bajar), tamanoBoton, fondoBoton) { onNivel((nivel - 1).coerceAtLeast(0)) }
        Row(
            Modifier.weight(1f).clearAndSetSemantics { contentDescription = descripcion },
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(NivelesVolumen) { i ->
                Box(
                    Modifier
                        .weight(1f)
                        .defaultMinSize(minWidth = 24.dp)
                        .height(40.dp)
                        .background(if (i < nivel) color else Color(0xFFDCE7EB), RoundedCornerShape(10.dp))
                )
            }
        }
        BotonVolumen("+", stringResource(R.string.volumen_subir), tamanoBoton, fondoBoton) { onNivel((nivel + 1).coerceAtMost(NivelesVolumen)) }
    }
}

@Composable
private fun BotonVolumen(simbolo: String, descripcion: String, tamano: Dp, fondo: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(tamano)
            .clip(CircleShape)
            .background(fondo)
            .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .clearAndSetSemantics { contentDescription = descripcion },
        contentAlignment = Alignment.Center,
    ) {
        Text(simbolo, fontFamily = BalooDos, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, color = MaterialTheme.colorScheme.primary)
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun PausaDialogPreview() {
    ReciclaKidsTheme { PausaDialog(volumen = 4, onVolumen = {}, onContinuar = {}, onReiniciar = {}, onMenu = {}) }
}
