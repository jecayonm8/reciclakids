package com.reciclakids.model

/**
 * Límites que pone el acudiente desde el Modo Padres (UI-33). Mientras ese modo no exista, la
 * app usa estos valores por defecto: los del prototipo.
 */
data class ControlParental(val limiteMinutosDiarios: Int = 20) {
    val limiteMs: Long get() = limiteMinutosDiarios * 60_000L
}
