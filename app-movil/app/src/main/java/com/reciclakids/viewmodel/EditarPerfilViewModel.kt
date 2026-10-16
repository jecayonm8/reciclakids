package com.reciclakids.viewmodel

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reciclakids.network.DatosPerfilAcudiente
import com.reciclakids.network.DatosPerfilDocente
import com.reciclakids.network.Respuesta
import com.reciclakids.network.ResultadoEdicionPerfil
import com.reciclakids.util.correoValido
import kotlinx.coroutines.launch

/**
 * Campos de un perfil en edición: arrancan con lo guardado y saben si algo cambió. Validan
 * igual que el registro (UI-04/05): nombre obligatorio y correo con formato válido.
 */
@Stable
abstract class FormularioPerfil<D>(private val original: D, nombre: String, correo: String) {
    var nombre by mutableStateOf(nombre)
    var correo by mutableStateOf(correo)

    /** Lo escrito, sin espacios en los extremos: es lo que se compara y lo que se envía. */
    abstract val datos: D

    open val valido: Boolean
        get() = nombre.isNotBlank() && correoValido(correo)

    val hayCambios: Boolean
        get() = datos != original
}

@Stable
class FormularioPerfilDocente(original: DatosPerfilDocente) :
    FormularioPerfil<DatosPerfilDocente>(original, original.nombre, original.correo) {
    var jardin by mutableStateOf(original.jardin)
    var grupo by mutableStateOf(original.grupo)

    override val datos: DatosPerfilDocente
        get() = DatosPerfilDocente(nombre.trim(), correo.trim(), jardin.trim(), grupo.trim())

    override val valido: Boolean
        get() = super.valido && jardin.isNotBlank() && grupo.isNotBlank()
}

@Stable
class FormularioPerfilAcudiente(original: DatosPerfilAcudiente) :
    FormularioPerfil<DatosPerfilAcudiente>(original, original.nombre, original.correo) {
    override val datos: DatosPerfilAcudiente
        get() = DatosPerfilAcudiente(nombre.trim(), correo.trim())
}

/** Lo que pasa con «Guardar cambios». */
sealed interface EstadoGuardado {
    data object Editando : EstadoGuardado
    data object Guardando : EstadoGuardado
    data object SinConexion : EstadoGuardado

    /** [correo] ya es de otra cuenta. El error se va en cuanto se escribe otro. */
    data class CorreoEnUso(val correo: String) : EstadoGuardado

    /** Quedó guardado: la pantalla vuelve a la cuenta con los datos nuevos. */
    data object Guardado : EstadoGuardado
}

/** Mientras se guarda, o ya se guardó y la pantalla va de salida, el formulario no se toca. */
val EstadoGuardado.ocupado: Boolean
    get() = this == EstadoGuardado.Guardando || this == EstadoGuardado.Guardado

/** El correo escrito es el mismo que el servicio dijo que ya tiene otra cuenta. */
fun FormularioPerfil<*>.correoEnUso(guardado: EstadoGuardado): Boolean =
    guardado is EstadoGuardado.CorreoEnUso && guardado.correo == correo.trim()

/** Hay algo nuevo que guardar y todo está bien escrito. Si no, «Guardar cambios» queda deshabilitado. */
fun FormularioPerfil<*>.listoParaGuardar(guardado: EstadoGuardado): Boolean =
    hayCambios && valido && !correoEnUso(guardado)

/**
 * Editar el perfil de la docente (desde UI-28) o del acudiente (desde UI-35). Carga lo guardado
 * una sola vez: volver a componer la pantalla no borra lo que se está escribiendo.
 */
class EditarPerfilViewModel<F : FormularioPerfil<*>>(
    private val cargarFormulario: suspend () -> Respuesta<F>,
    private val guardarFormulario: suspend (F) -> ResultadoEdicionPerfil,
) : ViewModel() {

    var estado: EstadoUi<F> by mutableStateOf(EstadoUi.Cargando)
        private set

    var guardado: EstadoGuardado by mutableStateOf(EstadoGuardado.Editando)
        private set

    fun cargar() {
        if (estado.datosCargados != null) return
        viewModelScope.launch {
            estado = EstadoUi.Cargando
            estado = when (val respuesta = cargarFormulario()) {
                is Respuesta.Ok -> EstadoUi.Contenido(respuesta.dato)
                Respuesta.SinConexion -> EstadoUi.Error
            }
        }
    }

    fun guardar() {
        val formulario = estado.datosCargados ?: return
        if (guardado.ocupado || !formulario.listoParaGuardar(guardado)) return
        val correo = formulario.correo.trim()
        guardado = EstadoGuardado.Guardando
        viewModelScope.launch {
            guardado = when (guardarFormulario(formulario)) {
                ResultadoEdicionPerfil.Guardado -> EstadoGuardado.Guardado
                ResultadoEdicionPerfil.CorreoEnUso -> EstadoGuardado.CorreoEnUso(correo)
                ResultadoEdicionPerfil.SinConexion -> EstadoGuardado.SinConexion
            }
        }
    }
}
