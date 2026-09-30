package com.reciclakids.datos.acceso

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ServicioAccesoEnMemoriaTest {

    private val servicio = ServicioAccesoEnMemoria(latenciaMs = 0)
    private val docente = RegistroDocente("Laura Restrepo", "laura.r@jardin.edu.co", "Jardín Gotitas", "Jardín B", "clave-de-prueba")
    private val acudiente = RegistroAcudiente("Mariana Ríos", "mariana.r@correo.com", "otra-clave-de-prueba", "JB-2M91")

    @Test
    fun laSesionAbreConElRolDeLaCuenta() = runBlocking {
        assertEquals(ResultadoRegistro.Creada, servicio.registrarDocente(docente))
        assertEquals(ResultadoRegistro.Creada, servicio.registrarAcudiente(acudiente))

        val sesionDocente = servicio.iniciarSesion(docente.correo, docente.contrasena)
        val sesionAcudiente = servicio.iniciarSesion(acudiente.correo, acudiente.contrasena)

        assertEquals(RolAdulto.Docente, (sesionDocente as ResultadoSesion.Iniciada).cuenta.rol)
        assertEquals(RolAdulto.Acudiente, (sesionAcudiente as ResultadoSesion.Iniciada).cuenta.rol)
    }

    @Test
    fun contrasenaEquivocadaOCuentaInexistenteDanElMismoResultado() = runBlocking {
        servicio.registrarDocente(docente)

        assertEquals(ResultadoSesion.CredencialesInvalidas, servicio.iniciarSesion(docente.correo, "no-es-la-clave"))
        assertEquals(ResultadoSesion.CredencialesInvalidas, servicio.iniciarSesion("nadie@correo.com", docente.contrasena))
    }

    @Test
    fun elCorreoNoDistingueMayusculasNiEspacios() = runBlocking {
        servicio.registrarDocente(docente)

        assertEquals(ResultadoRegistro.CorreoEnUso, servicio.registrarAcudiente(acudiente.copy(correo = " LAURA.R@jardin.edu.co ")))
        assertTrue(servicio.iniciarSesion("Laura.R@Jardin.edu.co", docente.contrasena) is ResultadoSesion.Iniciada)
    }

    @Test
    fun laRecuperacionNoRevelaSiElCorreoExiste() = runBlocking {
        servicio.registrarDocente(docente)

        assertEquals(
            servicio.enviarEnlaceRecuperacion(docente.correo),
            servicio.enviarEnlaceRecuperacion("nadie@correo.com"),
        )
    }
}
