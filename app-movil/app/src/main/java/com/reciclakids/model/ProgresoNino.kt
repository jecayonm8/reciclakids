package com.reciclakids.model

import java.time.LocalDate

/**
 * Retos completados que llevan el acuario al 100 % (insignia «Agua cristalina»). Queda antes de
 * los 20 retos de «Guardián del mar» para que las dos insignias no se ganen a la vez.
 */
const val RetosParaAcuarioLimpio = 15

/**
 * Lo que el niño lleva acumulado en este teléfono. El acuario solo crece con los retos
 * completados, así que nunca retrocede.
 */
data class ProgresoNino(
    val ninoId: String,
    val tutorialVisto: Boolean = false,
    val retosCompletados: Int = 0,
    /** Código y fecha del último reto terminado, para saber si el del día sigue pendiente. */
    val ultimoReto: RetoCompletado? = null,
    /**
     * Residuos seguidos sin error, contados de un reto a otro. A diferencia de la racha que se ve
     * en el juego, no vuelve a 0 al empezar otra partida: solo un error la corta.
     */
    val rachaEntreRetos: Int = 0,
    /** La [rachaEntreRetos] más larga que ha tenido; da «Racha de 5» y «Coral feliz». */
    val mejorRachaEntreRetos: Int = 0,
) {
    /** El progreso tras un intento: un acierto alarga la racha entre retos y un error la corta. */
    fun conIntento(correcto: Boolean): ProgresoNino {
        val racha = if (correcto) rachaEntreRetos + 1 else 0
        return copy(rachaEntreRetos = racha, mejorRachaEntreRetos = maxOf(mejorRachaEntreRetos, racha))
    }

    /** 0–100 %, proporcional a los retos completados. */
    val porcentajeAcuario: Int get() = (retosCompletados * 100 / RetosParaAcuarioLimpio).coerceAtMost(100)

    /** Nivel 1 turbio (0–25 %), 2 opaco (26–50 %), 3 claro (51–80 %), 4 limpio (81–100 %). */
    val nivelAcuario: Int
        get() = when (porcentajeAcuario) {
            in 0..25 -> 1
            in 26..50 -> 2
            in 51..80 -> 3
            else -> 4
        }

    fun completo(reto: Reto): Boolean = ultimoReto == RetoCompletado(reto.codigo, reto.fecha)
}

data class RetoCompletado(val codigo: String, val fecha: LocalDate)
