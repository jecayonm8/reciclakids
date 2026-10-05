package com.reciclakids.ui.acceso

import kotlin.random.Random

/** Suma sencilla de la puerta para adultos: la respuesta correcta y dos distractores. */
data class SumaPuerta(val a: Int, val b: Int, val opciones: List<Int>) {
    val respuesta: Int get() = a + b
}

/**
 * Genera la suma de la puerta. Los sumandos cambian cada vez para que un niño no pueda
 * memorizar qué botón tocar.
 */
fun generarSumaPuerta(random: Random = Random.Default): SumaPuerta {
    val a = random.nextInt(2, 7)
    val b = random.nextInt(2, 7)
    val respuesta = a + b
    val distractores = listOf(respuesta - 2, respuesta + 2, respuesta - 1, respuesta + 1).shuffled(random).take(2)
    return SumaPuerta(a, b, (distractores + respuesta).sorted())
}
