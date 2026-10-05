package com.reciclakids.viewmodel

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.reciclakids.model.RolAdulto
import com.reciclakids.network.RegistroAcudiente
import com.reciclakids.network.RegistroDocente
import com.reciclakids.util.contrasenaValida
import com.reciclakids.util.correoValido

/** Campos del registro de docente (UI-04). La aceptación de términos nunca arranca marcada. */
@Stable
class FormularioDocente {
    var nombre by mutableStateOf("")
    var correo by mutableStateOf("")
    var jardin by mutableStateOf("")
    var grupo by mutableStateOf("")
    var contrasena by mutableStateOf("")
    var aceptaTerminos by mutableStateOf(false)

    val completo: Boolean
        get() = nombre.isNotBlank() && correoValido(correo) && jardin.isNotBlank() && grupo.isNotBlank() &&
            contrasenaValida(contrasena)

    fun aRegistro() = RegistroDocente(nombre.trim(), correo.trim(), jardin.trim(), grupo.trim(), contrasena)
}

/** Campos del registro de acudiente, paso 1 de 2 (UI-05). */
@Stable
class FormularioAcudiente {
    var nombre by mutableStateOf("")
    var correo by mutableStateOf("")
    var contrasena by mutableStateOf("")
    var codigoVinculacion by mutableStateOf("")

    val completo: Boolean
        get() = nombre.isNotBlank() && correoValido(correo) && contrasenaValida(contrasena) && codigoVinculacion.isNotBlank()

    fun aRegistro() = RegistroAcudiente(nombre.trim(), correo.trim(), contrasena, codigoVinculacion.trim())
}

/**
 * Formularios del registro. Vive mientras dure el grafo de registro para que los datos del
 * paso 1 lleguen al paso 2 sin viajar como argumentos de navegación ni guardarse en disco.
 */
class RegistroViewModel : ViewModel() {
    var rol by mutableStateOf(RolAdulto.Docente)
    val docente = FormularioDocente()
    val acudiente = FormularioAcudiente()
}
