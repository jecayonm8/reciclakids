package com.reciclakids.services.notificaciones.correos

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FormatoCorreoTest {

    private val f = EspacioFijo

    @Test
    fun rangoDentroDelMismoMesNombraElMesUnaVez() {
        assertEquals(
            "del 15 al 19 de septiembre",
            formatearRangoFechas(LocalDate.of(2025, 9, 15), LocalDate.of(2025, 9, 19)),
        )
    }

    @Test
    fun rangoEntreDosMesesNombraAmbos() {
        assertEquals(
            "del 29 de septiembre al 3 de octubre",
            formatearRangoFechas(LocalDate.of(2025, 9, 29), LocalDate.of(2025, 10, 3)),
        )
    }

    @Test
    fun rangoEntreDosAnosLlevaElAno() {
        assertEquals(
            "del 29 de diciembre de 2025 al 2 de enero de 2026",
            formatearRangoFechas(LocalDate.of(2025, 12, 29), LocalDate.of(2026, 1, 2)),
        )
    }

    @Test
    fun rangoDeUnSoloDia() {
        assertEquals("del 1 de octubre", formatearRangoFechas(LocalDate.of(2025, 10, 1), LocalDate.of(2025, 10, 1)))
    }

    @Test
    fun rangoAlRevesFalla() {
        assertFailsWith<IllegalArgumentException> {
            formatearRangoFechas(LocalDate.of(2025, 9, 19), LocalDate.of(2025, 9, 15))
        }
    }

    @Test
    fun duracionEnHorasYMinutos() {
        assertEquals("1${f}h 12${f}min", formatearDuracion(72))
        assertEquals("45${f}min", formatearDuracion(45))
        assertEquals("2${f}h", formatearDuracion(120))
        assertEquals("0${f}min", formatearDuracion(0))
        assertFailsWith<IllegalArgumentException> { formatearDuracion(-1) }
    }

    @Test
    fun porcentajeConEspacioFijoAntesDelSigno() {
        assertEquals("88${f}%", formatearPorcentaje(88))
    }

    @Test
    fun enumeracionEnEspanol() {
        assertEquals("", unirConY(emptyList()))
        assertEquals("Amiga tortuga", unirConY(listOf("Amiga tortuga")))
        assertEquals("Amiga tortuga y Racha de 5", unirConY(listOf("Amiga tortuga", "Racha de 5")))
        assertEquals(
            "Amiga tortuga, Racha de 5 y Coral feliz",
            unirConY(listOf("Amiga tortuga", "Racha de 5", "Coral feliz")),
        )
    }

    @Test
    fun puntoFinalSoloCuandoFalta() {
        assertEquals("Terminó el reto.", conPuntoFinal("Terminó el reto"))
        assertEquals("Terminó el reto.", conPuntoFinal("Terminó el reto."))
        assertEquals("¡Lo logró!", conPuntoFinal("¡Lo logró!"))
    }
}
