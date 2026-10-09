package com.reciclakids.viewmodel

import com.reciclakids.model.CategoriaResiduo
import com.reciclakids.model.Dificultad
import com.reciclakids.model.EstadoReto
import com.reciclakids.model.RetoDocente
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AsistenteRetoTest {

    private val martes = LocalDate.of(2026, 9, 22)
    private val asistente = AsistenteReto(martes)

    private val lonchera = RetoDocente(
        id = "reto-lonchera",
        nombre = "Clasificar la lonchera",
        categorias = setOf(CategoriaResiduo.Aprovechable, CategoriaResiduo.Organico),
        dificultad = Dificultad.Medio,
        fecha = martes.minusDays(5),
        estado = EstadoReto.Publicado,
        codigo = "4729",
    )

    @Test
    fun arrancaEnElPaso1ConLasCanecasDeFacilEnMedioYParaHoy() {
        assertEquals(1, asistente.paso)
        assertEquals(setOf(CategoriaResiduo.Aprovechable, CategoriaResiduo.Organico), asistente.categorias)
        assertEquals(Dificultad.Medio, asistente.dificultad)
        assertEquals(martes, asistente.fecha)
    }

    @Test
    fun losDiasParaProgramarSonLosCincoDiasHabilesSiguientes() {
        val esperados = listOf(23, 24, 25, 28, 29).map { LocalDate.of(2026, 9, it) }

        assertEquals(esperados, asistente.diasProgramables)
    }

    @Test
    fun sinCanecasNoSePasaDelPaso1() {
        val vacio = asistente.alternar(CategoriaResiduo.Aprovechable).alternar(CategoriaResiduo.Organico)

        assertFalse(vacio.tieneCategorias)
        assertEquals(1, vacio.siguiente().paso)
        assertEquals(1, vacio.irA(4).paso)
    }

    @Test
    fun losChipsSaltanACualquierPasoValido() {
        assertEquals(4, asistente.irA(4).paso)
        assertEquals(1, asistente.irA(0).paso)
        assertEquals(1, asistente.irA(6).paso)
        assertEquals(1, asistente.atras().paso)
        assertTrue(asistente.irA(5).esUltimoPaso)
    }

    @Test
    fun programarSoloAceptaLosDiasOfrecidos() {
        val jueves = LocalDate.of(2026, 9, 24)

        val programado = asistente.programar(jueves)
        assertTrue(programado.programado)
        assertEquals(jueves, programado.fecha)

        assertEquals(programado, programado.programar(LocalDate.of(2026, 12, 25)))
        assertEquals(martes, programado.paraHoy().fecha)
    }

    @Test
    fun reutilizarCopiaLaConfiguracionParaHoyYSaltaALaVistaPrevia() {
        val copia = asistente.conDificultad(Dificultad.Dificil).programar().reutilizando(lonchera)

        assertEquals(AsistenteReto.PasoVistaPrevia, copia.paso)
        assertEquals(lonchera.categorias, copia.categorias)
        assertEquals(Dificultad.Medio, copia.dificultad)
        assertEquals(martes, copia.fecha)
        assertEquals("reto-lonchera", copia.aBorrador().reutilizadoDe)
        assertNull(copia.aBorrador().idExistente)
        assertEquals("Clasificar la lonchera", copia.aBorrador().nombre)
    }

    @Test
    fun editarUnProgramadoConservaSuIdYSuDiaAunqueQuedeLejos() {
        val lejano = LocalDate.of(2026, 10, 9)
        val programado = lonchera.copy(id = "reto-9", fecha = lejano, estado = EstadoReto.Programado, codigo = null)

        val edicion = asistente.editando(programado)

        assertEquals(1, edicion.paso)
        assertTrue(edicion.programado)
        assertEquals(lejano, edicion.fecha)
        assertTrue(lejano in edicion.diasProgramables)
        assertEquals("reto-9", edicion.aBorrador().idExistente)
    }

    @Test
    fun editarUnBorradorSinFechaLoDejaParaHoy() {
        val borrador = lonchera.copy(id = "reto-5", fecha = null, estado = EstadoReto.Borrador, codigo = null)

        val edicion = asistente.editando(borrador)

        assertFalse(edicion.programado)
        assertEquals(martes, edicion.aBorrador().fecha)
        assertEquals("reto-5", edicion.aBorrador().idExistente)
    }
}
