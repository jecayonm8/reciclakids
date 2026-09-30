package com.reciclakids.ui.theme

import androidx.compose.animation.core.spring

// 1.8 Movimiento (ms)
object Duracion {
    const val tacto = 80 // resalte al tocar · LinearEasing
    const val rapida = 150 // snap del objeto a la caneca
    const val estandar = 250 // transición de pantalla adultos
    const val enfatica = 400 // rebote del personaje
    const val celebracion = 900 // insignia, burbujas
    const val guiaCiclo = 1800 // pista en bucle tras 5 s de inactividad
    const val limiteFeedback = 500 // NFR: toda retroalimentación del juego empieza antes de esto
}

val ResorteRebote = spring<Float>(dampingRatio = 0.5f, stiffness = 380f)
