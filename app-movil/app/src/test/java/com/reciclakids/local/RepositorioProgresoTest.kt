package com.reciclakids.local

import com.reciclakids.contenedorDePrueba
import com.reciclakids.model.ProgresoNino
import com.reciclakids.model.RetosParaAcuarioLimpio
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RepositorioProgresoTest {

    private val repositorio = contenedorDePrueba().progreso

    @Test
    fun unNinoNuevoEmpiezaConElAcuarioTurbioYSinTutorial() = runBlocking {
        val progreso = repositorio.observar("nino-1").first()

        assertEquals(1, progreso.nivelAcuario)
        assertFalse(progreso.tutorialVisto)
    }

    @Test
    fun elTutorialVistoQuedaGuardadoSoloParaEseNino() = runBlocking {
        repositorio.marcarTutorialVisto("nino-1")

        assertTrue(repositorio.leer("nino-1").tutorialVisto)
        assertFalse(repositorio.leer("nino-2").tutorialVisto)
    }

    @Test
    fun elAcuarioSubeDeNivelConLosRetosCompletados() {
        fun nivel(retos: Int) = ProgresoNino("nino-1", retosCompletados = retos).nivelAcuario

        assertEquals(1, nivel(0))
        assertEquals(1, nivel(3))
        assertEquals(2, nivel(4))
        assertEquals(2, nivel(7))
        assertEquals(3, nivel(8))
        assertEquals(3, nivel(12))
        assertEquals(4, nivel(13))
        assertEquals(4, nivel(RetosParaAcuarioLimpio * 3))
    }
}
