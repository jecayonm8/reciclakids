package com.reciclakids.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

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

    @Test
    fun lasFechasCortasCabenEnUnChip() {
        assertEquals("8–12 sep", rangoCorto(LocalDate.of(2026, 9, 8), LocalDate.of(2026, 9, 12)))
        assertEquals("29 sep–3 oct", rangoCorto(LocalDate.of(2026, 9, 29), LocalDate.of(2026, 10, 3)))
        assertEquals("12 sep", fechaCorta(LocalDate.of(2026, 9, 12)))
        assertEquals("12 de septiembre de 2026", fechaLarga(LocalDate.of(2026, 9, 12)))
        assertEquals("12 de septiembre", diaYMes(LocalDate.of(2026, 9, 12)))
        assertEquals("jueves", nombreDia(LocalDate.of(2026, 9, 17)))
    }

    @Test
    fun laHoraSeEscribeComoEnColombia() {
        assertEquals("6:10\u00A0p. m.", horaCorta(LocalTime.of(18, 10)))
        assertEquals("12:00\u00A0p. m.", horaCorta(LocalTime.NOON))
        assertEquals("12:05\u00A0a. m.", horaCorta(LocalTime.of(0, 5)))
        assertEquals("9:30\u00A0a. m.", horaCorta(LocalTime.of(9, 30)))
    }

    @Test
    fun laDuracionUsaHorasYMinutos() {
        assertEquals("1 h 12 min", duracionMinutos(72))
        assertEquals("45 min", duracionMinutos(45))
        assertEquals("2 h", duracionMinutos(120))
        assertEquals("0 min", duracionMinutos(0))
    }
}
