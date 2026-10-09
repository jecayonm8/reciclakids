package com.reciclakids.model

/**
 * Las 8 insignias del diseño. Los criterios exactos no están definidos en el handoff; estos salen
 * de las notas del prototipo («Sin errores», «A tiempo», «5 seguidos», «5 días», «20 retos»…) y
 * se pueden ajustar sin tocar las pantallas.
 *
 * [requisito] es la nota de la insignia pendiente; [logro] es la frase al ganarla.
 */
enum class Insignia(val nombre: String, val requisito: String, val logro: String) {
    AmigaTortuga("Amiga tortuga", "Sin errores", "Terminaste el reto sin ninguna ayuda"),
    RapidoComoPez("Rápido como pez", "A tiempo", "Terminaste el reto difícil a tiempo"),
    RachaDeCinco("Racha de 5", "5 seguidos", "Separaste 5 residuos seguidos"),
    CincoRetosDiarios("5 retos diarios", "5 días", "Jugaste el reto en 5 días distintos"),
    PulpoOrdenado("Pulpo ordenado", "10 retos", "Completaste 10 retos"),
    AguaCristalina("Agua cristalina", "Acuario al 100 %", "Dejaste tu acuario limpio"),
    CoralFeliz("Coral feliz", "Racha de 10", "Separaste 10 residuos seguidos"),
    GuardianDelMar("Guardián del mar", "20 retos", "Completaste 20 retos"),
}

const val DiasParaCincoRetos = 5
const val RetosParaPulpoOrdenado = 10
const val RetosParaGuardian = 20

/**
 * Insignias que el niño acaba de ganar al terminar una partida (RNF-04: se otorgan de forma
 * automática e inmediata). [progreso] ya incluye el reto recién terminado.
 */
fun insigniasNuevas(
    estado: EstadoPartida,
    dificultad: Dificultad,
    aTiempo: Boolean,
    progreso: ProgresoNino,
    diasConReto: Int,
    yaGanadas: Set<Insignia>,
): List<Insignia> {
    val cumplidas = buildList {
        if (estado.errores == 0) add(Insignia.AmigaTortuga)
        if (dificultad == Dificultad.Dificil && aTiempo) add(Insignia.RapidoComoPez)
        if (estado.rachaMaxima >= 5) add(Insignia.RachaDeCinco)
        if (diasConReto >= DiasParaCincoRetos) add(Insignia.CincoRetosDiarios)
        if (progreso.retosCompletados >= RetosParaPulpoOrdenado) add(Insignia.PulpoOrdenado)
        if (progreso.porcentajeAcuario >= 100) add(Insignia.AguaCristalina)
        if (estado.rachaMaxima >= 10) add(Insignia.CoralFeliz)
        if (progreso.retosCompletados >= RetosParaGuardian) add(Insignia.GuardianDelMar)
    }
    return cumplidas.filterNot { it in yaGanadas }
}
