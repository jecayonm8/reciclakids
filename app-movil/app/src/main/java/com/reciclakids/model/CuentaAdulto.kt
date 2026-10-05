package com.reciclakids.model

/** Los dos roles con cuenta. El niño no tiene usuario: entra con el código del reto. */
enum class RolAdulto { Docente, Acudiente }

data class CuentaAdulto(val nombre: String, val correo: String, val rol: RolAdulto)
