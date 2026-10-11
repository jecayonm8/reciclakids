package com.reciclakids.network

import com.reciclakids.model.CategoriaResiduo
import com.reciclakids.model.Dificultad
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ServicioRetosEnMemoriaTest {

    private val servicio = ServicioRetosEnMemoria(latenciaMs = 0)
    private val hoy = LocalDate.of(2026, 10, 8)

    private fun reto(codigo: String) = runBlocking {
        (servicio.buscarReto(codigo, hoy) as ResultadoCodigo.Valido).reto
    }

    @Test
    fun facilSoloUsaLasCanecasBlancaYVerde() {
        val reto = reto("1111")

        assertEquals(Dificultad.Facil, reto.dificultad)
        assertEquals(4, reto.residuos.size)
        assertTrue(reto.residuos.none { it.categoria == CategoriaResiduo.NoAprovechable })
    }

    @Test
    fun medioUsaLasTresCanecasYDificilTieneMasObjetos() {
        val medio = reto("4729")
        val dificil = reto("9999")

        assertEquals(CategoriaResiduo.entries.toSet(), medio.residuos.map { it.categoria }.toSet())
        assertTrue(dificil.residuos.size > medio.residuos.size)
        assertTrue(dificil.dificultad.conTiempo)
    }

    @Test
    fun unCodigoSoloValeElDiaEnQueSeCreo() = runBlocking {
        assertEquals(ResultadoCodigo.Vencido, servicio.buscarReto("1234", hoy))
        assertEquals(hoy, reto("4729").fecha)
    }

    @Test
    fun elGrupoSoloExponeNombreEInicialDelApellido() = runBlocking {
        val ninos = servicio.ninosDelGrupo(reto("4729"))

        assertTrue(ninos.isNotEmpty())
        assertTrue(ninos.all { it.nombre.matches(Regex("""\p{L}+ \p{Lu}\.""")) })
    }
}
