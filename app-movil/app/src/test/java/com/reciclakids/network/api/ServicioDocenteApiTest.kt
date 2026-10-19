package com.reciclakids.network.api

import com.reciclakids.model.CategoriaResiduo
import com.reciclakids.model.Confusion
import com.reciclakids.model.Dificultad
import com.reciclakids.model.EstadoNinoHoy
import com.reciclakids.model.EstadoReto
import com.reciclakids.model.NinoAgregado
import com.reciclakids.model.RetoBorrador
import com.reciclakids.network.DatosPerfilDocente
import com.reciclakids.network.Respuesta
import com.reciclakids.network.ResultadoEdicionPerfil
import com.reciclakids.network.ResultadoPublicacion
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

private const val RetoHoyJson =
    """{"id":"reto-1","nombre":"Las tres canecas","categorias":["Aprovechable","NoAprovechable","Organico"],""" +
        """"dificultad":"Medio","fecha":"2026-10-16","estado":"Publicado","codigo":"4729"}"""

class ServicioDocenteApiTest {

    /** El teléfono va un día atrás del backend: manda el «hoy» del backend. */
    private val reloj = Clock.fixed(LocalDate.of(2026, 10, 15).atStartOfDay(ZoneId.of("America/Bogota")).toInstant(), ZoneId.of("America/Bogota"))

    private fun servicio(api: ApiDePrueba) = ServicioDocenteApi(api.cliente, api.sesion, reloj, intervaloIngresosMs = 1_000).also { api.conSesion() }

    @Test
    fun elTableroTraeElRetoDeHoyYElHoyDelBackend() = runBlocking {
        val api = ApiDePrueba {
            json("""{"hoy":"2026-10-16","nombreDocente":"Laura","grupo":"Jardín B","retoHoy":$RetoHoyJson,"completaron":3,"totalNinos":12}""")
        }
        val servicio = servicio(api)

        val tablero = (servicio.tablero() as Respuesta.Ok).dato

        assertEquals("GET docente/tablero", api.rutas().single())
        assertEquals("acceso-1", api.peticiones.single().token)
        assertEquals(3, tablero.completaron)
        assertNull(tablero.aciertosPromedio)
        val reto = checkNotNull(tablero.retoHoy)
        assertEquals(EstadoReto.Publicado, reto.estado)
        assertEquals(CategoriaResiduo.entries.toSet(), reto.categorias)
        assertEquals("4729", reto.codigoActivo(LocalDate.of(2026, 10, 16)))
        assertEquals(LocalDate.of(2026, 10, 16), servicio.hoy())
    }

    @Test
    fun unRetoONinoQueNoExisteEsNullYSinRedEsSinConexion() = runBlocking {
        val noExiste = servicio(ApiDePrueba { error(HttpStatusCode.NotFound, "reto_no_encontrado") })
        assertEquals(Respuesta.Ok(null), noExiste.reto("otro"))
        assertEquals(Respuesta.Ok(null), noExiste.detalleNino("otro"))

        val sinRed = servicio(ApiDePrueba { throw IOException("sin red") })
        assertEquals(Respuesta.SinConexion, sinRed.reto("reto-1"))
        assertEquals(Respuesta.SinConexion, sinRed.tablero())
    }

    @Test
    fun publicarMandaElBorradorYDevuelveElRetoConSuCodigo() = runBlocking {
        val api = ApiDePrueba { json(RetoHoyJson, HttpStatusCode.Created) }

        val resultado = servicio(api).publicar(RetoBorrador(CategoriaResiduo.entries.toSet(), Dificultad.Medio, LocalDate.of(2026, 10, 16)))

        assertEquals("4729", (resultado as ResultadoPublicacion.Publicado).reto.codigo)
        val cuerpo = api.peticiones.single().json
        assertEquals("2026-10-16", cuerpo["fecha"]?.jsonPrimitive?.content)
        assertEquals("Medio", cuerpo["dificultad"]?.jsonPrimitive?.content)
        assertEquals(3, cuerpo["categorias"]!!.jsonArray.size)
    }

