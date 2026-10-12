package com.reciclakids.services.notificaciones.correos

/**
 * Datos del correo de logro (UI-36). Lleva solo datos del propio hijo o hija del acudiente:
 * nunca otros niños ni comparaciones con el grupo (Ley 1581 de 2012).
 */
data class DatosCorreoLogro(
    /** Solo el primer nombre, como lo muestra el correo: «Salomé». */
    val nombreNino: String,
    /** «Jardín Gotitas». */
    val jardin: String,
    /** «Jardín B». */
    val grupo: String,
    /** «Amiga tortuga». */
    val nombreInsignia: String,
    /** Qué hizo para ganarla, en tercera persona: «Terminó el reto de hoy sin ninguna ayuda…». */
    val descripcionLogro: String,
    /** Contando la que acaba de ganar. */
    val insigniasGanadas: Int,
    val insigniasTotales: Int,
    /** Abre el detalle de ese hijo en la app: el botón «Ver su progreso». */
    val enlaceProgreso: String,
    val enlacesPie: EnlacesPieCorreo,
) {
    init {
        require(nombreNino.isNotBlank()) { "El correo de logro necesita el nombre del niño" }
        require(nombreInsignia.isNotBlank()) { "El correo de logro necesita el nombre de la insignia" }
        require(descripcionLogro.isNotBlank()) { "El correo de logro necesita la descripción del logro" }
        require(insigniasTotales > 0) { "El total de insignias debe ser mayor que cero" }
        require(insigniasGanadas in 1..insigniasTotales) { "Las insignias ganadas deben estar entre 1 y el total" }
    }
}
