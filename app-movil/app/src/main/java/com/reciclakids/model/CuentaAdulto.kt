package com.reciclakids.model

/** Los dos roles con cuenta. El niño no tiene usuario: entra con el código del reto. */
enum class RolAdulto { Docente, Acudiente }

/** [jardin] y [grupo] solo existen en la cuenta de una docente: son los datos de su registro. */
data class CuentaAdulto(
    val nombre: String,
    val correo: String,
    val rol: RolAdulto,
    val jardin: String? = null,
    val grupo: String? = null,
)
