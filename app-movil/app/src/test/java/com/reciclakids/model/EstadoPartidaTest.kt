package com.reciclakids.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class EstadoPartidaTest {

    private val residuos = CatalogoResiduos.todos.take(6)

    private fun EstadoPartida.acertar() = intentar(checkNotNull(residuoActual).categoria).estado

    private fun EstadoPartida.fallar(): EstadoPartida {
        val correcta = checkNotNull(residuoActual).categoria
        return intentar(CategoriaResiduo.entries.first { it != correcta }).estado
    }

    @Test
    fun cadaAciertoSumaDiezYDesdeElTerceroSeguidoVeinte() {
        var estado = EstadoPartida(residuos)
        val puntos = (1..4).map {
            val antes = estado.puntaje
            estado = estado.acertar()
            estado.puntaje - antes
        }

        assertEquals(listOf(10, 10, 20, 20), puntos)
    }

    @Test
    fun unErrorNoRestaNiBorraLaRacha() {
        val conRacha = EstadoPartida(residuos).acertar().acertar()

        val trasError = conRacha.fallar()

        assertEquals(conRacha.puntaje, trasError.puntaje)
        assertEquals(conRacha.racha, trasError.racha)
        assertEquals(conRacha.indice, trasError.indice)
        assertEquals(1, trasError.errores)
    }

    @Test
    fun losErroresDelResiduoActualSeReinicianAlAcertar() {
        val dosErrores = EstadoPartida(residuos).fallar().fallar()
        assertEquals(2, dosErrores.erroresResiduoActual)

        assertEquals(0, dosErrores.acertar().erroresResiduoActual)
    }

    @Test
    fun laPartidaTerminaAlClasificarElUltimoResiduo() {
        var estado = EstadoPartida(residuos)
        repeat(residuos.size - 1) { estado = estado.acertar() }
        assertFalse(estado.terminada)

        estado = estado.acertar()

        assertTrue(estado.terminada)
        assertEquals(residuos.size, estado.rachaMaxima)
    }

    /** RNF-02: la evaluación acierta en más del 99 % frente a un set de prueba de 500 intentos. */
    @Test
    fun laEvaluacionCoincideConElCodigoDeColoresEnQuinientosIntentos() {
        val azar = Random(2184)
        val fallos = (1..500).count {
            val residuo = CatalogoResiduos.todos.random(azar)
            val elegida = CategoriaResiduo.entries.random(azar)
            val intento = EstadoPartida(listOf(residuo)).intentar(elegida)
            intento.correcto != (residuo.categoria == elegida)
        }

        assertEquals(0, fallos)
    }

    @Test
    fun siempreHayAlMenosUnaEstrella() {
        fun estrellas(errores: Int) = ResultadoReto(6, errores, 0, sumoAlAcuario = true, insigniasNuevas = emptyList()).estrellas

        assertEquals(3, estrellas(0))
        assertEquals(2, estrellas(2))
        assertEquals(1, estrellas(30))
    }
}
