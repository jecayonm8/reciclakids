package com.reciclakids.ui.acceso

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ValidacionTest {

    @Test
    fun aceptaCorreosBienFormados() {
        listOf("laura.docente@jardin.edu.co", "mariana.r@correo.com", "  a+b@x.co  ").forEach {
            assertTrue(it, correoValido(it))
        }
    }

    @Test
    fun rechazaCorreosIncompletos() {
        listOf("", "laura", "laura@", "@jardin.edu.co", "laura@jardin", "laura @jardin.co").forEach {
            assertFalse(it, correoValido(it))
        }
    }

    @Test
    fun contrasenaExigeOchoCaracteres() {
        assertFalse(contrasenaValida("1234567"))
        assertTrue(contrasenaValida("12345678"))
    }

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
