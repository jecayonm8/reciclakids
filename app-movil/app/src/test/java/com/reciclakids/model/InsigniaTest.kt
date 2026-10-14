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
        mejorRacha: Int = 0,
    ) = insigniasNuevas(
        estado, dificultad, aTiempo, ProgresoNino("n", retosCompletados = retos, mejorRachaEntreRetos = mejorRacha), dias, ya,
    )

    private fun rachas(insignias: List<Insignia>) = insignias.filter { it == Insignia.RachaDeCinco || it == Insignia.CoralFeliz }

    /** Lleva [intentos] por la racha entre retos, como lo hace cada intento guardado. */
    private fun ProgresoNino.tras(vararg intentos: Boolean) = intentos.fold(this) { progreso, correcto -> progreso.conIntento(correcto) }

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
    fun lasRachasDanSusInsigniasConLaRachaEntreRetos() {
        assertEquals(listOf(Insignia.RachaDeCinco, Insignia.CoralFeliz), rachas(nuevas(partida(errores = 1), mejorRacha = 10)))
        assertEquals(listOf(Insignia.RachaDeCinco), rachas(nuevas(mejorRacha = 5)))
        // La racha de una sola partida ya no cuenta: sin racha entre retos no hay insignia.
        assertEquals(emptyList<Insignia>(), rachas(nuevas(partida(rachaMaxima = 10), mejorRacha = 4)))
    }

    @Test
    fun laRachaEntreRetosSigueDeUnaPartidaAOtraYUnErrorLaCorta() {
        val dosRetos = ProgresoNino("n").tras(*BooleanArray(6) { true }).tras(*BooleanArray(4) { true })

        assertEquals(10, dosRetos.rachaEntreRetos)

        val trasError = dosRetos.tras(false, true)

        assertEquals(1, trasError.rachaEntreRetos)
        assertEquals(10, trasError.mejorRachaEntreRetos)
    }

    @Test
    fun coralFelizSeAlcanzaUniendoDosRetosMediosSinErrores() {
        // Un reto medio tiene 6 residuos: sola, ninguna partida llega a 10 seguidos.
        val primero = ProgresoNino("n").tras(*BooleanArray(6) { true })
        val segundo = primero.tras(*BooleanArray(6) { true })

        assertFalse(Insignia.CoralFeliz in insigniasNuevas(partida(rachaMaxima = 6), Dificultad.Medio, true, primero, 1, emptySet()))
        assertTrue(Insignia.CoralFeliz in insigniasNuevas(partida(rachaMaxima = 6), Dificultad.Medio, true, segundo, 2, emptySet()))
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
