package com.reciclakids.services.notificaciones.correos

import java.time.LocalDate

/** Espacio que no se parte al final de la línea: «88 %», «20 min» y «1 h» no se separan. */
internal const val EspacioFijo = ' '

private val Meses = listOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
)

/** «88 %», con el espacio antes del signo que pide la norma del español. */
fun formatearPorcentaje(valor: Int): String = "$valor$EspacioFijo%"

/** «1 h 12 min», «45 min» o «2 h». */
fun formatearDuracion(minutos: Int): String {
    require(minutos >= 0) { "Los minutos no pueden ser negativos" }
    val horas = minutos / 60
    val resto = minutos % 60
    return when {
        horas == 0 -> "$resto${EspacioFijo}min"
        resto == 0 -> "$horas${EspacioFijo}h"
        else -> "$horas${EspacioFijo}h $resto${EspacioFijo}min"
    }
}

/**
 * Rango de fechas en español, sin el año salvo que cambie: «del 15 al 19 de septiembre»,
 * «del 29 de septiembre al 3 de octubre» o «del 29 de diciembre de 2025 al 2 de enero de 2026».
 */
fun formatearRangoFechas(inicio: LocalDate, fin: LocalDate): String {
    require(!fin.isBefore(inicio)) { "El rango de fechas termina antes de empezar" }
    fun diaYMes(fecha: LocalDate) = "${fecha.dayOfMonth} de ${Meses[fecha.monthValue - 1]}"
    return when {
        inicio == fin -> "del ${diaYMes(inicio)}"
        inicio.year != fin.year -> "del ${diaYMes(inicio)} de ${inicio.year} al ${diaYMes(fin)} de ${fin.year}"
        inicio.month != fin.month -> "del ${diaYMes(inicio)} al ${diaYMes(fin)}"
        else -> "del ${inicio.dayOfMonth} al ${diaYMes(fin)}"
    }
}

/** Enumeración en español: «A», «A y B», «A, B y C». */
internal fun unirConY(elementos: List<String>): String = when (elementos.size) {
    0 -> ""
    1 -> elementos.first()
    else -> elementos.dropLast(1).joinToString(", ") + " y " + elementos.last()
}

/** Agrega el punto final si la frase no termina ya en «.», «!» o «?». */
internal fun conPuntoFinal(frase: String): String =
    if (frase.isEmpty() || frase.last() in ".!?") frase else "$frase."
