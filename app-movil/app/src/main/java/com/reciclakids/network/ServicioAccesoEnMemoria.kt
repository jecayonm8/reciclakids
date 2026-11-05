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

    /** Correo de la cuenta con la sesión abierta, para cambiar su contraseña. */
    private var correoSesion: String? = null

    override suspend fun iniciarSesion(correo: String, contrasena: String): ResultadoSesion {
        delay(latenciaMs)
        val registro = mutex.withLock { cuentas[correo.normalizado()] }
        return if (registro != null && registro.contrasena == contrasena) {
            correoSesion = registro.cuenta.correo
            ResultadoSesion.Iniciada(registro.cuenta)
        } else {
            ResultadoSesion.CredencialesInvalidas
        }
    }

    override suspend fun registrarDocente(datos: RegistroDocente): ResultadoRegistro {
        val cuenta = CuentaAdulto(
            nombre = datos.nombre.trim(),
            correo = datos.correo.normalizado(),
            rol = RolAdulto.Docente,
            jardin = datos.jardin.trim(),
            grupo = datos.grupo.trim(),
        )
        return registrar(cuenta, datos.contrasena)
    }

    override suspend fun registrarAcudiente(datos: RegistroAcudiente): ResultadoRegistro =
        registrar(CuentaAdulto(datos.nombre.trim(), datos.correo.normalizado(), RolAdulto.Acudiente), datos.contrasena)

    override suspend fun enviarEnlaceRecuperacion(correo: String): Boolean {
        delay(latenciaMs)
        return true
    }

    override suspend fun cambiarContrasena(actual: String, nueva: String): ResultadoCambioContrasena {
        delay(latenciaMs)
        return mutex.withLock {
            val registro = correoSesion?.let(cuentas::get)
            if (registro == null || registro.contrasena != actual) {
                ResultadoCambioContrasena.ActualIncorrecta
            } else {
                cuentas[registro.cuenta.correo] = Registro(registro.cuenta, nueva)
                ResultadoCambioContrasena.Cambiada
            }
        }
    }

    override fun cerrarSesion() {
        correoSesion = null
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
