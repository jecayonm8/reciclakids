package com.reciclakids.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.reciclakids.model.CuentaAdulto
import com.reciclakids.model.RolAdulto
import com.reciclakids.network.ServicioDocente

/**
 * Sesión adulta abierta, a nivel de la actividad, para que sobreviva a los cambios de
 * configuración. No se guarda en disco: al cerrar la app hay que volver a iniciar sesión.
 */
class SesionViewModel : ViewModel() {
    var cuenta: CuentaAdulto? by mutableStateOf(null)
        private set

    /** Solo existe mientras la sesión es de una docente. */
    var servicioDocente: ServicioDocente? by mutableStateOf(null)
        private set

    // Un servicio por docente mientras viva la app: al volver a entrar encuentra lo que publicó.
    private val serviciosDocente = mutableMapOf<String, ServicioDocente>()

    fun iniciar(cuenta: CuentaAdulto, crearServicioDocente: (CuentaAdulto) -> ServicioDocente) {
        this.cuenta = cuenta
        servicioDocente = if (cuenta.rol == RolAdulto.Docente) {
            serviciosDocente.getOrPut(cuenta.correo) { crearServicioDocente(cuenta) }
        } else {
            null
        }
    }

    fun cerrar() {
        cuenta = null
        servicioDocente = null
    }
}
