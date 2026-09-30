package com.reciclakids.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reciclakids.R
import com.reciclakids.ui.theme.BalooDos
import com.reciclakids.ui.theme.ReciclaKidsColors
import com.reciclakids.ui.theme.ReciclaKidsTheme

/** Aciertos seguidos a partir de los cuales la racha da bonificación doble (+20 en vez de +10). */
const val RachaConBonificacion = 3

/**
 * Burbuja con los aciertos seguidos. Desde [RachaConBonificacion] pasa a turquesa y muestra «×2».
 * Un error deja la racha quieta: el número solo crece, nunca se borra.
 */
@Composable
fun IndicadorRacha(racha: Int, modifier: Modifier = Modifier) {
    val conBonificacion = racha >= RachaConBonificacion
    val escala = remember { Animatable(1f) }
    LaunchedEffect(conBonificacion) { if (conBonificacion) escala.rebotar() }
    val descripcion = stringResource(
        if (conBonificacion) R.string.racha_doble_descripcion else R.string.racha_descripcion,
        racha,
    )

    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = escala.value
                scaleY = escala.value
            }
            .height(56.dp)
            .background(ReciclaKidsColors.panelNino, CircleShape)
            .padding(start = 6.dp, end = 14.dp)
            .clearAndSetSemantics { contentDescription = descripcion },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    if (conBonificacion) ReciclaKidsColors.halo else MaterialTheme.colorScheme.tertiaryContainer,
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = racha.toString(),
                fontFamily = BalooDos,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                color = if (conBonificacion) ReciclaKidsColors.onHalo else MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
        Text(
            text = stringResource(if (conBonificacion) R.string.racha_doble else R.string.racha),
            fontFamily = BalooDos,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = MaterialTheme.colorScheme.tertiary,
        )
    }
}

@Preview
@Composable
private fun IndicadorRachaPreview() {
    ReciclaKidsTheme {
        FondoSubmarino(agua = ReciclaKidsColors.aguaJuego) {
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IndicadorRacha(racha = 1)
                IndicadorRacha(racha = 3)
            }
        }
    }
}
