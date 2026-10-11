package com.reciclakids.ui.nino

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reciclakids.R
import com.reciclakids.ui.common.CasillasCodigo
import com.reciclakids.ui.common.EstadoCodigo
import com.reciclakids.ui.common.FondoSubmarino
import com.reciclakids.ui.common.MarcadorIlustracion
import com.reciclakids.ui.common.TecladoNumericoNino
import com.reciclakids.ui.common.flotar
import com.reciclakids.ui.theme.EscalaFijaNino
import com.reciclakids.ui.theme.Nunito
import com.reciclakids.ui.theme.ReciclaKidsColors
import com.reciclakids.ui.theme.ReciclaKidsTheme

internal val AguaCodigo = Brush.verticalGradient(listOf(Color(0xFF8CE8F2), Color(0xFF1A93B6)))

/** Lo que dice el pez guía en cada estado del código. Se muestra y se dice en voz alta. */
fun EstadoCodigo.mensaje(): Int = when (this) {
    EstadoCodigo.Editando -> R.string.codigo_mensaje_editando
    EstadoCodigo.Correcto -> R.string.codigo_mensaje_correcto
    EstadoCodigo.Invalido -> R.string.codigo_mensaje_invalido
    EstadoCodigo.Vencido -> R.string.codigo_mensaje_vencido
}

/**
 * UI-08 Código del reto. El niño entra sin saber leer: empareja el color y la forma de cada
 * dígito con los que la docente muestra en el salón (UI-24).
 */
@Composable
fun CodigoRetoScreen(
    codigo: String,
    estado: EstadoCodigo,
    onDigito: (Char) -> Unit,
    onBorrar: () -> Unit,
    onConfirmar: () -> Unit,
    onRepetirVoz: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EscalaFijaNino {
        FondoSubmarino(agua = AguaCodigo, altoArena = 60.dp, modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MarcadorIlustracion("pez\nguía", Modifier.flotar().size(84.dp))
                    Text(
                        text = stringResource(estado.mensaje()),
                        modifier = Modifier
                            .weight(1f)
                            .background(ReciclaKidsColors.panelNino, RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .semantics { liveRegion = LiveRegionMode.Polite },
                        fontFamily = Nunito,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        lineHeight = 22.sp,
                        color = ReciclaKidsColors.tintaNino,
                    )
                    BotonRepetirVoz(onRepetirVoz)
                }
                CasillasCodigo(codigo = codigo, estado = estado)
                TecladoNumericoNino(onDigito = onDigito, onBorrar = onBorrar, onConfirmar = onConfirmar)
                Spacer(Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.codigo_pista),
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.8f), CircleShape)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    fontFamily = Nunito,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    color = ReciclaKidsColors.tintaSobreAgua,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun CodigoRetoScreenPreview() {
    ReciclaKidsTheme {
        CodigoRetoScreen("47", EstadoCodigo.Editando, onDigito = {}, onBorrar = {}, onConfirmar = {}, onRepetirVoz = {})
    }
}