    @Test
    fun sinRedElRetoQuedaComoBorradorLocalYSePublicaAlReintentar() = runBlocking {
        var hayRed = false
        val api = ApiDePrueba { peticion ->
            when {
                !hayRed -> throw IOException("sin red")
                peticion.metodo == HttpMethod.Post -> json(RetoHoyJson, HttpStatusCode.Created)
                else -> json("""{"hoy":"2026-10-16","retos":[$RetoHoyJson]}""")
            }
        }
        val servicio = servicio(api)
        val borrador = RetoBorrador(setOf(CategoriaResiduo.Organico), Dificultad.Facil, LocalDate.of(2026, 10, 16))

        val guardado = (servicio.publicar(borrador) as ResultadoPublicacion.GuardadoComoBorrador).reto
        assertEquals(EstadoReto.Borrador, guardado.estado)
        assertEquals("Reto de orgánicos", guardado.nombre)
        assertEquals(Respuesta.Ok(guardado), servicio.reto(guardado.id))

        hayRed = true
        val biblioteca = (servicio.biblioteca() as Respuesta.Ok).dato
        assertEquals(listOf(guardado.id, "reto-1"), biblioteca.retos.map { it.id })

        // El asistente reintenta con el id del borrador: al backend llega como reto nuevo.
        val publicado = servicio.publicar(borrador.copy(idExistente = guardado.id))
        assertTrue(publicado is ResultadoPublicacion.Publicado)
        assertFalse(api.peticiones.last().json.containsKey("idExistente"))
        assertEquals(listOf("reto-1"), (servicio.biblioteca() as Respuesta.Ok).dato.retos.map { it.id })
    }

    @Test
    fun losNinosQueEntraronSeConsultanCadaPocoYSoloEmitenCambios() = runTest {
        val respuestas = ArrayDeque(listOf(0, 0, 2, 2, 5))
        val api = ApiDePrueba { json("""{"ninosQueEntraron":${synchronized(respuestas) { respuestas.removeFirst() }}}""") }

        val vistos = servicio(api).ninosQueEntraron("reto-1").take(3).toList()

        assertEquals(listOf(0, 2, 5), vistos)
        assertEquals(5, api.rutas().count { it == "GET docente/retos/reto-1/ingresos" })
    }

    @Test
    fun elGrupoDeHoyYElDetalleDelNino() = runBlocking {
        val api = ApiDePrueba { peticion ->
            if (peticion.ruta == "docente/grupo/hoy") {
                json(
                    """{"nombreGrupo":"Jardín B","retoHoy":"Las tres canecas","ninos":[""" +
                        """{"id":"n1","nombre":"Salomé M.","estado":{"tipo":"Completo","minutos":4,"aciertos":5,"total":6}},""" +
                        """{"id":"n2","nombre":"Juan T.","estado":{"tipo":"EnCurso","residuoActual":3}},""" +
                        """{"id":"n3","nombre":"Ana L.","estado":{"tipo":"SinEmpezar"}}]}"""
                )
            } else {
                json(
                    """{"id":"n1","nombre":"Salomé M.","aciertosPorSemana":[70,85],"aciertosPorCategoria":{"Organico":90},""" +
                        """"confusion":{"residuo":"empaque metalizado","correcta":"NoAprovechable","elegida":"Aprovechable"},""" +
                        """"insignias":[{"nombre":"Amiga tortuga","ganada":true}],"codigoVinculacion":"JB-2M91","acudienteVinculado":true}"""
                )
            }
        }
        val servicio = servicio(api)

        val grupo = (servicio.grupoHoy() as Respuesta.Ok).dato
        assertEquals(
            listOf(EstadoNinoHoy.Completo(4, 5, 6), EstadoNinoHoy.EnCurso(3), EstadoNinoHoy.SinEmpezar),
            grupo.ninos.map { it.estado },
        )
        val detalle = checkNotNull((servicio.detalleNino("n1") as Respuesta.Ok).dato)
        assertEquals(mapOf(CategoriaResiduo.Organico to 90), detalle.aciertosPorCategoria)
        assertEquals(Confusion("el empaque metalizado", CategoriaResiduo.NoAprovechable, CategoriaResiduo.Aprovechable), detalle.confusion)
        assertEquals("JB-2M91", detalle.codigoVinculacion)
        assertTrue(detalle.acudienteVinculado)
    }

