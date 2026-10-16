package com.reciclakids.viewmodel

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reciclakids.model.NinoAgregado
import com.reciclakids.model.inicialDe
import com.reciclakids.model.nombreConInicial
import com.reciclakids.network.Respuesta
import kotlinx.coroutines.launch

/** Nombre e inicial del apellido de un niño nuevo. La inicial nunca pasa de una letra. */
@Stable
class FormularioNino {
    var nombre by mutableStateOf("")

    var inicial by mutableStateOf("")
        private set

    fun escribirInicial(texto: String) {
        inicial = inicialDe(texto)
    }

    val valido: Boolean
        get() = nombre.isNotBlank() && inicial.length == 1

    /** «Salomé M.»: lo único que se guarda de su nombre. */
    val nombreGuardado: String
        get() = nombreConInicial(nombre, inicial)
}

sealed interface EstadoAgregarNino {
    data object Editando : EstadoAgregarNino
    data object Guardando : EstadoAgregarNino
    data object SinConexion : EstadoAgregarNino

    /** Ya está en el grupo: la pantalla muestra su código para compartirlo con su acudiente. */
    data class Agregado(val nino: NinoAgregado) : EstadoAgregarNino
}

/** Agregar un niño desde UI-25. Después de agregarlo se puede seguir con otro sin salir. */
class AgregarNinoViewModel(
    private val agregar: suspend (nombre: String, inicialApellido: String) -> Respuesta<NinoAgregado>,
) : ViewModel() {

    var formulario by mutableStateOf(FormularioNino())
        private set

    var estado: EstadoAgregarNino by mutableStateOf(EstadoAgregarNino.Editando)
        private set

    fun guardar() {
        val actual = formulario
        if (!actual.valido || estado == EstadoAgregarNino.Guardando) return
        estado = EstadoAgregarNino.Guardando
        viewModelScope.launch {
            estado = when (val respuesta = agregar(actual.nombre.trim(), actual.inicial)) {
                is Respuesta.Ok -> EstadoAgregarNino.Agregado(respuesta.dato)
                Respuesta.SinConexion -> EstadoAgregarNino.SinConexion
            }
        }
    }

    /** Formulario en blanco para agregar al siguiente niño. */
    fun otro() {
        formulario = FormularioNino()
        estado = EstadoAgregarNino.Editando
    }
}
