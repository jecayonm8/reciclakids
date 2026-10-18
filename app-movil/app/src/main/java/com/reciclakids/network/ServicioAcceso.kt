package com.reciclakids.network

import com.reciclakids.model.CuentaAdulto

data class RegistroDocente(
    val nombre: String,
    val correo: String,
    val jardin: String,
    val grupo: String,
    val contrasena: String,
)

/** [codigoVinculacion] lo entrega la docente y vincula la cuenta a un solo niño o niña. */
data class RegistroAcudiente(
    val nombre: String,
    val correo: String,
    val contrasena: String,
    val codigoVinculacion: String,
)

sealed interface ResultadoSesion {
    data class Iniciada(val cuenta: CuentaAdulto) : ResultadoSesion
    data object CredencialesInvalidas : ResultadoSesion
    data object SinConexion : ResultadoSesion
}

/** [CodigoInvalido]: ningún niño tiene ese código de vinculación (solo en el registro del acudiente). */
enum class ResultadoRegistro { Creada, CorreoEnUso, CodigoInvalido, SinConexion }

/** Acceso de los modos adultos. La app abre Docente o Padres según el rol de la cuenta. */
interface ServicioAcceso {
    suspend fun iniciarSesion(correo: String, contrasena: String): ResultadoSesion

    suspend fun registrarDocente(datos: RegistroDocente): ResultadoRegistro

    /** Solo se llama después de la autorización explícita de datos del menor (Ley 1581 de 2012). */
    suspend fun registrarAcudiente(datos: RegistroAcudiente): ResultadoRegistro

    /**
     * Pide el enlace de recuperación. Devuelve `false` solo si no hubo conexión: nunca revela
     * si el correo tiene cuenta.
     */
    suspend fun enviarEnlaceRecuperacion(correo: String): Boolean

    /**
     * La sesión que quedó abierta en el teléfono, para entrar al modo sin volver a escribir la
     * contraseña (ADR-08). `null` si no hay o ya venció. Se consulta después de la puerta para
     * adultos: el niño nunca llega a un modo adulto sin pasarla.
     */
    suspend fun sesionGuardada(): CuentaAdulto? = null

    /** Cierra la sesión en el backend y la borra del teléfono. */
    fun cerrarSesion() {}
}
