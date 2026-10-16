package com.reciclakids.network

import com.reciclakids.model.CategoriaResiduo
import com.reciclakids.model.CuentaAdulto
import com.reciclakids.model.Dificultad
import com.reciclakids.model.EstadoNinoHoy
import com.reciclakids.model.EstadoReto
import com.reciclakids.model.NinoHoy
import com.reciclakids.model.RetoBorrador
import com.reciclakids.model.RolAdulto
import com.reciclakids.model.residuosPorReto
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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
        categorias = setOf(CategoriaResiduo.NoAprovechable),
        dificultad = Dificultad.Facil,
        fecha = fecha,
        idExistente = idExistente,
    )

    @Test
    fun elTableroDemoTieneElRetoDeHoyYElAvanceDelGrupo() = runBlocking {
        val tablero = servicio().tablero().dato()

        assertEquals("Clasificar la lonchera", tablero.retoHoy?.nombre)
        assertEquals("4729", tablero.retoHoy?.codigoActivo(martes))
        assertEquals(6, tablero.completaron)
        assertEquals(10, tablero.totalNinos)
        // El reto de hoy es medio (6 residuos): 31 aciertos de 36 posibles entre los que terminaron.
        assertEquals(86, tablero.aciertosPromedio)
    }

    @Test
    fun quienTerminoElRetoDemoDeHoyClasificoTantosResiduosComoTieneSuDificultad() {
        val retoHoy = DatosDemoDocente.retos(martes).first { it.id == DatosDemoDocente.IdRetoHoy }
        val completos = DatosDemoDocente.ninos().map { it.estadoHoy }.filterIsInstance<EstadoNinoHoy.Completo>()

        assertTrue(completos.isNotEmpty())
        completos.forEach {
            assertEquals(retoHoy.dificultad.residuosPorReto, it.total)
            assertTrue("${it.aciertos} de ${it.total}", it.aciertos in 0..it.total)
        }
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
        assertNull(servicio.regenerarCodigoVinculacion(DatosDemoPadres.IdSalome))
        assertEquals(Respuesta.SinConexion, servicio.agregarNino("Pedro", "Q"))
        assertEquals(false, servicio.quitarNino(DatosDemoPadres.IdSalome))
    }

    @Test
    fun elContadorCuentaLosNinosQueYaEntraronConElCodigo() = runBlocking {
        // 6 terminaron y 2 van en curso.
        assertEquals(listOf(8), servicio().ninosQueEntraron(DatosDemoDocente.IdRetoHoy).toList())
    }

    @Test
    fun elReporteEsDeLaSemanaEscolarAnterior() = runBlocking {
        val reporte = servicio().reporteSemanal().dato()

        assertEquals(LocalDate.of(2026, 9, 14), reporte.inicio)
        assertEquals(LocalDate.of(2026, 9, 18), reporte.fin)
        assertEquals(5, reporte.participacionPorDia.size)
        assertEquals(10, reporte.completadosPorNino.size)
        // Solo nombre e inicial del apellido.
        assertTrue(reporte.completadosPorNino.all { Regex("^\\p{L}+ \\p{Lu}\\.$").matches(it.nombre) })
    }

    @Test
    fun eliminarLosDatosDelGrupoBorraResultadosInsigniasYReportes() = runBlocking {
        val servicio = servicio()

        assertTrue(servicio.eliminarDatosGrupo())

        assertTrue(servicio.reporteSemanal().dato().sinDatos)
        val detalle = servicio.detalleNino("nino-1").dato()
        assertTrue(detalle!!.sinResultados)
        assertTrue(detalle.insignias.none { it.ganada })
        assertTrue(servicio.grupoHoy().dato().ninos.all { it.estado == EstadoNinoHoy.SinEmpezar })
        assertEquals(10, servicio.grupoHoy().dato().ninos.size)
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
    fun elPerfilUsaLosDatosDelRegistro() = runBlocking {
        val perfil = servicio().perfil().dato()

        assertEquals("Gotitas", perfil.jardin)
        assertEquals("Jardín B", perfil.grupo)
        assertEquals(10, perfil.totalNinos)
    }

    // --- Niños del grupo y su código de vinculación ---

    private val salome = DatosDemoPadres.IdSalome
    private val emilio = DatosDemoPadres.IdEmilio

    /** Iniciales del grupo, guion y cuatro caracteres sin 0/O ni 1/I. */
    private val codigoGenerado = Regex("^JB-[A-HJ-NP-Z2-9]{4}$")

    private suspend fun ServicioDocenteEnMemoria.detalle(id: String) = detalleNino(id).dato()!!

    @Test
    fun cadaNinoTieneSuCodigoYLosDeLaDemoSonLosMismosDePadres() = runBlocking {
        val servicio = servicio()

        assertEquals("JB-2M91", servicio.detalle(salome).codigoVinculacion)
        assertTrue(servicio.detalle(salome).acudienteVinculado)
        // El código de Emilio es el que usa la demo de padres para vincularlo.
        assertEquals(emilio, DatosDemoPadres.codigos[servicio.detalle(emilio).codigoVinculacion])
        assertEquals(false, servicio.detalle(emilio).acudienteVinculado)
        val codigos = servicio.grupoHoy().dato().ninos.map { servicio.detalle(it.id).codigoVinculacion }
        assertEquals(10, codigos.toSet().size)
        assertTrue(codigos.all { Regex("^JB-[A-Z0-9]{4}$").matches(it) })
    }

    @Test
    fun agregarUnNinoGuardaSoloSuNombreConLaInicialYLeDaSuPropioCodigo() = runBlocking {
        val servicio = servicio()

        val nuevo = servicio.agregarNino(" Pedro ", "q").dato()

        assertEquals("Pedro Q.", nuevo.nombre)
        assertTrue("código ${nuevo.codigoVinculacion}", codigoGenerado.matches(nuevo.codigoVinculacion))
        assertTrue(servicio.grupoHoy().dato().ninos.contains(NinoHoy(nuevo.id, "Pedro Q.", EstadoNinoHoy.SinEmpezar)))
        val detalle = servicio.detalle(nuevo.id)
        assertTrue(detalle.sinResultados)
        assertEquals(nuevo.codigoVinculacion, detalle.codigoVinculacion)
        assertEquals(false, detalle.acudienteVinculado)
        assertEquals(11, servicio.tablero().dato().totalNinos)
        // Todavía no tiene semanas jugadas: no entra al reporte, que sigue saliendo.
        assertTrue(servicio.reporteSemanal().dato().completadosPorNino.none { it.nombre == "Pedro Q." })
    }

    @Test
    fun regenerarCambiaSoloElCodigoDeEseNinoYSuAcudienteSigueVinculado() = runBlocking {
        val servicio = servicio()

        val nuevo = servicio.regenerarCodigoVinculacion(salome)

        assertTrue("código $nuevo", nuevo != null && codigoGenerado.matches(nuevo))
        assertEquals(nuevo, servicio.detalle(salome).codigoVinculacion)
        assertTrue(servicio.detalle(salome).acudienteVinculado)
        assertEquals("JB-5H8W", servicio.detalle(emilio).codigoVinculacion)
        assertNull(servicio.regenerarCodigoVinculacion("nino-que-no-existe"))
    }

    @Test
    fun quitarUnNinoLoSacaDelGrupoYBorraSusDatos() = runBlocking {
        val servicio = servicio()

        assertTrue(servicio.quitarNino(salome))

        assertTrue(servicio.grupoHoy().dato().ninos.none { it.id == salome })
        assertNull(servicio.detalleNino(salome).dato())
        val tablero = servicio.tablero().dato()
        assertEquals(9, tablero.totalNinos)
        // Salomé ya había completado el reto de hoy: su resultado también se borra.
        assertEquals(5, tablero.completaron)
        assertTrue(servicio.reporteSemanal().dato().completadosPorNino.none { it.nombre == "Salomé M." })
    }

    @Test
    fun losAvisosSeGuardan() = runBlocking {
        val servicio = servicio()
        val avisos = servicio.perfil().dato().avisos.copy(ninosSinJugar = true)

        assertTrue(servicio.guardarAvisos(avisos))

        assertEquals(avisos, servicio.perfil().dato().avisos)
    }

    @Test
    fun elPerfilEditadoSeVeEnLaCuentaYEnElTablero() = runBlocking {
        val servicio = servicio()
        val datos = DatosPerfilDocente("Laura Gómez", "laura.gomez@jardin.edu.co", "Gotitas del Saber", "Jardín C")

        assertEquals(ResultadoEdicionPerfil.Guardado, servicio.guardarPerfil(datos))

        assertEquals(datos, servicio.perfil().dato().datosEditables)
        assertEquals("Laura Gómez", servicio.tablero().dato().nombreDocente)
        assertEquals("Jardín C", servicio.grupoHoy().dato().nombreGrupo)
    }

    @Test
    fun sinConexionElPerfilNoCambia() = runBlocking {
        val servicio = servicio()
        servicio.sinConexion = true

        val resultado = servicio.guardarPerfil(DatosPerfilDocente("Otra", "otra@jardin.edu.co", "Otro", "Otro"))

        assertEquals(ResultadoEdicionPerfil.SinConexion, resultado)
        servicio.sinConexion = false
        assertEquals("Laura Restrepo", servicio.perfil().dato().nombre)
    }
}
