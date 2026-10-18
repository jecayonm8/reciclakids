package com.reciclakids.network.api

import com.reciclakids.model.CatalogoResiduos
import com.reciclakids.model.CategoriaResiduo
import com.reciclakids.model.Conteo
import com.reciclakids.model.ControlParental
import com.reciclakids.model.Dificultad
import com.reciclakids.model.Insignia
import com.reciclakids.model.TipoCorreo
import com.reciclakids.network.DatosPerfilAcudiente
import com.reciclakids.network.Respuesta
import com.reciclakids.network.ResultadoEdicionPerfil
import com.reciclakids.network.ResultadoVinculacion
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime

private const val HijoJson = """{"id":"n1","nombre":"Salomé M.","grupo":"Jardín B","avatar":"tortuga","vinculadoEl":"2026-09-01"}"""

private const val SemanaJson =
    """{"inicio":"2026-10-12","retosPublicados":5,"dias":[{"fecha":"2026-10-13","minutos":6,"completo":true,""" +
        """"porCategoria":{"Organico":{"aciertos":3,"total":4},"Aprovechable":{"aciertos":2,"total":2}},"rachaMaxima":4,""" +
        """"confusiones":["cascara","desconocido"]}],"insignias":[{"insignia":"AmigaTortuga","ganadaEl":"2026-10-13"},{"insignia":"CoralFeliz"}]}"""

class ServicioPadresApiTest {

    private val limitesGuardados = mutableListOf<Pair<String, Int>>()

    private fun servicio(api: ApiDePrueba) =
        ServicioPadresApi(api.cliente, api.sesion, alGuardarLimite = { id, minutos -> limitesGuardados += id to minutos })
            .also { api.conSesion(cuenta = ApiDePrueba.CuentaAcudiente) }

    @Test
    fun elInicioTraduceLaSemanaDelHijo() = runBlocking {
        val api = ApiDePrueba {
            json(
                """{"hijo":$HijoJson,"hoy":"2026-10-16","semana":$SemanaJson,"ultimoLogro":{"insignia":"AmigaTortuga","ganadaEl":"2026-10-13"},""" +
                    """"insigniasGanadas":1,"limiteMinutos":20,"sinDatos":false}"""
            )
        }
        val servicio = servicio(api)

        val inicio = (servicio.inicio("n1") as Respuesta.Ok).dato

        assertEquals("GET acudiente/hijos/n1/inicio", api.rutas().single())
        // Un niño agregado sin edad no la inventa.
        assertNull(inicio.hijo.edad)
        assertEquals(LocalDate.of(2026, 9, 1), inicio.hijo.vinculadoEl)
        assertNull(inicio.anterior)
        val dia = inicio.semana.dias.single()
        assertEquals(mapOf(CategoriaResiduo.Organico to Conteo(3, 4), CategoriaResiduo.Aprovechable to Conteo(2, 2)), dia.porCategoria)
        assertEquals(listOf(CatalogoResiduos.cascaraBanano), dia.confusiones)
        assertEquals(LocalDate.of(2026, 10, 13), inicio.ultimoLogro?.ganadaEl)
        assertEquals(listOf(true, false), inicio.semana.insignias.map { it.ganada })
        assertEquals(LocalDate.of(2026, 10, 16), servicio.hoy())
    }

    @Test
    fun elReportePideLaSemanaElegida() = runBlocking {
        val api = ApiDePrueba {
            json(
                """{"hijo":$HijoJson,"hoy":"2026-10-16","semanas":["2026-10-12","2026-10-05"],"semana":$SemanaJson,""" +
                    """"evolucion":[{"semana":"2026-10-05","aciertos":70},{"semana":"2026-10-12","aciertos":83}],"limiteMinutos":20}"""
            )
        }
        val servicio = servicio(api)

        val reporte = (servicio.reporte("n1", LocalDate.of(2026, 10, 12)) as Respuesta.Ok).dato
        servicio.reporte("n1", null)

        assertEquals("semana=2026-10-12", api.peticiones[0].consulta)
        assertEquals("", api.peticiones[1].consulta)
        assertEquals(listOf(LocalDate.of(2026, 10, 5) to 70, LocalDate.of(2026, 10, 12) to 83), reporte.evolucion)
    }

    @Test
    fun losCorreosEnviados() = runBlocking {
        val api = ApiDePrueba {
            json(
                """{"hijo":$HijoJson,"hoy":"2026-10-16","correos":[""" +
                    """{"tipo":"Logro","enviado":"2026-10-13T18:10","destinatario":"marta@correo.com","insignia":"AmigaTortuga"},""" +
                    """{"tipo":"ReporteSemanal","enviado":"2026-10-10T17:00:00","destinatario":"marta@correo.com","retosCompletados":4,"retosPublicados":5,"aciertos":80}]}"""
            )
        }

        val correos = (servicio(api).correos("n1") as Respuesta.Ok).dato.correos

        assertEquals(listOf(TipoCorreo.Logro, TipoCorreo.ReporteSemanal), correos.map { it.tipo })
        assertEquals(LocalDateTime.of(2026, 10, 13, 18, 10), correos[0].enviado)
        assertEquals(Insignia.AmigaTortuga, correos[0].insignia)
        assertEquals(80, correos[1].aciertos)
    }

