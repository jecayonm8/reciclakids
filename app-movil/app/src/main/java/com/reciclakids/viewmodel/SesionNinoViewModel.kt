package com.reciclakids.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.reciclakids.model.Nino
import com.reciclakids.model.ResultadoReto
import com.reciclakids.model.Reto

/**
 * Sesión del niño: el reto del día que abrió con el código y el avatar que eligió.
 * Vive mientras dure el grafo del Modo Niño.
 */
class SesionNinoViewModel : ViewModel() {
    var reto by mutableStateOf<Reto?>(null)
        private set
    var nino by mutableStateOf<Nino?>(null)
        private set

    /** Resultado del último reto terminado, para el resultado (UI-15) y la insignia (UI-16). */
    var ultimoResultado by mutableStateOf<ResultadoReto?>(null)
        private set

    /** El último reto fue de práctica: no se guardó, no sumó al acuario ni dio insignias. */
    var ultimoFuePractica by mutableStateOf(false)
        private set

    fun abrirReto(reto: Reto) {
        this.reto = reto
        nino = null
    }

    fun elegirNino(nino: Nino) {
        this.nino = nino
    }

    fun terminarReto(resultado: ResultadoReto, practica: Boolean = false) {
        ultimoResultado = resultado
        ultimoFuePractica = practica
    }
}
