package com.reciclakids.network

import com.reciclakids.model.ControlParental
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * El control parental de cada niño, compartido entre el Modo Padres, que lo cambia, y el Modo
 * Niño, que corta el juego al llegar al límite.
 *
 * PROVISIONAL: vive en memoria mientras el backend no lo entregue junto con el reto del día; al
 * cerrar la app vuelve a [porDefecto].
 */
class ControlesParentales(private val porDefecto: ControlParental = ControlParental()) {

    private val controles = MutableStateFlow<Map<String, ControlParental>>(emptyMap())

    fun de(ninoId: String): ControlParental = controles.value[ninoId] ?: porDefecto

    fun observar(ninoId: String): Flow<ControlParental> =
        controles.map { it[ninoId] ?: porDefecto }.distinctUntilChanged()

    fun guardar(ninoId: String, control: ControlParental) {
        controles.update { it + (ninoId to control) }
    }
}
