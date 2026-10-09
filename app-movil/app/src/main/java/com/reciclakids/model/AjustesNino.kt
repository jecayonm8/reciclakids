package com.reciclakids.model

/** Niveles de volumen: se muestran como bloques que se llenan, sin números. */
const val NivelesVolumen = 5

/** Ajustes del Modo Niño (UI-18). Los valores iniciales son los del prototipo. */
data class AjustesNino(val volumenSonidos: Int = 4, val volumenMusica: Int = 3) {
    fun conSonidos(nivel: Int) = copy(volumenSonidos = nivel.coerceIn(0, NivelesVolumen))
    fun conMusica(nivel: Int) = copy(volumenMusica = nivel.coerceIn(0, NivelesVolumen))
}
