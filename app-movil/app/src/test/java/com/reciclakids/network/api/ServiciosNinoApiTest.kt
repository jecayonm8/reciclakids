package com.reciclakids.network.api

import com.reciclakids.model.CatalogoResiduos
import com.reciclakids.model.CategoriaResiduo
import com.reciclakids.model.Dificultad
import com.reciclakids.model.Insignia
import com.reciclakids.model.Nino
import com.reciclakids.network.IntentoEnviado
import com.reciclakids.network.PartidaEnviada
import com.reciclakids.network.ResultadoCodigo
import com.reciclakids.network.ResultadoEnvio
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.LocalDate

private const val RetoJson =
    """{"id":"reto-9","codigo":"4729","fecha":"2026-10-16","dificultad":"Facil","categorias":["Organico","Aprovechable"],""" +
        """"residuos":["botella","cascara","lata","restos"]}"""

private const val CodigoJson =
    """{"reto":$RetoJson,"ninos":[{"id":"n1","nombre":"Salomé M.","avatar":"tortuga"},{"id":"n2","nombre":"Juan T.","avatar":"pez"}]}"""

private fun sesionNinoJson(token: String, limite: Int = 25) =
    """{"token":"$token","venceEnSegundos":604800,"nino":{"id":"n1","nombre":"Salomé M.","avatar":"tortuga"},""" +
        """"reto":$RetoJson,"limiteMinutosDiarios":$limite}"""

class ServiciosNinoApiTest {

    private val limites = mutableMapOf<String, Int>()

    private fun sesiones(api: ApiDePrueba) = SesionesNino(api.cliente, TokensNino(api.almacen)) { id, minutos -> limites[id] = minutos }

    private val partida = PartidaEnviada(
        idLocal = "2f0c-local",
        retoId = "reto-9",
        terminada = true,
        aciertos = 4,
        errores = 1,
        rachaMaxima = 3,
        tiempoMs = 61_000,
        intentos = listOf(
            IntentoEnviado("botella", CategoriaResiduo.Organico, false, 1_000),
            IntentoEnviado("botella", CategoriaResiduo.Aprovechable, true, 2_000),
        ),
    )

    // --- Código del reto (UI-08) ---

    @Test
    fun unCodigoValidoTraeElRetoConSuIdYLosNinosDelGrupoSinPedirlosDosVeces() = runBlocking {
        val api = ApiDePrueba { json(CodigoJson) }
        val servicio = ServicioRetosApi(api.cliente, sesiones(api))

        val resultado = servicio.buscarReto(" 4729", LocalDate.of(2026, 10, 16))

        val reto = (resultado as ResultadoCodigo.Valido).reto
        assertEquals("reto-9", reto.id)
        assertEquals("4729", reto.codigo)
        assertEquals(LocalDate.of(2026, 10, 16), reto.fecha)
        assertEquals(Dificultad.Facil, reto.dificultad)
        assertEquals(setOf(CategoriaResiduo.Aprovechable, CategoriaResiduo.Organico), reto.categorias)
        assertEquals(listOf(CatalogoResiduos.botellaPlastica, CatalogoResiduos.cascaraBanano, CatalogoResiduos.lata, CatalogoResiduos.restosComida), reto.residuos)
        assertEquals(listOf(Nino("n1", "Salomé M."), Nino("n2", "Juan T.")), servicio.ninosDelGrupo(reto))
        assertEquals(listOf("GET nino/retos/4729"), api.rutas())
        assertNull(api.peticiones.single().token)
    }

