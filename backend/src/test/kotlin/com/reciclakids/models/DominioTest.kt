package com.reciclakids.models

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DominioTest {

    private val sinHistorial = HistorialNino(retosCompletados = 1, diasConReto = 1, mejorRachaEntreRetos = 0)

    @Test
    fun sinErroresDaAmigaTortuga() {
        val insignias = insigniasCumplidas(errores = 0, dificultad = Dificultad.Medio, tiempoMs = 60_000, historial = sinHistorial)
        assertEquals(setOf(Insignia.AmigaTortuga), insignias)
    }

    @Test
    fun rapidoComoPezSoloEnDificilYATiempo() {
        assertTrue(Insignia.RapidoComoPez in insigniasCumplidas(1, Dificultad.Dificil, LimiteDificilMs, sinHistorial))
        assertTrue(Insignia.RapidoComoPez !in insigniasCumplidas(1, Dificultad.Dificil, LimiteDificilMs + 1, sinHistorial))
        assertTrue(Insignia.RapidoComoPez !in insigniasCumplidas(1, Dificultad.Medio, 10_000, sinHistorial))
        // Una partida sin tiempo registrado no cuenta como «a tiempo».
        assertTrue(Insignia.RapidoComoPez !in insigniasCumplidas(1, Dificultad.Dificil, 0, sinHistorial))
    }

    @Test
    fun lasRachasSeMidenEntreRetos() {
        val cinco = insigniasCumplidas(2, Dificultad.Medio, 1, sinHistorial.copy(mejorRachaEntreRetos = 5))
        assertEquals(setOf(Insignia.RachaDeCinco), cinco)
        val diez = insigniasCumplidas(2, Dificultad.Medio, 1, sinHistorial.copy(mejorRachaEntreRetos = 10))
        assertEquals(setOf(Insignia.RachaDeCinco, Insignia.CoralFeliz), diez)
    }

    @Test
    fun insigniasDeConstancia() {
        val historial = HistorialNino(retosCompletados = 20, diasConReto = 5, mejorRachaEntreRetos = 0)
        val insignias = insigniasCumplidas(1, Dificultad.Facil, 1, historial)
        assertEquals(
            setOf(Insignia.CincoRetosDiarios, Insignia.PulpoOrdenado, Insignia.AguaCristalina, Insignia.GuardianDelMar),
            insignias,
        )
    }

    @Test
    fun residuosDelRetoSalenDeLasCategoriasElegidas() {
        val residuos = residuosDelReto(setOf(CategoriaResiduo.Organico), Dificultad.Medio, Random(7))
        assertEquals(6, residuos.size)
        assertTrue(residuos.all { it.categoria == CategoriaResiduo.Organico })
        // Orgánicos tiene 2 residuos en el catálogo: para llegar a 6 se repiten.
        assertEquals(2, residuos.map { it.id }.toSet().size)
    }

    @Test
    fun cadaDificultadTraeSuCantidadDeResiduos() {
        val todas = CategoriaResiduo.entries.toSet()
        for (dificultad in Dificultad.entries) {
            assertEquals(dificultad.residuosPorReto, residuosDelReto(todas, dificultad, Random(1)).size)
        }
        assertFailsWith<IllegalArgumentException> { residuosDelReto(emptySet(), Dificultad.Facil, Random(1)) }
    }

    @Test
    fun lasCategoriasIdaYVueltaPorLaColumna() {
        val categorias = setOf(CategoriaResiduo.Organico, CategoriaResiduo.Aprovechable)
        assertEquals("Aprovechable,Organico", categorias.aColumna())
        assertEquals(categorias, categoriasDeColumna(categorias.aColumna()))
        assertEquals(Dificultad.Dificil, dificultadDeColumna(Dificultad.Dificil.aColumna()))
        assertEquals(EstadoReto.Publicado, estadoDeColumna("PUBLICADO"))
    }

    @Test
    fun elCatalogoTieneLosMismosIdQueLaApp() {
        assertEquals(
            listOf("botella", "cascara", "servilleta", "lata", "restos", "empaque", "papel", "carton"),
            CatalogoResiduos.todos.map { it.id },
        )
    }
}
