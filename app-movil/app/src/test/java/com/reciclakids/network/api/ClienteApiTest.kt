package com.reciclakids.network.api

import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ClienteApiTest {

    @Test
    fun anteUn401RefrescaGuardaLosTokensRotadosYRepiteLaPeticion() = runBlocking {
        val api = ApiDePrueba { peticion ->
            when (peticion.ruta) {
                "auth/refresco" -> json(sesionJson("acceso-2", "refresco-2"))
                else -> if (peticion.token == "acceso-2") json("""{"ninosQueEntraron":3}""") else error(HttpStatusCode.Unauthorized, "no_autenticado")
            }
        }
        api.conSesion("acceso-1", "refresco-1")

        val respuesta = api.cliente.get<IngresosDto>("docente/retos/r1/ingresos")

        assertEquals(RespuestaApi.Exito(IngresosDto(3)), respuesta)
        assertEquals(listOf("GET docente/retos/r1/ingresos", "POST auth/refresco", "GET docente/retos/r1/ingresos"), api.rutas())
        assertEquals("refresco-1", api.peticiones[1].json["tokenRefresco"].toString().trim('"'))
        // El token de refresco rotó: el viejo ya no se guarda en ninguna parte.
        assertEquals("refresco-2", api.sesion.leer()?.tokenRefresco)
        assertEquals("refresco-2", SesionAdultoGuardada(api.almacen).leer()?.tokenRefresco)
    }

    @Test
    fun variasPeticionesCon401AlTiempoRefrescanUnaSolaVez() = runBlocking {
        val listas = CompletableDeferred<Unit>()
        var rechazadas = 0
        val api = ApiDePrueba { peticion ->
            when {
                peticion.ruta == "auth/refresco" -> json(sesionJson("acceso-2", "refresco-2"))
                peticion.token == "acceso-2" -> json("""{"ninosQueEntraron":1}""")
                else -> {
                    // Las tres llegan con el token viejo antes de que alguna refresque.
                    if (synchronized(this@runBlocking) { ++rechazadas } == 3) listas.complete(Unit)
                    withTimeout(5_000) { listas.await() }
                    error(HttpStatusCode.Unauthorized, "no_autenticado")
                }
            }
        }
        api.conSesion("acceso-1", "refresco-1")

        val respuestas = (1..3).map { async { api.cliente.get<IngresosDto>("docente/retos/r$it/ingresos") } }.awaitAll()

        assertTrue(respuestas.all { it == RespuestaApi.Exito(IngresosDto(1)) })
        // Reusar un token de refresco ya rotado cerraría todas las sesiones de la cuenta.
        assertEquals(1, api.rutas().count { it == "POST auth/refresco" })
    }

    @Test
    fun siElRefrescoFallaBorraLaSesionYAvisaQueVencio() = runBlocking {
        val api = ApiDePrueba { peticion ->
            if (peticion.ruta == "auth/refresco") error(HttpStatusCode.Unauthorized, "sesion_vencida") else error(HttpStatusCode.Unauthorized, "no_autenticado")
        }
        api.conSesion()
        val aviso = async(start = CoroutineStart.UNDISPATCHED) { withTimeout(5_000) { api.cliente.sesionVencida.first() } }

        val respuesta = api.cliente.get<TableroDto>("docente/tablero")

        assertEquals(RespuestaApi.SesionVencida, respuesta)
        assertNull(api.sesion.leer())
        assertTrue(api.almacen.valores.isEmpty())
        aviso.await()
    }

    @Test
    fun sinRedElRefrescoNoCierraLaSesion() = runBlocking {
        val api = ApiDePrueba { peticion ->
            if (peticion.ruta == "auth/refresco") throw IOException("sin red") else error(HttpStatusCode.Unauthorized, "no_autenticado")
        }
        api.conSesion("acceso-1", "refresco-1")

        assertEquals(RespuestaApi.SinConexion, api.cliente.get<TableroDto>("docente/tablero"))
        assertEquals("refresco-1", api.sesion.leer()?.tokenRefresco)
    }

    @Test
    fun sinSesionGuardadaNoSeLlamaAlBackend() = runBlocking {
        val api = ApiDePrueba { json("{}") }

        assertEquals(RespuestaApi.SesionVencida, api.cliente.get<TableroDto>("docente/tablero"))
        assertTrue(api.peticiones.isEmpty())
    }

    @Test
    fun fallasDeRedErroresDelServidorYCuerposRarosSonSinConexion() = runBlocking {
        val api = ApiDePrueba { peticion ->
            when (peticion.ruta) {
                "red" -> throw IOException("sin red")
                "servidor" -> error(HttpStatusCode.InternalServerError, "error_interno")
                "caido" -> respondError(HttpStatusCode.BadGateway)
                else -> json("""{"inesperado":true}""")
            }
        }
        for (ruta in listOf("red", "servidor", "caido", "raro")) {
            assertEquals(ruta, RespuestaApi.SinConexion, api.cliente.get<IngresosDto>(ruta, Credencial.Ninguna))
        }
    }

    @Test
    fun un4xxTraeElCodigoDeErrorDelCuerpo() = runBlocking {
        val api = ApiDePrueba { error(HttpStatusCode.Gone, "codigo_vencido") }

        assertEquals(RespuestaApi.Rechazo(410, "codigo_vencido"), api.cliente.get<CodigoRetoDto>("nino/retos/1234", Credencial.Ninguna))
    }

    @Test
    fun elTokenDelNinoViajaEnSuPeticionYSinRefresco() = runBlocking {
        val api = ApiDePrueba { error(HttpStatusCode.Unauthorized, "no_autenticado") }

        val respuesta = api.cliente.get<ProgresoNinoDto>("nino/progreso", Credencial.Nino("token-nino"))

        assertEquals(RespuestaApi.Rechazo(401, "no_autenticado"), respuesta)
        assertEquals(listOf("GET nino/progreso"), api.rutas())
        assertEquals("token-nino", api.peticiones.single().token)
    }
}
