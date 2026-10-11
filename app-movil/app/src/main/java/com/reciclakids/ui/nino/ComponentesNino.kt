package com.reciclakids.ui.nino

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.reciclakids.R
import com.reciclakids.ui.common.BotonCircularNino
import com.reciclakids.ui.theme.Tactil

/** Botón de repetir la locución: cada pantalla del niño tiene uno. */
@Composable
internal fun BotonRepetirVoz(onClick: () -> Unit, modifier: Modifier = Modifier, tamano: Dp = Tactil.minimoNino) {
    BotonCircularNino(
        icono = painterResource(R.drawable.ic_altavoz),
        descripcion = stringResource(R.string.nino_repetir_voz),
        onClick = onClick,
        modifier = modifier,
        tamano = tamano,
        tamanoIcono = tamano * 34 / 72,
        conBorde = true,
    )
}
