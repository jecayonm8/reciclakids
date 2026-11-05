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
import com.reciclakids.network.ResultadoAgregarNino
import com.reciclakids.util.nombreValido
import kotlinx.coroutines.launch

/** Por qué un campo del niño nuevo no sirve todavía. */
enum class ErrorCampoNino { Vacio, Simbolos }

/** Nombre e inicial del apellido de un niño nuevo. La inicial nunca pasa de una letra. */
@Stable
class FormularioNino {
    var nombre by mutableStateOf("")

    var inicial by mutableStateOf("")
        private set

    /** Se vuelve `true` al tocar «Agregar al grupo»: desde ahí también se marcan los campos vacíos. */
    var intentoGuardar by mutableStateOf(false)
        private set

    fun escribirInicial(texto: String) {
        inicial = inicialDe(texto)
    }

    fun marcarIntento() {
        intentoGuardar = true
    }

    val errorNombre: ErrorCampoNino?
        get() = when {
            nombre.isBlank() -> ErrorCampoNino.Vacio
            !nombreValido(nombre) -> ErrorCampoNino.Simbolos
            else -> null
        }

    val errorInicial: ErrorCampoNino?
        get() = if (inicial.length == 1) null else ErrorCampoNino.Vacio

    val valido: Boolean
        get() = errorNombre == null && errorInicial == null

    /** «Salomé M.»: lo único que se guarda de su nombre. */
    val nombreGuardado: String
        get() = nombreConInicial(nombre, inicial)
}

sealed interface EstadoAgregarNino {
    data object Editando : EstadoAgregarNino
    data object Guardando : EstadoAgregarNino
    data object SinConexion : EstadoAgregarNino

    /** [nombre] ya está en el grupo. El aviso se va en cuanto se escribe otro nombre. */
    data class Repetido(val nombre: String) : EstadoAgregarNino

    /** El backend no aceptó [nombre] aunque en el teléfono pareciera bien escrito. */
    data class NombreInvalido(val nombre: String) : EstadoAgregarNino

    /** Ya está en el grupo: la pantalla muestra su código para compartirlo con su acudiente. */
    data class Agregado(val nino: NinoAgregado) : EstadoAgregarNino
}

/** Agregar un niño desde UI-25. Después de agregarlo se puede seguir con otro sin salir. */
class AgregarNinoViewModel(
    private val agregar: suspend (nombre: String, inicialApellido: String) -> ResultadoAgregarNino,
) : ViewModel() {

    var formulario by mutableStateOf(FormularioNino())
        private set

    var estado: EstadoAgregarNino by mutableStateOf(EstadoAgregarNino.Editando)
        private set

    /** Con algún campo mal, «Agregar al grupo» marca qué falta en vez de quedarse quieto. */
    fun guardar() {
        val actual = formulario
        if (estado == EstadoAgregarNino.Guardando) return
        actual.marcarIntento()
        if (!actual.valido) return
        val nombre = actual.nombreGuardado
        estado = EstadoAgregarNino.Guardando
        viewModelScope.launch {
            estado = when (val resultado = agregar(actual.nombre.trim(), actual.inicial)) {
                is ResultadoAgregarNino.Agregado -> EstadoAgregarNino.Agregado(resultado.nino)
                ResultadoAgregarNino.Repetido -> EstadoAgregarNino.Repetido(nombre)
                ResultadoAgregarNino.NombreInvalido -> EstadoAgregarNino.NombreInvalido(nombre)
                ResultadoAgregarNino.SinConexion -> EstadoAgregarNino.SinConexion
            }
        }
    }

    /** Formulario en blanco para agregar al siguiente niño. */
    fun otro() {
        formulario = FormularioNino()
        estado = EstadoAgregarNino.Editando
    }
}
