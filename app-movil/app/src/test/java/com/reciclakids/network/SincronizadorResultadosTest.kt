package com.reciclakids.network

import com.reciclakids.contenedorDePrueba
import com.reciclakids.model.CatalogoResiduos
import com.reciclakids.model.Dificultad
import com.reciclakids.model.Reto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class SincronizadorResultadosTest {

    private val contenedor = contenedorDePrueba()
    private val reto = Reto("4729", LocalDate.of(2026, 10, 8), Dificultad.Medio, CatalogoResiduos.todos.take(6))

    /** Guarda [cuantos] intentos como lo hace el juego. */
    private suspend fun jugar(cuantos: Int) {
        val partida = contenedor.juego.abrirPartida("nino-1", reto)
        var estado = partida.estado
        repeat(cuantos) {
            val intento = estado.intentar(checkNotNull(estado.residuoActual).categoria)
            contenedor.juego.registrarIntento(partida.id, intento, intento.residuo.categoria, tiempoMs = 0)
            estado = intento.estado
        }
    }

    @Test
    fun loQueElBackendConfirmaDejaDeEstarPendiente() = runBlocking {
        val recibidos = mutableListOf<IntentoEnviado>()
        val servicio = object : ServicioResultados {
            override suspend fun enviar(intentos: List<IntentoEnviado>): Boolean {
                recibidos += intentos
                return true
            }
        }
        jugar(3)

        assertTrue(SincronizadorResultados(contenedor.baseDatos, servicio).sincronizar())

        assertEquals(0, contenedor.juego.intentosPendientes().first())
        assertEquals(3, recibidos.size)
        assertTrue(recibidos.all { it.ninoId == "nino-1" && it.codigoReto == "4729" && it.correcto })
    }

    @Test
    fun siElEnvioFallaNadaSePierdeYSeReintenta() = runBlocking {
        val servicio = object : ServicioResultados {
            override suspend fun enviar(intentos: List<IntentoEnviado>) = false
        }
        jugar(2)

        assertFalse(SincronizadorResultados(contenedor.baseDatos, servicio).sincronizar())

        assertEquals(2, contenedor.juego.intentosPendientes().first())
    }
}
