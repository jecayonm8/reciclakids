package com.reciclakids.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reciclakids.local.PartidaAbierta
import com.reciclakids.local.RepositorioJuego
import com.reciclakids.model.CategoriaResiduo
import com.reciclakids.model.EstadoPartida
import com.reciclakids.model.LimiteDificilMs
import com.reciclakids.model.Nino
import com.reciclakids.model.Residuo
import com.reciclakids.model.ResultadoReto
import com.reciclakids.model.Reto
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class Retroalimentacion { Ninguna, Acierto, Rebote }

/** Lo que dura la celebración de un acierto antes de pasar al siguiente residuo. */
const val DuracionAciertoMs = 1000L

/** Lo que dura el rebote tras un error: el residuo vuelve y la caneca correcta brilla. */
const val DuracionReboteMs = 900L

private const val PasoCronometroMs = 250L

/**
 * UI-12 Juego. La retroalimentación arranca en el mismo instante del intento y el guardado en
 * el teléfono va detrás, para cumplir los 500 ms del RNF sin depender del disco.
 */
class JuegoViewModel(
    private val reto: Reto,
    private val nino: Nino,
    private val repositorio: RepositorioJuego,
    /** Se llama con la partida ya guardada como terminada; programa la sincronización. */
    private val alTerminar: () -> Unit = {},
) : ViewModel() {

    /** null mientras se abre la partida. */
    var estado by mutableStateOf<EstadoPartida?>(null)
        private set

    /** Residuo en pantalla. Tras un acierto sigue siendo el anterior hasta que acaba la celebración. */
    var residuoEnPantalla by mutableStateOf<Residuo?>(null)
        private set
    var retroalimentacion by mutableStateOf(Retroalimentacion.Ninguna)
        private set
    var pausado by mutableStateOf(false)
        private set

    /** Tiempo jugado sin contar las pausas. Solo corre en dificultad difícil. */
    var tiempoMs by mutableLongStateOf(0L)
        private set

    /** Se llena al terminar el último residuo; la pantalla pasa entonces al resultado (UI-15). */
    var resultado by mutableStateOf<ResultadoReto?>(null)
        private set

    val fraccionTiempoRestante: Float?
        get() = if (reto.dificultad.conTiempo) (1f - tiempoMs.toFloat() / LimiteDificilMs).coerceIn(0f, 1f) else null

    private var partidaId = 0L
    private var cronometro: Job? = null

    /** Las escrituras se hacen en orden: un intento nunca se guarda después del cierre de la partida. */
    private val escrituras = Mutex()

    init {
        viewModelScope.launch { abrir(repositorio.abrirPartida(nino.id, reto)) }
    }

    fun clasificar(categoria: CategoriaResiduo) {
        val actual = estado ?: return
        if (retroalimentacion != Retroalimentacion.Ninguna || pausado || actual.terminada) return

        val intento = actual.intentar(categoria)
        estado = intento.estado
        retroalimentacion = if (intento.correcto) Retroalimentacion.Acierto else Retroalimentacion.Rebote

        viewModelScope.launch {
            escrituras.withLock { repositorio.registrarIntento(partidaId, intento, categoria, tiempoMs) }
        }
        viewModelScope.launch {
            delay(if (intento.correcto) DuracionAciertoMs else DuracionReboteMs)
            retroalimentacion = Retroalimentacion.Ninguna
            residuoEnPantalla = intento.estado.residuoActual
            if (intento.estado.terminada) terminar(intento.estado)
        }
    }

    fun pausar() {
        if (resultado == null) pausado = true
    }

    fun reanudar() {
        pausado = false
    }

    fun reiniciar() {
        viewModelScope.launch {
            val nueva = escrituras.withLock { repositorio.reiniciarPartida(partidaId, nino.id, reto) }
            retroalimentacion = Retroalimentacion.Ninguna
            pausado = false
            abrir(nueva)
        }
    }

    private fun abrir(partida: PartidaAbierta) {
        partidaId = partida.id
        estado = partida.estado
        residuoEnPantalla = partida.estado.residuoActual
        tiempoMs = partida.tiempoMs
        if (partida.estado.terminada) {
            terminar(partida.estado)
        } else if (reto.dificultad.conTiempo) {
            arrancarCronometro()
        }
    }

    private fun arrancarCronometro() {
        cronometro?.cancel()
        cronometro = viewModelScope.launch {
            while (isActive) {
                delay(PasoCronometroMs)
                if (!pausado && retroalimentacion != Retroalimentacion.Acierto) tiempoMs += PasoCronometroMs
            }
        }
    }

    private fun terminar(final: EstadoPartida) {
        cronometro?.cancel()
        viewModelScope.launch {
            resultado = escrituras.withLock {
                repositorio.terminarPartida(partidaId, nino.id, reto, final, tiempoMs)
            }
            alTerminar()
        }
    }
}
