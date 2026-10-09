package com.reciclakids.network

import com.reciclakids.model.CatalogoResiduos
import com.reciclakids.model.CategoriaResiduo
import com.reciclakids.model.ControlParental
import com.reciclakids.model.CuentaAdulto
import com.reciclakids.model.Insignia
import com.reciclakids.model.MensajeSemana
import com.reciclakids.model.RolAdulto
import com.reciclakids.model.TipoCorreo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class ServicioPadresEnMemoriaTest {

    // Viernes 18 de septiembre de 2026, 6:30 p. m.: ya salió el reporte de la semana.
    private val viernes = LocalDate.of(2026, 9, 18)
    private val cuenta = CuentaAdulto("Mariana Ríos", "mariana.r@correo.com", RolAdulto.Acudiente)
    private val salome = DatosDemoPadres.IdSalome
    private val martin = DatosDemoPadres.IdMartin

    private fun reloj(momento: LocalDateTime): Clock = Clock.fixed(momento.toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    private fun servicio(
        momento: LocalDateTime = viernes.atTime(18, 30),
        controles: ControlesParentales = ControlesParentales(),
        conDatosDemo: Boolean = true,
    ) = ServicioPadresEnMemoria(cuenta, controles, latenciaMs = 0, reloj = reloj(momento), conDatosDemo = conDatosDemo)

    private fun <T> Respuesta<T>.dato(): T = (this as Respuesta.Ok).dato

    @Test
    fun laCuentaArrancaConSusDosHijosVinculados() = runBlocking {
        val hijos = servicio().hijos().dato()

        assertEquals(listOf("Salomé M.", "Martín M."), hijos.map { it.nombre })
        // Solo nombre e inicial del apellido (Ley 1581 de 2012).
        assertTrue(hijos.all { Regex("^\\p{L}+ \\p{Lu}\\.$").matches(it.nombre) })
    }

    @Test
    fun elInicioResumeLaSemanaEnCincoDatos() = runBlocking {
        val inicio = servicio().inicio(salome).dato()

        assertFalse(inicio.sinDatos)
        assertEquals(LocalDate.of(2026, 9, 14), inicio.semana.inicio)
        assertEquals(4, inicio.semana.retosCompletados)
        assertEquals(5, inicio.semana.retosPublicados)
        // 20 aciertos al primer intento de 24 residuos.
        assertEquals(83, inicio.semana.aciertos)
        assertEquals(6, inicio.semana.rachaMaxima)
        assertEquals(Insignia.AmigaTortuga, inicio.ultimoLogro?.insignia)
        assertEquals(LocalDate.of(2026, 9, 17), inicio.ultimoLogro?.ganadaEl)
        assertEquals(4, inicio.insigniasGanadas)
        assertEquals(20, inicio.limiteMinutos)
        assertEquals(MensajeSemana.Mejoro(CategoriaResiduo.Organico, antes = 67, ahora = 88), inicio.mensaje)
    }

    @Test
    fun lasInsigniasDeLaDemoSalenDeLasReglasDelJuego() = runBlocking {
        val logros = servicio().logros(salome).dato()
        val ganadas = logros.logros.filter { it.ganada }.associate { it.insignia to it.ganadaEl }

        assertEquals(
            mapOf(
                Insignia.RachaDeCinco to LocalDate.of(2026, 8, 31),
                Insignia.CincoRetosDiarios to LocalDate.of(2026, 9, 1),
                Insignia.PulpoOrdenado to LocalDate.of(2026, 9, 14),
                Insignia.AmigaTortuga to LocalDate.of(2026, 9, 17),
            ),
            ganadas,
        )
        assertEquals(Insignia.entries.size, logros.logros.size)
        assertEquals(13, logros.retosCompletados)
        assertEquals(13, logros.diasConReto)
    }

    @Test
    fun elReporteTraeCuatroSemanasYSeCambiaDeSemana() = runBlocking {
        val servicio = servicio()
        val reporte = servicio.reporte(salome, semana = null).dato()

        assertEquals(4, reporte.semanas.size)
        assertEquals(LocalDate.of(2026, 9, 14), reporte.semana.inicio)
        assertEquals(listOf(61, 78, 72, 83), reporte.evolucion.map { it.second })
        assertEquals(11, reporte.cambio)
        assertEquals(CatalogoResiduos.empaqueMetalizado, reporte.semana.confusionFrecuente)
        assertEquals(listOf(16, 20, 0, 18, 18), reporte.semana.minutosPorDia(viernes).map { it.second })
        assertEquals(72, reporte.semana.minutos)
        assertEquals(14, reporte.semana.minutosPromedio)
        assertEquals(listOf(Insignia.PulpoOrdenado, Insignia.AmigaTortuga), reporte.semana.insignias.map { it.insignia })

        // Cualquier día de la semana lleva a su lunes.
        val otra = servicio.reporte(salome, semana = LocalDate.of(2026, 9, 2)).dato()
        assertEquals(LocalDate.of(2026, 8, 31), otra.semana.inicio)
        assertEquals(LocalDate.of(2026, 8, 24), otra.anterior?.inicio)
        assertEquals(listOf(Insignia.RachaDeCinco, Insignia.CincoRetosDiarios), otra.semana.insignias.map { it.insignia })
    }

    @Test
    fun aMitadDeSemanaSoloCuentanLosDiasQueYaPasaron() = runBlocking {
        val martes = servicio(momento = LocalDate.of(2026, 9, 15).atTime(9, 0))
        val inicio = martes.inicio(salome).dato()

        assertEquals(2, inicio.semana.retosPublicados)
        assertEquals(2, inicio.semana.retosCompletados)
        assertEquals(listOf(Insignia.PulpoOrdenado), inicio.semana.insignias.map { it.insignia })
        // El reporte de esta semana todavía no ha salido.
        val correos = martes.correos(salome).dato().correos
        assertTrue(correos.none { it.tipo == TipoCorreo.ReporteSemanal && !it.enviado.toLocalDate().isBefore(LocalDate.of(2026, 9, 14)) })
    }

    @Test
    fun elHistorialRegistraLosLogrosYLosReportesYaEnviados() = runBlocking {
        val antesDeLas5 = servicio(momento = viernes.atTime(16, 0)).correos(salome).dato().correos
        val despues = servicio().correos(salome).dato().correos

        assertEquals(7, antesDeLas5.size)
        assertEquals(8, despues.size)
        val ultimo = despues.first()
        assertEquals(TipoCorreo.ReporteSemanal, ultimo.tipo)
        assertEquals(viernes.atTime(17, 0), ultimo.enviado)
        assertEquals(4, ultimo.retosCompletados)
        assertEquals(83, ultimo.aciertos)
        assertTrue(despues.all { it.destinatario == "mariana.r@correo.com" })
        assertEquals(4, despues.count { it.tipo == TipoCorreo.Logro })
    }

    @Test
    fun unHijoQueTodaviaNoJuegaVeLosEstadosVacios() = runBlocking {
        val servicio = servicio()

        assertTrue(servicio.inicio(martin).dato().sinDatos)
        assertTrue(servicio.reporte(martin, null).dato().sinDatos)
        assertTrue(servicio.correos(martin).dato().correos.isEmpty())
        assertTrue(servicio.logros(martin).dato().logros.none { it.ganada })
        assertTrue(servicio(conDatosDemo = false).inicio(salome).dato().sinDatos)
    }

    @Test
    fun elControlParentalSeGuardaParaElModoNino() = runBlocking {
        val controles = ControlesParentales()
        val servicio = servicio(controles = controles)
        val nuevo = ControlParental(limiteMinutosDiarios = 10, correosLogro = false)

        assertTrue(servicio.guardarControlParental(salome, nuevo))

        assertEquals(nuevo, servicio.controlParental(salome).dato())
        assertEquals(10, controles.de(salome).limiteMinutosDiarios)
        assertEquals(nuevo, controles.observar(salome).first())
        assertEquals(10, servicio.inicio(salome).dato().limiteMinutos)
        // El de su hermano no cambia.
        assertEquals(ControlParental(), controles.de(martin))
    }

    @Test
    fun laSolicitudDeEliminacionQuedaPendienteYSePuedeCancelar() = runBlocking {
        val servicio = servicio()
        val antes = servicio.privacidad(salome).dato()
        assertNull(antes.solicitudEliminacion)
        assertEquals(13, antes.registros)
        assertEquals(4, antes.insignias)
        assertEquals("Mariana Ríos", antes.autorizadaPor)

        assertEquals(viernes, servicio.solicitarEliminacion(salome))
        assertEquals(viernes, servicio.privacidad(salome).dato().solicitudEliminacion)

        assertTrue(servicio.cancelarSolicitudEliminacion(salome))
        assertNull(servicio.privacidad(salome).dato().solicitudEliminacion)
    }

    @Test
    fun vincularUsaElCodigoDeLaDocenteUnaSolaVez() = runBlocking {
        val servicio = servicio()

        val nuevo = servicio.vincular(" jb-5h8w ")
        assertEquals("Emilio R.", (nuevo as ResultadoVinculacion.Vinculado).hijo.nombre)
        assertEquals(3, servicio.hijos().dato().size)
        assertTrue(servicio.vincular("JB-5H8W") is ResultadoVinculacion.YaVinculado)
        assertTrue(servicio.vincular("JB-2M91") is ResultadoVinculacion.YaVinculado)
        assertEquals(ResultadoVinculacion.CodigoInvalido, servicio.vincular("ZZ-0000"))
    }

    @Test
    fun sinConexionNadaSeGuardaNiSeConsulta() = runBlocking {
        val servicio = servicio()
        servicio.sinConexion = true

        assertEquals(Respuesta.SinConexion, servicio.hijos())
        assertEquals(Respuesta.SinConexion, servicio.inicio(salome))
        assertFalse(servicio.guardarControlParental(salome, ControlParental(limiteMinutosDiarios = 5)))
        assertNull(servicio.solicitarEliminacion(salome))
        assertEquals(ResultadoVinculacion.SinConexion, servicio.vincular("JB-5H8W"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun unAcudienteNoPuedeConsultarAUnNinoQueNoEsSuyo() {
        runBlocking { servicio().inicio("nino-2") }
    }
}
