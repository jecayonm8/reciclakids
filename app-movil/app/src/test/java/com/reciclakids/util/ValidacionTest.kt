package com.reciclakids.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
}
