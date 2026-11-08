package com.reciclakids.ui.nino

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reciclakids.R
import com.reciclakids.ui.common.BotonCircularNino
import com.reciclakids.ui.common.BotonNino
import com.reciclakids.ui.common.FondoSubmarino
import com.reciclakids.ui.common.VarianteBotonNino
import com.reciclakids.ui.common.flotar
import com.reciclakids.ui.theme.BalooDos
import com.reciclakids.ui.theme.EscalaFijaNino
import com.reciclakids.ui.theme.ReciclaKidsColors
import com.reciclakids.ui.theme.ReciclaKidsTheme

/**
 * Al tocar un reto que ya terminó hoy. Antes entraba directo al tablero y la repetición contaba
 * otra vez (hallazgo 9 de la auditoría); ahora se avisa y se ofrece practicar: la práctica no se
 * guarda, no suma al acuario ni da insignias.
 */
@Composable
fun RetoCompletadoScreen(
    onPracticar: () -> Unit,
    onVolver: () -> Unit,
    onRepetirVoz: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EscalaFijaNino {
        FondoSubmarino(agua = ReciclaKidsColors.aguaJuego, altoArena = 90.dp, modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
            ) {
                Box(
                    Modifier.flotar(periodoMs = 4000).size(120.dp).background(Color.White.copy(alpha = 0.9f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(R.drawable.ic_estrella), contentDescription = null, modifier = Modifier.size(72.dp), tint = Color(0xFFF2B531))
                }
                Text(
                    stringResource(R.string.reto_completado_titulo),
                    modifier = Modifier.semantics { heading() },
                    fontFamily = BalooDos,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 34.sp,
                    lineHeight = 40.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Text(
                    stringResource(R.string.reto_completado_texto),
                    fontFamily = BalooDos,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    lineHeight = 26.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                BotonNino(
                    texto = stringResource(R.string.reto_completado_practicar),
                    onClick = onPracticar,
                    modifier = Modifier.fillMaxWidth(),
                    icono = painterResource(R.drawable.ic_play),
                    alto = 96.dp,
                    forma = RoundedCornerShape(32.dp),
                )
                BotonNino(
                    texto = stringResource(R.string.reto_completado_volver),
                    onClick = onVolver,
                    modifier = Modifier.fillMaxWidth(),
                    icono = painterResource(R.drawable.ic_casa),
                    variante = VarianteBotonNino.Secundario,
                    alto = 84.dp,
                    forma = RoundedCornerShape(28.dp),
                )
                BotonCircularNino(
                    icono = painterResource(R.drawable.ic_altavoz),
                    descripcion = stringResource(R.string.nino_repetir_voz),
                    onClick = onRepetirVoz,
                    tinta = ReciclaKidsColors.tintaNino,
                )
            }
        }
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun RetoCompletadoScreenPreview() {
    ReciclaKidsTheme { RetoCompletadoScreen(onPracticar = {}, onVolver = {}, onRepetirVoz = {}) }
}
