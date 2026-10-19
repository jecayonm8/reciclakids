package com.reciclakids.viewmodel

import com.reciclakids.contenedorDePrueba
import com.reciclakids.model.CatalogoResiduos
import com.reciclakids.model.Dificultad
import com.reciclakids.model.Nino
import com.reciclakids.model.Reto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

/** El tiempo jugado viaja al backend: de él salen los minutos que ven la docente y el acudiente. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class JuegoViewModelTest {

    private val contenedor = contenedorDePrueba()
    private val reto = Reto("4729", LocalDate.of(2026, 10, 16), Dificultad.Medio, CatalogoResiduos.todos.take(6), id = "reto-9")

    @Before
    fun relojDePrueba() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun restaurar() {
        Dispatchers.resetMain()
    }

    private fun TestScope.esperar(ms: Long) {
        advanceTimeBy(ms)
        runCurrent()
    }

    @Test
    fun enDificultadMedioTambienSeMideElTiempoSinContarLaPausa() = runTest {
        val juego = JuegoViewModel(reto, Nino("n1", "Salomé M."), contenedor.juego)
        esperar(5_000)
        // Medio no tiene temporizador visual: el tiempo se mide igual.
        assertNull(juego.fraccionTiempoRestante)

        juego.pausar()
        esperar(10_000)
        juego.reanudar()
        while (juego.resultado == null) {
            juego.clasificar(checkNotNull(juego.estado?.residuoActual).categoria)
            esperar(DuracionAciertoMs + 1_000)
        }

        val partida = checkNotNull(contenedor.baseDatos.juegoDao().leerPartida(1))
        assertTrue(partida.terminada)
        // Unos 10 s: 5 antes de la pausa y 1 tras cada acierto. Con la pausa serían 20; antes del arreglo, 0.
        assertTrue("tiempo guardado: ${partida.tiempoMs}", partida.tiempoMs in 8_000L..12_000L)
        assertNotNull(juego.resultado)
    }
}
