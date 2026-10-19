package com.reciclakids.network

import com.reciclakids.contenedorDePrueba
import com.reciclakids.model.CatalogoResiduos
import com.reciclakids.model.CategoriaResiduo
import com.reciclakids.model.Dificultad
import com.reciclakids.model.Insignia
import com.reciclakids.model.Reto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class SincronizadorResultadosTest {

    private val hoy = LocalDate.of(2026, 10, 16)
    private val contenedor = contenedorDePrueba()
    private val reto = Reto("4729", hoy, Dificultad.Medio, CatalogoResiduos.todos.take(6), id = "reto-9")

    /** Un backend falso que guarda lo que recibe, por niño. */
    private class BackendFalso(var respuesta: (String) -> ResultadoEnvio = { ResultadoEnvio.Recibido() }) : ServicioResultados {
        val recibido = mutableListOf<Triple<String, List<PartidaEnviada>, String?>>()

        override suspend fun enviar(ninoId: String, partidas: List<PartidaEnviada>, codigoHoy: String?): ResultadoEnvio {
            val resultado = respuesta(ninoId)
            if (resultado is ResultadoEnvio.Recibido) recibido += Triple(ninoId, partidas, codigoHoy)
            return resultado
        }
    }

    private fun sincronizador(backend: ServicioResultados) = SincronizadorResultados(contenedor.baseDatos, backend) { hoy }

    /** Juega [cuantos] intentos como lo hace el juego; con [error] el primero va a la caneca equivocada. */
    private suspend fun jugar(ninoId: String, cuantos: Int, terminar: Boolean = false, error: Boolean = false): Long {
        val partida = contenedor.juego.abrirPartida(ninoId, reto)
        var estado = partida.estado
        repeat(cuantos) { i ->
            val correcta = checkNotNull(estado.residuoActual).categoria
            val elegida = if (error && i == 0) CategoriaResiduo.entries.first { it != correcta } else correcta
            val intento = estado.intentar(elegida)
            contenedor.juego.registrarIntento(partida.id, intento, elegida, tiempoMs = (i + 1) * 1_000L)
            estado = intento.estado
        }
        if (terminar) contenedor.juego.terminarPartida(partida.id, ninoId, reto, estado, tiempoMs = 30_000)
        return partida.id
    }

    @Test
    fun cadaNinoEnviaSusPartidasCompletasYLoConfirmadoDejaDeEstarPendiente() = runBlocking {
        val backend = BackendFalso()
        jugar("salome", 3, error = true)
        jugar("juan", 6, terminar = true)

        assertTrue(sincronizador(backend).sincronizar())

        assertEquals(listOf("salome", "juan"), backend.recibido.map { it.first })
        val salome = backend.recibido[0].second.single()
        assertEquals("reto-9", salome.retoId)
        assertFalse(salome.terminada)
        // El error también viaja: el backend vuelve a evaluar cada intento.
        assertEquals(3, salome.intentos.size)
        assertFalse(salome.intentos.first().correcto)
        assertEquals(2, salome.aciertos)
        val juan = backend.recibido[1].second.single()
        assertTrue(juan.terminada)
        assertEquals(6, juan.aciertos)
        assertEquals(30_000, juan.tiempoMs)
        // El código de hoy viaja por si hay que abrirle la sesión al niño.
        assertEquals("4729", backend.recibido[0].third)
        assertEquals(0, contenedor.juego.intentosPendientes().first())
        assertTrue(contenedor.baseDatos.juegoDao().partidasPorSincronizar(emptyList(), 10).isEmpty())

        // Nada nuevo: la siguiente vuelta no envía nada.
        assertTrue(sincronizador(backend).sincronizar())
        assertEquals(2, backend.recibido.size)
    }

    @Test
    fun loQueSeJuegaDespuesDeEnviarSaleConLaMismaPartidaYElMismoIdLocal() = runBlocking {
        val backend = BackendFalso()
        val partidaId = jugar("salome", 2)
        sincronizador(backend).sincronizar()

        // La retoma y la termina: se reenvía completa, con el mismo id local para que el backend no la duplique.
        val abierta = contenedor.juego.abrirPartida("salome", reto)
        assertEquals(partidaId, abierta.id)
        var estado = abierta.estado
        while (!estado.terminada) {
            val intento = estado.intentar(checkNotNull(estado.residuoActual).categoria)
            contenedor.juego.registrarIntento(abierta.id, intento, intento.residuo.categoria, tiempoMs = 0)
            estado = intento.estado
        }
        contenedor.juego.terminarPartida(abierta.id, "salome", reto, estado, tiempoMs = 40_000)
        sincronizador(backend).sincronizar()

        val (primera, segunda) = backend.recibido.map { it.second.single() }
        assertEquals(primera.idLocal, segunda.idLocal)
        assertTrue(segunda.terminada)
        assertEquals(6, segunda.intentos.size)
        assertTrue(checkNotNull(contenedor.baseDatos.juegoDao().leerPartida(partidaId)).terminadaEnviada)
    }

    @Test
    fun siElEnvioFallaNadaSePierdeYSeReintenta() = runBlocking {
        jugar("salome", 2)

        assertFalse(sincronizador(BackendFalso { ResultadoEnvio.SinConexion }).sincronizar())

        assertEquals(2, contenedor.juego.intentosPendientes().first())
        // Al volver la red, sale todo.
        val backend = BackendFalso()
        assertTrue(sincronizador(backend).sincronizar())
        assertEquals(2, backend.recibido.single().second.single().intentos.size)
    }

    @Test
    fun unNinoSinSesionNoFrenaALosDemasYSusPartidasSiguenGuardadas() = runBlocking {
        jugar("salome", 2)
        jugar("juan", 1)
        val backend = BackendFalso { nino -> if (nino == "salome") ResultadoEnvio.SinSesion else ResultadoEnvio.Recibido() }

        // Lo de Salomé espera a que vuelva a entrar con un código: no hay nada que reintentar ahora.
        assertTrue(sincronizador(backend).sincronizar())

        assertEquals(listOf("juan"), backend.recibido.map { it.first })
        assertEquals(2, contenedor.juego.intentosPendientes().first())
    }

    @Test
    fun lasInsigniasQueOtorgaElBackendQuedanEnElTelefono() = runBlocking {
        jugar("salome", 6, terminar = true)

        sincronizador(BackendFalso { ResultadoEnvio.Recibido(listOf(Insignia.GuardianDelMar)) }).sincronizar()

        assertTrue(Insignia.GuardianDelMar in contenedor.juego.insigniasGanadas("salome").first())
    }

    @Test
    fun deUnDiaAnteriorNoHayCodigoParaAbrirSesion() = runBlocking {
        val ayer = reto.copy(fecha = hoy.minusDays(1))
        val partida = contenedor.juego.abrirPartida("salome", ayer)
        val intento = partida.estado.intentar(checkNotNull(partida.estado.residuoActual).categoria)
        contenedor.juego.registrarIntento(partida.id, intento, intento.residuo.categoria, tiempoMs = 0)
        val backend = BackendFalso()

        sincronizador(backend).sincronizar()

        assertNull(backend.recibido.single().third)
    }
}
