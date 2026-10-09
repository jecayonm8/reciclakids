package com.reciclakids.local

import com.reciclakids.contenedorDePrueba
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class RepositorioTiempoTest {

    private val tiempo = contenedorDePrueba().tiempo
    private val hoy = LocalDate.of(2026, 10, 8)

    @Test
    fun elTiempoSeAcumulaPorNinoYPorDia() = runBlocking {
        tiempo.sumar("nino-1", hoy, 5_000)
        tiempo.sumar("nino-1", hoy, 5_000)
        tiempo.sumar("nino-2", hoy, 5_000)
        tiempo.sumar("nino-1", hoy.plusDays(1), 5_000)

        assertEquals(10_000L, tiempo.observar("nino-1", hoy).first())
        assertEquals(5_000L, tiempo.observar("nino-2", hoy).first())
        assertEquals(5_000L, tiempo.observar("nino-1", hoy.plusDays(1)).first())
        assertEquals(0L, tiempo.observar("nino-3", hoy).first())
    }
}
