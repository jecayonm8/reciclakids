package com.reciclakids.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// 1.1 Roles Material 3 (tema claro). El piloto se entrega solo en tema claro.
val ReciclaKidsLightColors = lightColorScheme(
    primary = Color(0xFF00687F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB3ECFF),
    onPrimaryContainer = Color(0xFF001F28),
    secondary = Color(0xFF4B6269),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCDE7EF),
    onSecondaryContainer = Color(0xFF061F25),
    tertiary = Color(0xFF2E6B45), // acierto / positivo
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFB6F0C6),
    onTertiaryContainer = Color(0xFF0B2612),
    error = Color(0xFFBA1A1A), // SOLO modos adultos. Nunca en Modo Niño.
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF5FAFC),
    onBackground = Color(0xFF171C1E),
    surface = Color(0xFFF5FAFC),
    onSurface = Color(0xFF171C1E),
    surfaceVariant = Color(0xFFDBE4E7),
    onSurfaceVariant = Color(0xFF3F484B),
    outline = Color(0xFF6F797B),
    outlineVariant = Color(0xFFC3D3D8),
    surfaceContainer = Color(0xFFE4EBED),
    surfaceContainerLow = Color(0xFFE9F0F2),
    surfaceContainerHighest = Color(0xFFDCE7EB),
)

// 1.2 Canecas (Resolución 2184 de 2019) + 1.3 Acuario. Tokens propios fuera del ColorScheme.
object ReciclaKidsColors {
    // Una caneca nunca se distingue solo por color: siempre color + ícono + rótulo + personaje.
    val canecaBlanca = Color(0xFFFFFFFF)
    val canecaBlancaBorde = Color(0xFFB8C4C7)
    val onCanecaBlanca = Color(0xFF1A1C1E)
    val canecaNegra = Color(0xFF23262A)
    val onCanecaNegra = Color(0xFFFFFFFF)
    val canecaVerde = Color(0xFF1E8E4E)
    val onCanecaVerde = Color(0xFFFFFFFF)
    val halo = Color(0xFF8CE8F2) // resaltada / guía / imán

    val acuarioNivel1Turbio = Brush.verticalGradient(listOf(Color(0xFF8A7F6B), Color(0xFF5C5344)))
    val acuarioNivel2Opaco = Brush.verticalGradient(listOf(Color(0xFF7E948C), Color(0xFF4E6B68)))
    val acuarioNivel3Claro = Brush.verticalGradient(listOf(Color(0xFF5FB9CE), Color(0xFF2A7F99)))
    val acuarioNivel4Limpio = Brush.verticalGradient(listOf(Color(0xFF8CE8F2), Color(0xFF17A2C4)))
    val aguaJuego = Brush.verticalGradient(listOf(Color(0xFF7FE0EE), Color(0xFF1A93B6)))
    val contaminacion = Color(0xFF6B5B47)
    val algaSana = Color(0xFF2E9E63)
    val arena = Color(0xFFD8C9A8)
    val arenaSucia = Color(0xFF9C8A63)
    val burbuja = Color(0x99FFFFFF)

    val botonJugar = Color(0xFFFFC94D)
    val botonJugarSombra = Color(0xFFD39B18)
    val onBotonJugar = Color(0xFF4A3308)
    val botonConfirmar = Color(0xFF2E6B45)
    val botonConfirmarSombra = Color(0xFF1E4A2F)
    val tintaNino = Color(0xFF0C3A46)
}