    @Test
    fun codigoInvalidoVencidoOSinRed() = runBlocking {
        fun servicio(api: ApiDePrueba) = ServicioRetosApi(api.cliente, sesiones(api))
        val hoy = LocalDate.of(2026, 10, 16)
        assertEquals(ResultadoCodigo.Invalido, servicio(ApiDePrueba { error(HttpStatusCode.NotFound, "codigo_invalido") }).buscarReto("0000", hoy))
        assertEquals(ResultadoCodigo.Vencido, servicio(ApiDePrueba { error(HttpStatusCode.Gone, "codigo_vencido") }).buscarReto("1234", hoy))
        assertEquals(ResultadoCodigo.SinConexion, servicio(ApiDePrueba { throw IOException("sin red") }).buscarReto("4729", hoy))
        assertEquals(ResultadoCodigo.SinConexion, servicio(ApiDePrueba { error(HttpStatusCode.TooManyRequests, "demasiados_intentos") }).buscarReto("4729", hoy))
    }

    @Test
    fun alElegirSuNombreElNinoRecibeSuTokenYSuLimiteDiario() = runBlocking {
        val api = ApiDePrueba { peticion -> if (peticion.ruta == "nino/sesiones") json(sesionNinoJson("token-salome", limite = 15)) else json(CodigoJson) }
        val servicio = ServicioRetosApi(api.cliente, sesiones(api))
        val reto = (servicio.buscarReto("4729", LocalDate.of(2026, 10, 16)) as ResultadoCodigo.Valido).reto

        servicio.abrirSesion(reto, Nino("n1", "Salomé M."))

        val solicitud = api.peticiones.last()
        assertEquals("nino/sesiones", solicitud.ruta)
        assertEquals("4729", solicitud.json["codigo"]?.jsonPrimitive?.content)
        assertEquals("n1", solicitud.json["ninoId"]?.jsonPrimitive?.content)
        assertEquals("token-salome", TokensNino(api.almacen).leer("n1"))
        assertEquals(15, limites["n1"])
        // El token queda cifrado en el almacén, nunca en una base sin cifrar.
        assertTrue(api.almacen.valores.keys.single().startsWith("sesion.nino."))
    }

    @Test
    fun unTokenVencidoNoSeUsa() {
        val almacen = AlmacenEnMemoria()
        var ahora = 0L
        val tokens = TokensNino(almacen) { ahora }
        tokens.guardar("n1", "token", venceEnSegundos = 7 * 24 * 3600)

        assertEquals("token", tokens.leer("n1"))
        ahora = 7 * 24 * 3600 * 1_000L
        assertNull(tokens.leer("n1"))
    }

    // --- Partidas (POST /nino/partidas) ---

    @Test
    fun lasPartidasViajanCompletasConElTokenDelNino() = runBlocking {
        val api = ApiDePrueba { peticion ->
            when (peticion.ruta) {
                "nino/partidas" -> json("""{"recibidas":1,"insigniasNuevas":["AmigaTortuga"]}""")
                else -> json("""{"retosCompletados":1,"rachaEntreRetos":1,"mejorRachaEntreRetos":3,"insignias":[],"limiteMinutosDiarios":30}""")
            }
        }
        val sesiones = sesiones(api)
        TokensNino(api.almacen).guardar("n1", "token-salome", 604_800)

        val resultado = ServicioResultadosApi(api.cliente, sesiones).enviar("n1", listOf(partida), codigoHoy = null)

        assertEquals(ResultadoEnvio.Recibido(listOf(Insignia.AmigaTortuga)), resultado)
        val envio = api.peticiones.first()
        assertEquals("nino/partidas", envio.ruta)
        assertEquals("token-salome", envio.token)
        val enviada = envio.json["partidas"]!!.jsonArray.single().jsonObject
        assertEquals("2f0c-local", enviada["idLocal"]?.jsonPrimitive?.content)
        assertEquals("reto-9", enviada["retoId"]?.jsonPrimitive?.content)
        assertEquals("true", enviada["terminada"]?.jsonPrimitive?.content)
        assertEquals(61_000, enviada["tiempoMs"]?.jsonPrimitive?.int)
        val intentos = enviada["intentos"]!!.jsonArray
        assertEquals(2, intentos.size)
        assertEquals("Organico", intentos[0].jsonObject["categoriaElegida"]?.jsonPrimitive?.content)
        // Y trae su límite diario actualizado.
        assertEquals("GET nino/progreso", api.rutas().last())
        assertEquals(30, limites["n1"])
    }

