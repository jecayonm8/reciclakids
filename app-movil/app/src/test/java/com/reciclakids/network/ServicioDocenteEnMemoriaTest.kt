package com.reciclakids.network

import com.reciclakids.model.CategoriaResiduo
import com.reciclakids.model.CuentaAdulto
import com.reciclakids.model.Dificultad
import com.reciclakids.model.EstadoNinoHoy
import com.reciclakids.model.EstadoReto
import com.reciclakids.model.RetoBorrador
import com.reciclakids.model.RolAdulto
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.random.Random

/** Reloj que la prueba puede adelantar para ver cómo se activa un reto programado. */
private class RelojMovil(var fecha: LocalDate) : Clock() {
    override fun getZone(): ZoneId = ZoneOffset.UTC
    override fun withZone(zone: ZoneId?): Clock = this
    override fun instant(): Instant = fecha.atTime(9, 0).toInstant(ZoneOffset.UTC)
}

class ServicioDocenteEnMemoriaTest {

    // Martes 22 de septiembre de 2026, el día de los prototipos.
    private val martes = LocalDate.of(2026, 9, 22)
    private val reloj = RelojMovil(martes)
    private val cuenta = CuentaAdulto("Laura Restrepo", "laura.r@jardin.edu.co", RolAdulto.Docente, "Gotitas", "Jardín B")

    private fun servicio(conDatosDemo: Boolean = true) =
        ServicioDocenteEnMemoria(cuenta, latenciaMs = 0, reloj = reloj, azar = Random(7), conDatosDemo = conDatosDemo)

    private fun <T> Respuesta<T>.dato(): T = (this as Respuesta.Ok).dato

    private fun borrador(fecha: LocalDate = martes, idExistente: String? = null) = RetoBorrador(
        categorias = setOf(CategoriaResiduo.NoAprovechables),
        dificultad = Dificultad.Facil,
        fecha = fecha,
        idExistente = idExistente,
    )

    @Test
    fun elTableroDemoTieneElRetoDeHoyYElAvanceDelGrupo() = runBlocking {
        val tablero = servicio().tablero().dato()

        assertEquals("Clasificar la lonchera", tablero.retoHoy?.nombre)
        assertEquals("4729", tablero.retoHoy?.codigoActivo(martes))
        assertEquals(8, tablero.completaron)
        assertEquals(12, tablero.totalNinos)
        // 56 aciertos de 64 posibles entre los que terminaron.
        assertEquals(88, tablero.aciertosPromedio)
    }

    @Test
    fun publicarParaHoyGeneraUnCodigoDeCuatroDigitosYPasaASerElRetoDelTablero() = runBlocking {
        val servicio = servicio()

        val reto = (servicio.publicar(borrador()) as ResultadoPublicacion.Publicado).reto

        val codigo = reto.codigoActivo(martes)
        assertTrue("código $codigo", codigo != null && codigo.length == 4 && codigo.all(Char::isDigit))
        assertEquals(EstadoReto.Publicado, reto.estado)
        assertEquals("Reto de no aprovechables", reto.nombre)
        assertEquals(reto, servicio.tablero().dato().retoHoy)
        assertEquals(reto, servicio.biblioteca().dato().retos.first())
        // Nadie ha entrado todavía con el código nuevo.
        assertEquals(0, servicio.tablero().dato().completaron)
        assertEquals(listOf(0), servicio.ninosQueEntraron(reto.id).toList())
    }

    @Test
    fun unRetoParaOtroDiaQuedaProgramadoSinCodigoYSeActivaEseDia() = runBlocking {
        val servicio = servicio()
        val miercoles = martes.plusDays(1)

        val reto = (servicio.publicar(borrador(fecha = miercoles)) as ResultadoPublicacion.Publicado).reto
        assertEquals(EstadoReto.Programado, reto.estado)
        assertNull(reto.codigo)

        reloj.fecha = miercoles
        val activado = servicio.reto(reto.id).dato()

        assertEquals(EstadoReto.Publicado, activado?.estado)
        assertTrue(activado?.codigoActivo(miercoles) != null)
    }

