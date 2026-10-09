package com.reciclakids.viewmodel

import com.reciclakids.model.CategoriaResiduo
import com.reciclakids.model.Confusion
import com.reciclakids.model.DetalleNino
import com.reciclakids.model.Dificultad
import com.reciclakids.model.EstadoReto
import com.reciclakids.model.Reto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class FiltrosYMensajesTest {

    private val martes = LocalDate.of(2026, 9, 22)

    private fun reto(id: String, categorias: Set<CategoriaResiduo>, dificultad: Dificultad, fecha: LocalDate?) =
        Reto(id, id, categorias, dificultad, fecha, EstadoReto.Publicado)

    private val retos = listOf(
        reto("lonchera", setOf(CategoriaResiduo.Aprovechables, CategoriaResiduo.Organicos), Dificultad.Medio, martes),
        reto("refrigerio", setOf(CategoriaResiduo.Organicos), Dificultad.Facil, martes.plusDays(1)),
        reto("no-reciclable", setOf(CategoriaResiduo.NoAprovechables), Dificultad.Dificil, LocalDate.of(2026, 9, 18)),
        reto("mezcla", CategoriaResiduo.entries.toSet(), Dificultad.Dificil, null),
    )

    private fun ids(filtro: FiltroReto) = retos.filtrados(filtro, martes).map { it.id }

    @Test
    fun cadaFiltroDejaSoloLosRetosQueCoinciden() {
        assertEquals(retos.map { it.id }, ids(FiltroReto.Todos))
        assertEquals(listOf("lonchera", "mezcla"), ids(FiltroReto.Aprovechables))
        assertEquals(listOf("no-reciclable", "mezcla"), ids(FiltroReto.NoAprovechables))
        assertEquals(listOf("lonchera", "refrigerio", "mezcla"), ids(FiltroReto.Organicos))
        assertEquals(listOf("refrigerio"), ids(FiltroReto.Facil))
        assertEquals(listOf("lonchera"), ids(FiltroReto.Medio))
    }

    @Test
    fun estaSemanaVaDeLunesADomingoYDejaFueraLosBorradoresSinFecha() {
        // El jueves 18 es de la semana anterior al martes 22.
        assertEquals(listOf("lonchera", "refrigerio"), ids(FiltroReto.EstaSemana))
    }

    private fun detalle(
        semanas: List<Int> = listOf(62, 71, 68, 74),
        porCategoria: Map<CategoriaResiduo, Int> = mapOf(
            CategoriaResiduo.Aprovechables to 94,
            CategoriaResiduo.NoAprovechables to 61,
            CategoriaResiduo.Organicos to 88,
        ),
        confusion: Confusion? = null,
    ) = DetalleNino("n02", "Juan T.", semanas, porCategoria, confusion, emptyList())

    @Test
    fun laConfusionConcretaVaPrimero() {
        val confusion = Confusion("el empaque metalizado", CategoriaResiduo.NoAprovechables, CategoriaResiduo.Aprovechables)

        assertEquals(MensajeDetalleNino.Confunde(confusion), detalle(confusion = confusion).mensaje())
    }

    @Test
    fun sinConfusionSeNombraLaCanecaMasDificilSiEstaPorDebajoDe80() {
        assertEquals(MensajeDetalleNino.CanecaDificil(CategoriaResiduo.NoAprovechables, 61), detalle().mensaje())
    }

    @Test
    fun siTodasLasCanecasPasanDe80SeDiceQueVaBien() {
        val bien = detalle(porCategoria = CategoriaResiduo.entries.associateWith { 90 })

        assertEquals(MensajeDetalleNino.VaMuyBien, bien.mensaje())
    }

    @Test
    fun sinResultadosNoHayMensajeNiCambio() {
        val nuevo = detalle(semanas = emptyList(), porCategoria = emptyMap())

        assertNull(nuevo.mensaje())
        assertNull(nuevo.cambioAciertos)
    }

    @Test
    fun elCambioEsEntreLaPrimeraYLaUltimaSemana() {
        assertEquals(12, detalle().cambioAciertos)
        assertEquals(-5, detalle(semanas = listOf(80, 75)).cambioAciertos)
    }
}
