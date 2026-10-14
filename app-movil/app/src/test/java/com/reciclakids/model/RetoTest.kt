package com.reciclakids.model

import com.reciclakids.model.CategoriaResiduo.Aprovechable
import com.reciclakids.model.CategoriaResiduo.NoAprovechable
import com.reciclakids.model.CategoriaResiduo.Organico
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class RetoTest {

    private val hoy = LocalDate.of(2026, 10, 8)

    @Test
    fun lasCanecasDelRetoSonLasDeSusResiduosSiNoSeDicenAparte() {
        val reto = Reto("1111", hoy, Dificultad.Facil, listOf(CatalogoResiduos.botellaPlastica, CatalogoResiduos.cascaraBanano))

        assertEquals(setOf(Aprovechable, Organico), reto.categorias)
        assertEquals(listOf(Aprovechable, Organico), reto.canecas)
    }

    @Test
    fun laDificultadNoCambiaLasCanecasDelJuego() {
        val residuos = listOf(CatalogoResiduos.servilleta, CatalogoResiduos.restosComida)

        Dificultad.entries.forEach { dificultad ->
            assertEquals(listOf(NoAprovechable, Organico), Reto("1", hoy, dificultad, residuos).canecas)
        }
    }

    @Test
    fun unRetoDeUnaSolaCanecaMuestraLasTresParaQueHayaQueElegir() {
        assertEquals(CategoriaResiduo.entries, canecasDelJuego(setOf(Organico)))
        assertEquals(listOf(Aprovechable, NoAprovechable), canecasDelJuego(setOf(NoAprovechable, Aprovechable)))
        assertEquals(CategoriaResiduo.entries, canecasDelJuego(CategoriaResiduo.entries.toSet()))
    }
}