    @Test
    fun sinConexionElRetoQuedaComoBorradorYReintentarNoLoDuplica() = runBlocking {
        val servicio = servicio()
        val antes = servicio.biblioteca().dato().retos.size

        servicio.sinConexion = true
        val guardado = (servicio.publicar(borrador()) as ResultadoPublicacion.GuardadoComoBorrador).reto
        assertEquals(EstadoReto.Borrador, guardado.estado)

        servicio.sinConexion = false
        val publicado = (servicio.publicar(borrador(idExistente = guardado.id)) as ResultadoPublicacion.Publicado).reto

        val retos = servicio.biblioteca().dato().retos
        assertEquals(antes + 1, retos.size)
        assertEquals(guardado.id, publicado.id)
        assertEquals(EstadoReto.Publicado, retos.single { it.id == guardado.id }.estado)
    }

    @Test
    fun sinConexionLasConsultasFallanYElContadorNoEmite() = runBlocking {
        val servicio = servicio()
        servicio.sinConexion = true

        assertEquals(Respuesta.SinConexion, servicio.tablero())
        assertEquals(Respuesta.SinConexion, servicio.perfil())
        assertEquals(emptyList<Int>(), servicio.ninosQueEntraron(DatosDemoDocente.IdRetoHoy).toList())
        assertEquals(false, servicio.enviarReporteAcudientes())
        assertNull(servicio.regenerarCodigoVinculacion())
    }

    @Test
    fun elContadorCuentaLosNinosQueYaEntraronConElCodigo() = runBlocking {
        // 8 terminaron y 2 van en curso.
        assertEquals(listOf(10), servicio().ninosQueEntraron(DatosDemoDocente.IdRetoHoy).toList())
    }

    @Test
    fun elReporteEsDeLaSemanaEscolarAnterior() = runBlocking {
        val reporte = servicio().reporteSemanal().dato()

        assertEquals(LocalDate.of(2026, 9, 14), reporte.inicio)
        assertEquals(LocalDate.of(2026, 9, 18), reporte.fin)
        assertEquals(5, reporte.participacionPorDia.size)
        assertEquals(12, reporte.completadosPorNino.size)
        // Solo nombre e inicial del apellido.
        assertTrue(reporte.completadosPorNino.all { Regex("^\\p{L}+ \\p{Lu}\\.$").matches(it.nombre) })
    }

    @Test
    fun eliminarLosDatosDelGrupoBorraResultadosInsigniasYReportes() = runBlocking {
        val servicio = servicio()

        assertTrue(servicio.eliminarDatosGrupo())

        assertTrue(servicio.reporteSemanal().dato().sinDatos)
        val detalle = servicio.detalleNino("n01").dato()
        assertTrue(detalle!!.sinResultados)
        assertTrue(detalle.insignias.none { it.ganada })
        assertTrue(servicio.grupoHoy().dato().ninos.all { it.estado == EstadoNinoHoy.SinEmpezar })
        assertEquals(12, servicio.grupoHoy().dato().ninos.size)
    }

    @Test
    fun unaCuentaNuevaVeTodoVacio() = runBlocking {
        val servicio = servicio(conDatosDemo = false)

        assertNull(servicio.tablero().dato().retoHoy)
        assertTrue(servicio.biblioteca().dato().retos.isEmpty())
        assertTrue(servicio.grupoHoy().dato().ninos.isEmpty())
        assertTrue(servicio.reporteSemanal().dato().sinDatos)
    }

    @Test
    fun elPerfilUsaLosDatosDelRegistroYElCodigoDeVinculacionLlevaLasInicialesDelGrupo() = runBlocking {
        val servicio = servicio()
        val perfil = servicio.perfil().dato()

        assertEquals("Gotitas", perfil.jardin)
        assertEquals("Jardín B", perfil.grupo)
        assertEquals("JB-2M91", perfil.codigoVinculacion)

        val nuevo = servicio.regenerarCodigoVinculacion()
        assertTrue("código $nuevo", nuevo != null && Regex("^JB-[A-HJ-NP-Z2-9]{4}$").matches(nuevo))
        assertNotEquals("JB-2M91", nuevo)
        assertEquals(nuevo, servicio.perfil().dato().codigoVinculacion)
    }

    @Test
    fun losAvisosSeGuardan() = runBlocking {
        val servicio = servicio()
        val avisos = servicio.perfil().dato().avisos.copy(ninosSinJugar = true)

        assertTrue(servicio.guardarAvisos(avisos))

        assertEquals(avisos, servicio.perfil().dato().avisos)
    }
}