    @Test
    fun agregarYQuitarNinosYSuCodigoDeVinculacion() = runBlocking {
        val api = ApiDePrueba { peticion ->
            when {
                peticion.metodo == HttpMethod.Delete -> vacia()
                peticion.ruta.endsWith("codigo-vinculacion") -> json("""{"codigoVinculacion":"JB-7K3P"}""")
                else -> json("""{"id":"n9","nombre":"Emilio R.","codigoVinculacion":"JB-5H8W"}""", HttpStatusCode.Created)
            }
        }
        val servicio = servicio(api)

        assertEquals(Respuesta.Ok(NinoAgregado("n9", "Emilio R.", "JB-5H8W")), servicio.agregarNino("Emilio ", "R"))
        assertEquals("Emilio", api.peticiones[0].json["nombre"]?.jsonPrimitive?.content)
        assertEquals("R", api.peticiones[0].json["inicialApellido"]?.jsonPrimitive?.content)
        assertEquals("JB-7K3P", servicio.regenerarCodigoVinculacion("n9"))
        assertTrue(servicio.quitarNino("n9"))
        assertEquals(listOf("POST docente/ninos", "POST docente/ninos/n9/codigo-vinculacion", "DELETE docente/ninos/n9"), api.rutas())
    }

    @Test
    fun guardarElPerfilConUnCorreoAjenoEsCorreoEnUso() = runBlocking {
        val datos = DatosPerfilDocente("Laura", "otra@jardin.edu.co", "Arcoíris", "Jardín B")
        assertEquals(ResultadoEdicionPerfil.CorreoEnUso, servicio(ApiDePrueba { error(HttpStatusCode.Conflict, "correo_en_uso") }).guardarPerfil(datos))
        assertEquals(ResultadoEdicionPerfil.SinConexion, servicio(ApiDePrueba { throw IOException("sin red") }).guardarPerfil(datos))

        val api = ApiDePrueba {
            json("""{"nombre":"Laura","correo":"otra@jardin.edu.co","jardin":"Arcoíris","grupo":"Jardín B","totalNinos":12,"avisos":{}}""")
        }
        assertEquals(ResultadoEdicionPerfil.Guardado, servicio(api).guardarPerfil(datos))
        // La sesión guardada abre el modo con el correo nuevo la próxima vez.
        assertEquals("otra@jardin.edu.co", api.sesion.leer()?.cuenta?.correo)
    }

    @Test
    fun elReporteSemanalYLasAccionesDelPerfil() = runBlocking {
        val api = ApiDePrueba { peticion ->
            when (peticion.ruta) {
                "docente/reportes/semanal" -> json(
                    """{"inicio":"2026-10-05","fin":"2026-10-09","completadosPct":80,"cambioCompletados":5,"aciertosPct":76,""" +
                        """"cambioAciertos":-2,"retosPublicados":5,"retosReutilizados":1,"insignias":4,"cambioInsignias":1,""" +
                        """"participacionPorDia":[{"fecha":"2026-10-05","porcentaje":90}],"completadosPorNino":[{"nombre":"Salomé M.","porcentaje":100}]}"""
                )
                "docente/reportes/semanal/envio" -> json("""{"correosEnviados":7}""")
                else -> vacia()
            }
        }
        val servicio = servicio(api)

        val reporte = (servicio.reporteSemanal() as Respuesta.Ok).dato
        assertEquals(LocalDate.of(2026, 10, 9), reporte.fin)
        assertEquals(-2, reporte.cambioAciertos)
        assertTrue(servicio.enviarReporteAcudientes())
        assertTrue(servicio.guardarAvisos(com.reciclakids.model.AvisosDocente(ninosSinJugar = true)))
        assertEquals("true", api.peticiones.last().json["ninosSinJugar"]?.jsonPrimitive?.content)
        assertTrue(servicio.eliminarDatosGrupo())
        assertEquals("DELETE docente/grupo/datos", api.rutas().last())
    }
}
