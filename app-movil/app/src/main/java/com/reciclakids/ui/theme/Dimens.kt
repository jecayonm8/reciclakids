package com.reciclakids.ui.theme

import androidx.compose.ui.unit.dp

// App exclusiva para smartphone en vertical. Ancho de referencia 360 dp (probar 320–411 dp).

// 1.5 Espaciado (rejilla 4 dp)
object Espacio {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
    val xxxl = 64.dp
}

// 1.6 Radios. Modo Niño: mínimo `lg`.
object Radio {
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val full = 999.dp
}

// 1.7 Elevación (niveles M3)
object Elevacion {
    val nivel0 = 0.dp
    val nivel1 = 1.dp
    val nivel2 = 3.dp
    val nivel3 = 6.dp
    val nivel4 = 8.dp
    val nivel5 = 12.dp
}

// Tamaños táctiles
object Tactil {
    val minimoNino = 72.dp
    val teclaNino = 76.dp // alto de tecla del TecladoNumericoNino
    val radioIman = 110.dp // arrastre tolerante en teléfono 360×800
    val minimoAdulto = 48.dp // M3; 44 dp solo en chips densos
}
