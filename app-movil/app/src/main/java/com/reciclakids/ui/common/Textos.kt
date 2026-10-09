package com.reciclakids.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Sombra dura bajo los titulares blancos que van directo sobre el agua. */
@Composable
fun sombraTitular(desplazamiento: Dp = 4.dp): Shadow {
    val px = with(LocalDensity.current) { desplazamiento.toPx() }
    return Shadow(color = Color(0x5904323F), offset = Offset(0f, px), blurRadius = 0.5f)
}
