package com.reciclakids.local

import com.reciclakids.contenedorDePrueba
import com.reciclakids.model.CatalogoResiduos
import com.reciclakids.model.CategoriaResiduo
import com.reciclakids.model.Dificultad
import com.reciclakids.model.EstadoPartida
import com.reciclakids.model.Insignia
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
class RepositorioJuegoTest {

    private val contenedor = contenedorDePrueba()
    private val juego = contenedor.juego
    private val reto = Reto("4729", LocalDate.of(2026, 10, 8), Dificultad.Medio, CatalogoResiduos.todos.take(6))

    /** Clasifica bien [cuantos] residuos y los guarda uno a uno, como el juego. */
    private suspend fun jugar(partidaId: Long, desde: EstadoPartida, cuantos: Int): EstadoPartida {
        var estado = desde
        repeat(cuantos) {
            val intento = estado.intentar(checkNotNull(estado.residuoActual).categoria)
            juego.registrarIntento(partidaId, intento, intento.residuo.categoria, tiempoMs = 0)
            estado = intento.estado
        }
        return estado
    }

    @Test
    fun siLaAppSeCierraLaPartidaSeRetomaDondeQuedo() = runBlocking {
        val partida = juego.abrirPartida("nino-1", reto)
        jugar(partida.id, partida.estado, cuantos = 2)

        // Otra apertura, como tras un cierre inesperado.
        val retomada = juego.abrirPartida("nino-1", reto)

        assertEquals(partida.id, retomada.id)
        assertEquals(2, retomada.estado.indice)
        assertEquals(20, retomada.estado.puntaje)
    }

    @Test
    fun cadaIntentoQuedaPendienteDeSincronizar() = runBlocking {
        val partida = juego.abrirPartida("nino-1", reto)
        val intento = partida.estado.intentar(CategoriaResiduo.Organico)
        juego.registrarIntento(partida.id, intento, CategoriaResiduo.Organico, tiempoMs = 0)

        assertEquals(1, juego.intentosPendientes().first())
    }

    @Test
    fun terminarSumaAlAcuarioUnaSolaVezPorRetoYOtorgaInsignias() = runBlocking {
        val primera = juego.abrirPartida("nino-1", reto)
        val final = jugar(primera.id, primera.estado, reto.residuos.size)
        val resultado = juego.terminarPartida(primera.id, "nino-1", reto, final, tiempoMs = 0)

        assertTrue(resultado.sumoAlAcuario)
        assertTrue(Insignia.AmigaTortuga in resultado.insigniasNuevas)
        assertEquals(1, contenedor.progreso.leer("nino-1").retosCompletados)
        assertTrue(contenedor.progreso.leer("nino-1").completo(reto))

        val repeticion = juego.abrirPartida("nino-1", reto)
        val otraVez = juego.terminarPartida(
            repeticion.id, "nino-1", reto, jugar(repeticion.id, repeticion.estado, reto.residuos.size), tiempoMs = 0,
        )

        assertFalse(otraVez.sumoAlAcuario)
        assertFalse(Insignia.AmigaTortuga in otraVez.insigniasNuevas)
        assertEquals(1, contenedor.progreso.leer("nino-1").retosCompletados)
        // Las dos partidas sin errores suman 12 residuos seguidos entre retos: también «Coral feliz».
        assertEquals(setOf(Insignia.AmigaTortuga, Insignia.RachaDeCinco, Insignia.CoralFeliz), juego.insigniasGanadas("nino-1").first())
    }

    @Test
    fun laRachaEntreRetosSigueEnLaPartidaSiguienteYUnErrorLaCorta() = runBlocking {
        val primera = juego.abrirPartida("nino-1", reto)
        juego.terminarPartida(primera.id, "nino-1", reto, jugar(primera.id, primera.estado, reto.residuos.size), tiempoMs = 0)
        val otroReto = reto.copy(codigo = "1111", fecha = reto.fecha.plusDays(1))

        val segunda = juego.abrirPartida("nino-1", otroReto)
        // El contador del juego empieza en 0 en cada partida.
        assertEquals(0, segunda.estado.racha)
        val conCuatro = jugar(segunda.id, segunda.estado, cuantos = 4)

        assertEquals(10, contenedor.progreso.leer("nino-1").rachaEntreRetos)
        assertEquals(4, conCuatro.racha)

        val equivocada = CategoriaResiduo.entries.first { it != conCuatro.residuoActual?.categoria }
        juego.registrarIntento(segunda.id, conCuatro.intentar(equivocada), equivocada, tiempoMs = 0)

        assertEquals(0, contenedor.progreso.leer("nino-1").rachaEntreRetos)
        assertEquals(10, contenedor.progreso.leer("nino-1").mejorRachaEntreRetos)
    }

    @Test
    fun reiniciarEmpiezaDesdeElPrimerResiduo() = runBlocking {
        val partida = juego.abrirPartida("nino-1", reto)
        jugar(partida.id, partida.estado, cuantos = 3)

        val nueva = juego.reiniciarPartida(partida.id, "nino-1", reto)

        assertTrue(nueva.id != partida.id)
        assertEquals(0, nueva.estado.indice)
        assertEquals(0, juego.abrirPartida("nino-1", reto).estado.indice)
    }
}
