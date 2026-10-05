package com.reciclakids.ui.acceso

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SumaPuertaTest {

    @Test
    fun sumaDeLaPuertaSiempreOfreceLaRespuestaYDosDistractores() {
        repeat(200) { semilla ->
            val suma = generarSumaPuerta(Random(semilla))

            assertEquals("semilla $semilla", 3, suma.opciones.toSet().size)
            assertEquals("semilla $semilla", 1, suma.opciones.count { it == suma.respuesta })
            assertTrue("semilla $semilla", suma.opciones.all { it > 0 })
            assertEquals(suma.a + suma.b, suma.respuesta)
        }
    }

    @Test
    fun sumaDeLaPuertaNoDejaLaRespuestaSiempreEnElMismoSitio() {
        val posiciones = (0 until 200).map { semilla ->
            val suma = generarSumaPuerta(Random(semilla))
            suma.opciones.indexOf(suma.respuesta)
        }.toSet()

        assertEquals(setOf(0, 1, 2), posiciones)
    }
}
