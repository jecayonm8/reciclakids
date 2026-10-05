package com.reciclakids.network

import com.reciclakids.model.CuentaAdulto
import com.reciclakids.model.RolAdulto
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Implementación PROVISIONAL mientras el backend no expone autenticación: las cuentas viven
 * solo en memoria y se pierden al cerrar la app. Sirve para recorrer el flujo de acceso
 * (registro → inicio de sesión → modo según el rol); se reemplaza por el cliente del backend.
 */
class ServicioAccesoEnMemoria(private val latenciaMs: Long = 600) : ServicioAcceso {

    private class Registro(val cuenta: CuentaAdulto, val contrasena: String)

    private val mutex = Mutex()
    private val cuentas = mutableMapOf<String, Registro>()

    override suspend fun iniciarSesion(correo: String, contrasena: String): ResultadoSesion {
        delay(latenciaMs)
        val registro = mutex.withLock { cuentas[correo.normalizado()] }
        return if (registro != null && registro.contrasena == contrasena) {
            ResultadoSesion.Iniciada(registro.cuenta)
        } else {
            ResultadoSesion.CredencialesInvalidas
        }
    }

    override suspend fun registrarDocente(datos: RegistroDocente): ResultadoRegistro =
        registrar(CuentaAdulto(datos.nombre.trim(), datos.correo.normalizado(), RolAdulto.Docente), datos.contrasena)

    override suspend fun registrarAcudiente(datos: RegistroAcudiente): ResultadoRegistro =
        registrar(CuentaAdulto(datos.nombre.trim(), datos.correo.normalizado(), RolAdulto.Acudiente), datos.contrasena)

    override suspend fun enviarEnlaceRecuperacion(correo: String): Boolean {
        delay(latenciaMs)
        return true
    }

    private suspend fun registrar(cuenta: CuentaAdulto, contrasena: String): ResultadoRegistro {
        delay(latenciaMs)
        return mutex.withLock {
            if (cuenta.correo in cuentas) {
                ResultadoRegistro.CorreoEnUso
            } else {
                cuentas[cuenta.correo] = Registro(cuenta, contrasena)
                ResultadoRegistro.Creada
            }
        }
    }

    private fun String.normalizado() = trim().lowercase()
}
