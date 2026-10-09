package com.reciclakids.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InsigniaTest {

    private fun partida(errores: Int = 0, rachaMaxima: Int = 3) =
        EstadoPartida(CatalogoResiduos.todos, indice = 8, aciertos = 8, errores = errores, rachaMaxima = rachaMaxima)

    private fun nuevas(
        estado: EstadoPartida = partida(),
        dificultad: Dificultad = Dificultad.Medio,
        aTiempo: Boolean = true,
        retos: Int = 1,
        dias: Int = 1,
        ya: Set<Insignia> = emptySet(),
    ) = insigniasNuevas(estado, dificultad, aTiempo, ProgresoNino("n", retosCompletados = retos), dias, ya)

    @Test
    fun sinErroresSeGanaAmigaTortuga() {
        assertTrue(Insignia.AmigaTortuga in nuevas(partida(errores = 0)))
        assertFalse(Insignia.AmigaTortuga in nuevas(partida(errores = 1)))
    }

    @Test
    fun rapidoComoPezSoloEnDificilYATiempo() {
        assertTrue(Insignia.RapidoComoPez in nuevas(dificultad = Dificultad.Dificil, aTiempo = true))
        assertFalse(Insignia.RapidoComoPez in nuevas(dificultad = Dificultad.Dificil, aTiempo = false))
        assertFalse(Insignia.RapidoComoPez in nuevas(dificultad = Dificultad.Medio, aTiempo = true))
    }

    @Test
    fun lasRachasDanSusInsignias() {
        assertEquals(
            listOf(Insignia.RachaDeCinco, Insignia.CoralFeliz),
            nuevas(partida(errores = 1, rachaMaxima = 10)).filter { it == Insignia.RachaDeCinco || it == Insignia.CoralFeliz },
        )
    }

    @Test
    fun lasInsigniasDeConstanciaDependenDeRetosYDias() {
        val conMucho = nuevas(partida(errores = 1), retos = RetosParaGuardian, dias = DiasParaCincoRetos)

        assertTrue(conMucho.containsAll(listOf(Insignia.CincoRetosDiarios, Insignia.PulpoOrdenado, Insignia.AguaCristalina, Insignia.GuardianDelMar)))
        assertTrue(nuevas(partida(errores = 1), retos = 4, dias = 2).isEmpty())
    }

    @Test
    fun laColeccionDiceCuantoFaltaSoloEnLasDeConstancia() {
        val progreso = ProgresoNino("n", retosCompletados = 8)

        assertEquals(Faltante(2, enDias = false), Insignia.PulpoOrdenado.faltante(progreso, diasConReto = 3))
        assertEquals(Faltante(12, enDias = false), Insignia.GuardianDelMar.faltante(progreso, diasConReto = 3))
        assertEquals(Faltante(2, enDias = true), Insignia.CincoRetosDiarios.faltante(progreso, diasConReto = 3))
        assertEquals(null, Insignia.AmigaTortuga.faltante(progreso, diasConReto = 3))
    }

    @Test
    fun unaInsigniaNoSeOtorgaDosVeces() {
        assertFalse(Insignia.AmigaTortuga in nuevas(ya = setOf(Insignia.AmigaTortuga)))
    }
}
