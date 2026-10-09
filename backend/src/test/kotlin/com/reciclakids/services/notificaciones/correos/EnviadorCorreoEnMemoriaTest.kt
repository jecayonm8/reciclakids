package com.reciclakids.services.notificaciones.correos

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class EnviadorCorreoEnMemoriaTest {

    @Test
    fun guardaLosCorreosSinEnviarlos() = runBlocking {
        val enviador = EnviadorCorreoEnMemoria()
        val correo = PlantillaCorreoLogro.renderizar(EjemplosCorreo.logro)

        enviador.enviar("mariana.r@correo.com", correo)

        assertEquals(listOf(EnviadorCorreoEnMemoria.CorreoGuardado("mariana.r@correo.com", correo)), enviador.enviados)
    }
}
