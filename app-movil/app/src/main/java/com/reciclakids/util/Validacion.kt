package com.reciclakids.util

const val LongitudMinimaContrasena = 8

private val PatronCorreo = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)+$")

fun correoValido(correo: String): Boolean = PatronCorreo.matches(correo.trim())

fun contrasenaValida(contrasena: String): Boolean = contrasena.length >= LongitudMinimaContrasena
