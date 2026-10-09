package com.reciclakids.ui.nino

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.reciclakids.local.RepositorioTiempo
import com.reciclakids.model.ControlParental
import com.reciclakids.model.Nino
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/** Cada cuánto se suma el tiempo jugado. */
const val PasoTiempoJuegoMs = 5_000L

/**
 * Cuenta el tiempo que el niño pasa en el Modo Niño mientras la app está en primer plano y, al
 * llegar al límite diario del control parental, llama a [onAgotado] (UI-19). El tiempo se
 * guarda por niño y por día, así que cerrar y abrir la app no lo reinicia.
 */
@Composable
fun VigilanteTiempoJuego(
    nino: Nino,
    tiempo: RepositorioTiempo,
    control: ControlParental,
    onAgotado: () -> Unit,
    hoy: () -> LocalDate = LocalDate::now,
) {
    val agotado by rememberUpdatedState(onAgotado)
    val ciclo = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(nino.id, control.limiteMs) {
        ciclo.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var jugado = tiempo.observar(nino.id, hoy()).first()
            while (jugado < control.limiteMs) {
                delay(PasoTiempoJuegoMs)
                tiempo.sumar(nino.id, hoy(), PasoTiempoJuegoMs)
                jugado = tiempo.observar(nino.id, hoy()).first()
            }
            agotado()
        }
    }
}
