package com.reciclakids.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.reciclakids.model.Nino
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

    fun abrirReto(reto: Reto) {
        this.reto = reto
        nino = null
    }

    fun elegirNino(nino: Nino) {
        this.nino = nino
    }
}
