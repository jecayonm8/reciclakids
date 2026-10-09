package com.reciclakids.model

/** Minutos diarios que se pueden elegir en UI-33, de 5 en 5. */
val OpcionesMinutosDiarios: IntRange = 5..45
const val PasoMinutosDiarios = 5

/**
 * Lo que decide el acudiente desde el Modo Padres (UI-33) para su hijo o hija. El Modo Niño usa
 * [limiteMinutosDiarios] para cortar el juego con UI-19. Los valores por defecto son los del
 * prototipo.
 */
data class ControlParental(
    val limiteMinutosDiarios: Int = 20,
    /** Sugerencia para la docente: ella puede subirla en un reto puntual. */
    val dificultadSugerida: Dificultad = Dificultad.Medio,
    /** Un correo por logro, máximo uno al día (UI-36). */
    val correosLogro: Boolean = true,
    /** Los viernes a las 5:00 p. m. (UI-37). */
    val reporteSemanal: Boolean = true,
    /** Aviso si el niño lleva tres días seguidos sin jugar. */
    val recordatorioSinJugar: Boolean = false,
) {
    val limiteMs: Long get() = limiteMinutosDiarios * 60_000L

    /** Suma o resta [PasoMinutosDiarios] sin salirse de [OpcionesMinutosDiarios]. */
    fun conMinutos(minutos: Int): ControlParental =
        copy(limiteMinutosDiarios = minutos.coerceIn(OpcionesMinutosDiarios.first, OpcionesMinutosDiarios.last))
}