    @Test
    fun sinTokenSeAbreLaSesionConElCodigoDeHoyYSeEnvia() = runBlocking {
        val api = ApiDePrueba { peticion ->
            when (peticion.ruta) {
                "nino/sesiones" -> json(sesionNinoJson("token-nuevo"))
                "nino/partidas" -> json("""{"recibidas":1}""")
                else -> error(HttpStatusCode.ServiceUnavailable, "error_interno")
            }
        }

        val resultado = ServicioResultadosApi(api.cliente, sesiones(api)).enviar("n1", listOf(partida), codigoHoy = "4729")

        assertEquals(ResultadoEnvio.Recibido(), resultado)
        assertEquals(listOf("POST nino/sesiones", "POST nino/partidas", "GET nino/progreso"), api.rutas())
        assertEquals("token-nuevo", api.peticiones[1].token)
    }

    @Test
    fun unTokenRechazadoSeOlvidaYSeAbreOtraSesion() = runBlocking {
        val api = ApiDePrueba { peticion ->
            when {
                peticion.ruta == "nino/sesiones" -> json(sesionNinoJson("token-nuevo"))
                peticion.token == "token-viejo" -> error(HttpStatusCode.Unauthorized, "no_autenticado")
                peticion.ruta == "nino/partidas" -> json("""{"recibidas":1}""")
                else -> json("""{"retosCompletados":0,"rachaEntreRetos":0,"mejorRachaEntreRetos":0,"insignias":[],"limiteMinutosDiarios":20}""")
            }
        }
        TokensNino(api.almacen).guardar("n1", "token-viejo", 604_800)

        val resultado = ServicioResultadosApi(api.cliente, sesiones(api)).enviar("n1", listOf(partida), codigoHoy = "4729")

        assertEquals(ResultadoEnvio.Recibido(), resultado)
        assertEquals(listOf("POST nino/partidas", "POST nino/sesiones", "POST nino/partidas", "GET nino/progreso"), api.rutas())
        assertEquals("token-nuevo", TokensNino(api.almacen).leer("n1"))
    }

    @Test
    fun sinTokenNiCodigoVigenteLasPartidasEsperan() = runBlocking {
        val sinCodigo = ApiDePrueba { json("{}") }
        assertEquals(ResultadoEnvio.SinSesion, ServicioResultadosApi(sinCodigo.cliente, sesiones(sinCodigo)).enviar("n1", listOf(partida), null))
        assertTrue(sinCodigo.peticiones.isEmpty())

        val vencido = ApiDePrueba { error(HttpStatusCode.Gone, "codigo_vencido") }
        assertEquals(ResultadoEnvio.SinSesion, ServicioResultadosApi(vencido.cliente, sesiones(vencido)).enviar("n1", listOf(partida), "4729"))
    }

    @Test
    fun rechazoDelBackendYFaltaDeRed() = runBlocking {
        val rechazado = ApiDePrueba { error(HttpStatusCode.Forbidden, "prohibido") }
        TokensNino(rechazado.almacen).guardar("n1", "token", 604_800)
        assertEquals(ResultadoEnvio.Rechazado, ServicioResultadosApi(rechazado.cliente, sesiones(rechazado)).enviar("n1", listOf(partida), null))

        val sinRed = ApiDePrueba { throw IOException("sin red") }
        TokensNino(sinRed.almacen).guardar("n1", "token", 604_800)
        assertEquals(ResultadoEnvio.SinConexion, ServicioResultadosApi(sinRed.cliente, sesiones(sinRed)).enviar("n1", listOf(partida), "4729"))
        // Sin red no se pierde el token: sirve para el próximo intento.
        assertEquals("token", TokensNino(sinRed.almacen).leer("n1"))
    }
}