    @Test
    fun elControlParentalIdaYVueltaYAvisaAlModoNinoDelTelefono() = runBlocking {
        val api = ApiDePrueba { peticion ->
            if (peticion.metodo.value == "PUT") vacia() else {
                json("""{"limiteMinutosDiarios":15,"dificultadSugerida":"Facil","correosLogro":false,"reporteSemanal":true,"recordatorioSinJugar":true}""")
            }
        }
        val servicio = servicio(api)

        val control = (servicio.controlParental("n1") as Respuesta.Ok).dato
        assertEquals(ControlParental(15, Dificultad.Facil, correosLogro = false, reporteSemanal = true, recordatorioSinJugar = true), control)

        assertTrue(servicio.guardarControlParental("n1", control.conMinutos(30)))
        val cuerpo = api.peticiones.last().json
        assertEquals("acudiente/hijos/n1/control-parental", api.peticiones.last().ruta)
        assertEquals(30, cuerpo["limiteMinutosDiarios"]?.jsonPrimitive?.int)
        assertTrue(cuerpo["recordatorioSinJugar"]!!.jsonPrimitive.boolean)
        assertEquals(listOf("n1" to 30), limitesGuardados)
    }

    @Test
    fun sinRedElControlNoSeGuardaNiCambiaElLimiteLocal() = runBlocking {
        val servicio = servicio(ApiDePrueba { throw IOException("sin red") })

        assertEquals(false, servicio.guardarControlParental("n1", ControlParental()))
        assertTrue(limitesGuardados.isEmpty())
    }

    @Test
    fun vincularOtroHijo() = runBlocking {
        val nuevo = ApiDePrueba { json("""{"hijo":$HijoJson,"yaEstaba":false}""") }
        assertTrue(servicio(nuevo).vincular(" jb-2m91 ") is ResultadoVinculacion.Vinculado)
        assertEquals("JB-2M91", nuevo.peticiones.single().json["codigo"]?.jsonPrimitive?.content)

        assertTrue(servicio(ApiDePrueba { json("""{"hijo":$HijoJson,"yaEstaba":true}""") }).vincular("JB-2M91") is ResultadoVinculacion.YaVinculado)
        assertEquals(
            ResultadoVinculacion.CodigoInvalido,
            servicio(ApiDePrueba { error(HttpStatusCode.UnprocessableEntity, "codigo_invalido") }).vincular("XX-0000"),
        )
        assertEquals(ResultadoVinculacion.SinConexion, servicio(ApiDePrueba { throw IOException("sin red") }).vincular("JB-2M91"))
    }

    @Test
    fun privacidadYSolicitudDeEliminacion() = runBlocking {
        val api = ApiDePrueba { peticion ->
            when (peticion.metodo.value) {
                "POST" -> json("""{"solicitadaEl":"2026-10-16"}""", HttpStatusCode.Created)
                "DELETE" -> vacia()
                else -> json("""{"hijo":$HijoJson,"autorizadaEl":"2026-09-01","autorizadaPor":"Marta Ruiz","registros":42,"insignias":3}""")
            }
        }
        val servicio = servicio(api)

        val privacidad = (servicio.privacidad("n1") as Respuesta.Ok).dato
        assertEquals(42, privacidad.registros)
        assertNull(privacidad.solicitudEliminacion)
        assertEquals(LocalDate.of(2026, 10, 16), servicio.solicitarEliminacion("n1"))
        assertTrue(servicio.cancelarSolicitudEliminacion("n1"))
        assertEquals("DELETE acudiente/hijos/n1/privacidad/solicitud-eliminacion", api.rutas().last())
    }

    @Test
    fun elPerfilYSuCorreo() = runBlocking {
        val api = ApiDePrueba { peticion ->
            if (peticion.metodo.value == "PUT") error(HttpStatusCode.Conflict, "correo_en_uso") else {
                json("""{"nombre":"Marta Ruiz","correo":"marta@correo.com","hijos":[$HijoJson]}""")
            }
        }
        val servicio = servicio(api)

        assertEquals(listOf("Salomé M."), (servicio.perfil() as Respuesta.Ok).dato.hijos.map { it.nombre })
        assertEquals(ResultadoEdicionPerfil.CorreoEnUso, servicio.guardarPerfil(DatosPerfilAcudiente("Marta", "laura@jardin.edu.co")))
    }
}
