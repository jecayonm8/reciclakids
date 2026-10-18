package com.reciclakids.models.dto

import kotlinx.serialization.Serializable

// Cuerpos de la API de Autenticación (/api/v1/auth). Los nombres de los campos siguen los
// modelos de la app para que el cliente los use sin traducir.

@Serializable
enum class RolAdulto { Docente, Acudiente }

@Serializable
data class RegistroDocenteSolicitud(
    val nombre: String,
    val correo: String,
    val jardin: String,
    val grupo: String,
    val contrasena: String,
)

/**
 * [autorizaDatosMenor] es la casilla explícita de UI-07 (Ley 1581 de 2012): sin ella no se crea
 * la cuenta. [codigoVinculacion] es el del niño, que entrega la docente.
 */
@Serializable
data class RegistroAcudienteSolicitud(
    val nombre: String,
    val correo: String,
    val contrasena: String,
    val codigoVinculacion: String,
    val autorizaDatosMenor: Boolean,
)

@Serializable
data class InicioSesionSolicitud(val correo: String, val contrasena: String)

@Serializable
data class RefrescoSolicitud(val tokenRefresco: String)

@Serializable
data class RecuperacionSolicitud(val correo: String)

@Serializable
data class RestablecerSolicitud(val token: String, val contrasena: String)

/** [jardin] y [grupo] solo vienen en la cuenta de una docente. */
@Serializable
data class CuentaDto(
    val nombre: String,
    val correo: String,
    val rol: RolAdulto,
    val jardin: String? = null,
    val grupo: String? = null,
)

/** Respuesta de iniciar sesión y de refrescar. El token de refresco rota en cada uso. */
@Serializable
data class SesionDto(
    val tokenAcceso: String,
    val venceEnSegundos: Long,
    val tokenRefresco: String,
    val cuenta: CuentaDto,
)
