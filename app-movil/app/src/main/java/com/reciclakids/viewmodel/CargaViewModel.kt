package com.reciclakids.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reciclakids.network.Respuesta
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Los cuatro estados obligatorios de una pantalla adulta. [Vacio] también trae los datos: el
 * encabezado los necesita aunque todavía no haya nada que listar.
 */
sealed interface EstadoUi<out T> {
    data object Cargando : EstadoUi<Nothing>
    data class Vacio<out T>(val datos: T) : EstadoUi<T>
    data class Contenido<out T>(val datos: T) : EstadoUi<T>
    data object Error : EstadoUi<Nothing>
}

/** Datos ya cargados, estén vacíos o no. */
val <T> EstadoUi<T>.datosCargados: T?
    get() = when (val estado = this) {
        is EstadoUi.Contenido -> estado.datos
        is EstadoUi.Vacio -> estado.datos
        EstadoUi.Cargando, EstadoUi.Error -> null
    }

/**
 * Carga los datos de una pantalla adulta desde [fuente]. La pantalla llama a [cargar] cada vez
 * que entra en composición: la primera vez muestra el esqueleto; después refresca sin
 * parpadear y, si falla la red, conserva lo que ya mostraba.
 */
class CargaViewModel<T>(
    private val fuente: suspend () -> Respuesta<T>,
    private val esVacio: (T) -> Boolean = { false },
) : ViewModel() {

    var estado: EstadoUi<T> by mutableStateOf(EstadoUi.Cargando)
        private set

    private var trabajo: Job? = null

    fun cargar() {
        val silenciosa = estado.datosCargados != null
        trabajo?.cancel()
        trabajo = viewModelScope.launch {
            if (!silenciosa) estado = EstadoUi.Cargando
            estado = when (val respuesta = fuente()) {
                is Respuesta.Ok -> clasificar(respuesta.dato)
                Respuesta.SinConexion -> if (silenciosa) estado else EstadoUi.Error
            }
        }
    }

    /** Pone [datos] en pantalla de una vez, por ejemplo tras un cambio optimista. */
    fun mostrar(datos: T) {
        trabajo?.cancel()
        estado = clasificar(datos)
    }

    private fun clasificar(datos: T): EstadoUi<T> = if (esVacio(datos)) EstadoUi.Vacio(datos) else EstadoUi.Contenido(datos)
}
