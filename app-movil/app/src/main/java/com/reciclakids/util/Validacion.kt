package com.reciclakids.util

const val LongitudMinimaContrasena = 8

private val PatronCorreo = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)+$")

// Lo mismo que acepta el backend: letras (con tildes y ñ), espacios, apóstrofo, guion y punto.
private val PatronNombre = Regex("^[\\p{L}\\p{M}][\\p{L}\\p{M} '’.-]*$")

// Ni etiquetas ni llaves en los textos libres: se muestran en la app y en los correos.
private val CaracteresProhibidos = Regex("[<>{}]")

private val Espacios = Regex("\\s+")

fun correoValido(correo: String): Boolean = PatronCorreo.matches(correo.trim())

fun contrasenaValida(contrasena: String): Boolean = contrasena.length >= LongitudMinimaContrasena

/** Nombre de una persona: «María José», «D'Angelo». Vacío no es válido. */
fun nombreValido(nombre: String): Boolean = PatronNombre.matches(nombre.trim().replace(Espacios, " "))

/** Jardín, grupo o nombre del reto: cualquier texto sin etiquetas ni llaves. Vacío no es válido. */
fun textoLibreValido(texto: String): Boolean = texto.isNotBlank() && !CaracteresProhibidos.containsMatchIn(texto)
