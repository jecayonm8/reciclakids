package com.reciclakids.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

// El tema oscuro queda pendiente (ver Sistema de Diseño 1.1); el piloto usa siempre el claro,
// sin color dinámico, para que las canecas y el acuario conserven sus colores.
@Composable
fun ReciclaKidsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ReciclaKidsLightColors,
        typography = Typography,
        content = content
    )
}

// El Modo Niño fija la escala de fuente para no romper el juego; los modos adultos
// respetan la del sistema (hasta 200 %).
@Composable
fun EscalaFijaNino(content: @Composable () -> Unit) {
    val densidad = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density = densidad.density, fontScale = 1f),
        content = content
    )
}
