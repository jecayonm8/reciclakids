package com.reciclakids.network.api

import com.reciclakids.model.CuentaAdulto
import com.reciclakids.model.RolAdulto
import com.reciclakids.network.RegistroAcudiente
import com.reciclakids.network.RegistroDocente
import com.reciclakids.network.ResultadoRegistro
import com.reciclakids.network.ResultadoSesion
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ServicioAccesoApiTest {

    /** El cierre de sesión se avisa en segundo plano: las pruebas esperan a que termine. */
    private val trabajos = Job()

    private fun servicio(api: ApiDePrueba) = ServicioAccesoApi(api.cliente, api.sesion, CoroutineScope(Dispatchers.Unconfined + trabajos))

    private val acudiente = RegistroAcudiente("Marta Ruiz", "marta@correo.com", "clave-segura", "JB-2M91")

    @Test
    fun iniciarSesionGuardaLosTokensYDevuelveLaCuentaConSuRol() = runBlocking {
        val api = ApiDePrueba { json(sesionJson("acceso-1", "refresco-1")) }

        val resultado = servicio(api).iniciarSesion(" laura@jardin.edu.co ", "clave-segura")

        assertEquals(
            ResultadoSesion.Iniciada(CuentaAdulto("Laura Gómez", "laura@jardin.edu.co", RolAdulto.Docente, "Jardín Arcoíris", "Jardín B")),
            resultado,
        )
        assertEquals("laura@jardin.edu.co", api.peticiones.single().json["correo"]?.jsonPrimitive?.content)
        assertEquals("refresco-1", api.sesion.leer()?.tokenRefresco)
    }

    @Test
    fun credencialesMalasYFaltaDeRedSeDistinguen() = runBlocking {
        val malas = ApiDePrueba { error(HttpStatusCode.Unauthorized, "credenciales_invalidas") }
        assertEquals(ResultadoSesion.CredencialesInvalidas, servicio(malas).iniciarSesion("x@y.co", "otra-clave"))
        assertNull(malas.sesion.leer())

        val sinRed = ApiDePrueba { throw IOException("sin red") }
        assertEquals(ResultadoSesion.SinConexion, servicio(sinRed).iniciarSesion("x@y.co", "otra-clave"))
    }

    @Test
    fun elRegistroTraduceLosCodigosDeError() = runBlocking {
        val creada = ApiDePrueba { vacia(HttpStatusCode.Created) }
        assertEquals(ResultadoRegistro.Creada, servicio(creada).registrarDocente(RegistroDocente("Laura", "l@j.co", "Arcoíris", "Jardín B", "clave-segura")))
        assertEquals("POST auth/registro/docente", creada.rutas().single())

        val enUso = ApiDePrueba { error(HttpStatusCode.Conflict, "correo_en_uso") }
        assertEquals(ResultadoRegistro.CorreoEnUso, servicio(enUso).registrarAcudiente(acudiente))

        val codigoMalo = ApiDePrueba { error(HttpStatusCode.UnprocessableEntity, "codigo_invalido") }
        assertEquals(ResultadoRegistro.CodigoInvalido, servicio(codigoMalo).registrarAcudiente(acudiente))

        val caido = ApiDePrueba { error(HttpStatusCode.ServiceUnavailable, "error_interno") }
        assertEquals(ResultadoRegistro.SinConexion, servicio(caido).registrarAcudiente(acudiente))
    }

    @Test
    fun elAcudienteViajaConElCodigoDelNinoYLaAutorizacionExplicita() = runBlocking {
        val api = ApiDePrueba { vacia(HttpStatusCode.Created) }

        servicio(api).registrarAcudiente(acudiente)

        val cuerpo = api.peticiones.single().json
        assertEquals("auth/registro/acudiente", api.peticiones.single().ruta)
        assertEquals("JB-2M91", cuerpo["codigoVinculacion"]?.jsonPrimitive?.content)
        assertTrue(cuerpo["autorizaDatosMenor"]!!.jsonPrimitive.boolean)
    }

    @Test
    fun laRecuperacionSoloFallaSinRed() = runBlocking {
        assertTrue(servicio(ApiDePrueba { vacia(HttpStatusCode.Accepted) }).enviarEnlaceRecuperacion("x@y.co"))
        // Un correo mal escrito tampoco revela nada: para la pantalla, enviado.
        assertTrue(servicio(ApiDePrueba { error(HttpStatusCode.UnprocessableEntity, "correo_invalido") }).enviarEnlaceRecuperacion("x"))
        assertFalse(servicio(ApiDePrueba { throw IOException("sin red") }).enviarEnlaceRecuperacion("x@y.co"))
    }

    @Test
    fun laSesionGuardadaSeConfirmaConLaCuentaDelBackend() = runBlocking {
        val api = ApiDePrueba { json("""{"nombre":"Laura G.","correo":"laura@jardin.edu.co","rol":"Docente","jardin":"Arcoíris","grupo":"Jardín B"}""") }
        api.conSesion()

        val cuenta = servicio(api).sesionGuardada()

        assertEquals("GET cuenta", api.rutas().single())
        assertEquals("acceso-1", api.peticiones.single().token)
        assertEquals("Laura G.", cuenta?.nombre)
        assertEquals("Laura G.", api.sesion.leer()?.cuenta?.nombre)
    }

    @Test
    fun sinRedLaSesionGuardadaEntraConLaCuentaQueQuedoYVencidaPideIniciarSesion() = runBlocking {
        val sinRed = ApiDePrueba { throw IOException("sin red") }
        sinRed.conSesion(cuenta = ApiDePrueba.CuentaAcudiente)
        assertEquals(RolAdulto.Acudiente, servicio(sinRed).sesionGuardada()?.rol)

        val vencida = ApiDePrueba { error(HttpStatusCode.Unauthorized, "sesion_vencida") }
        vencida.conSesion()
        assertNull(servicio(vencida).sesionGuardada())
        assertNull(vencida.sesion.leer())

        val sinSesion = ApiDePrueba { json("{}") }
        assertNull(servicio(sinSesion).sesionGuardada())
        assertTrue(sinSesion.peticiones.isEmpty())
    }

    @Test
    fun cerrarSesionAvisaAlBackendConElTokenDeRefrescoYLaBorraDelTelefono() = runBlocking {
        val api = ApiDePrueba { vacia() }
        api.conSesion(refresco = "refresco-7")

        servicio(api).cerrarSesion()
        trabajos.children.toList().joinAll()

        assertNull(api.sesion.leer())
        assertTrue(api.almacen.valores.isEmpty())
        val cierre = api.peticiones.single()
        assertEquals(HttpMethod.Post, cierre.metodo)
        assertEquals("auth/cierre", cierre.ruta)
        assertEquals("refresco-7", cierre.json["tokenRefresco"]?.jsonPrimitive?.content)
    }
}
