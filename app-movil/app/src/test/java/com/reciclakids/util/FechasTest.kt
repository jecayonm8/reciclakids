package com.reciclakids.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FechasTest {

    private val martes = LocalDate.of(2026, 9, 22)

    @Test
    fun losNombresNoDependenDelIdiomaDelTelefono() {
        assertEquals("mar 22", diaCorto(martes))
        assertEquals("mié 23", diaCorto(martes.plusDays(1)))
        assertEquals("martes 22", diaLargo(martes))
        assertEquals("dom", nombreDiaCorto(LocalDate.of(2026, 9, 27)))
    }

    @Test
    fun elRangoNombraElMesUnaSolaVezSiNoCambia() {
        assertEquals("15 – 19 de septiembre", rangoFechas(LocalDate.of(2026, 9, 15), LocalDate.of(2026, 9, 19)))
        assertEquals("29 de septiembre – 3 de octubre", rangoFechas(LocalDate.of(2026, 9, 29), LocalDate.of(2026, 10, 3)))
    }

    @Test
    fun losDiasHabilesSaltanElFinDeSemana() {
        val viernes = LocalDate.of(2026, 9, 25)

        assertEquals(listOf(28, 29), siguientesDiasHabiles(viernes, 2).map { it.dayOfMonth })
        assertEquals(LocalDate.of(2026, 9, 18), diaHabilAnterior(martes, 2))
        assertFalse(esDiaHabil(LocalDate.of(2026, 9, 26)))
    }

    @Test
    fun laSemanaVaDeLunesADomingo() {
        assertTrue(mismaSemana(martes, LocalDate.of(2026, 9, 27)))
        assertFalse(mismaSemana(martes, LocalDate.of(2026, 9, 20)))
    }
}
