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

/**
 * Los ajustes de UI-33 que cambiaron; los que vienen en `null` se quedan como estaban. Así un
 * cambio nunca devuelve los demás ajustes a sus valores iniciales (hallazgo 11 de la auditoría).
 */
data class CambioControlParental(
    val limiteMinutosDiarios: Int? = null,
    val dificultadSugerida: Dificultad? = null,
    val correosLogro: Boolean? = null,
    val reporteSemanal: Boolean? = null,
    val recordatorioSinJugar: Boolean? = null,
) {
    val vacio: Boolean get() = this == CambioControlParental()

    /** [control] con este cambio encima. */
    fun aplicarA(control: ControlParental): ControlParental = ControlParental(
        limiteMinutosDiarios = limiteMinutosDiarios ?: control.limiteMinutosDiarios,
        dificultadSugerida = dificultadSugerida ?: control.dificultadSugerida,
        correosLogro = correosLogro ?: control.correosLogro,
        reporteSemanal = reporteSemanal ?: control.reporteSemanal,
        recordatorioSinJugar = recordatorioSinJugar ?: control.recordatorioSinJugar,
    )

    companion object {
        /** Solo lo que es distinto en [despues], con su valor nuevo. */
        fun entre(antes: ControlParental, despues: ControlParental) = CambioControlParental(
            limiteMinutosDiarios = despues.limiteMinutosDiarios.takeIf { it != antes.limiteMinutosDiarios },
            dificultadSugerida = despues.dificultadSugerida.takeIf { it != antes.dificultadSugerida },
            correosLogro = despues.correosLogro.takeIf { it != antes.correosLogro },
            reporteSemanal = despues.reporteSemanal.takeIf { it != antes.reporteSemanal },
            recordatorioSinJugar = despues.recordatorioSinJugar.takeIf { it != antes.recordatorioSinJugar },
        )
    }
}
