package com.reciclakids.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

// Nombres fijos en español de Colombia: no dependen de los datos de idioma del teléfono,
// que en algunos equipos abrevian «mié.» con punto o en otro idioma.
private val DiasCortos = listOf("lun", "mar", "mié", "jue", "vie", "sáb", "dom")
private val DiasLargos = listOf("lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo")
private val Meses = listOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
)

private val MesesCortos = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

/** «mié 23» */
fun diaCorto(fecha: LocalDate): String = "${DiasCortos[fecha.dayOfWeek.value - 1]} ${fecha.dayOfMonth}"

/** «martes 22» */
fun diaLargo(fecha: LocalDate): String = "${DiasLargos[fecha.dayOfWeek.value - 1]} ${fecha.dayOfMonth}"

/** «lun» */
fun nombreDiaCorto(fecha: LocalDate): String = DiasCortos[fecha.dayOfWeek.value - 1]

/** «15 – 19 de septiembre», o «29 de septiembre – 3 de octubre» si cambia el mes. */
fun rangoFechas(inicio: LocalDate, fin: LocalDate): String {
    val mesFin = Meses[fin.monthValue - 1]
    return if (inicio.month == fin.month && inicio.year == fin.year) {
        "${inicio.dayOfMonth} – ${fin.dayOfMonth} de $mesFin"
    } else {
        "${inicio.dayOfMonth} de ${Meses[inicio.monthValue - 1]} – ${fin.dayOfMonth} de $mesFin"
    }
}

/** «8–12 sep», o «29 sep–3 oct» si cambia el mes: cabe en un chip. */
fun rangoCorto(inicio: LocalDate, fin: LocalDate): String =
    if (inicio.month == fin.month && inicio.year == fin.year) {
        "${inicio.dayOfMonth}–${fin.dayOfMonth} ${MesesCortos[fin.monthValue - 1]}"
    } else {
        "${fechaCorta(inicio)}–${fechaCorta(fin)}"
    }

/** «12 sep» */
fun fechaCorta(fecha: LocalDate): String = "${fecha.dayOfMonth} ${MesesCortos[fecha.monthValue - 1]}"

/** «12 de septiembre de 2026» */
fun fechaLarga(fecha: LocalDate): String = "${fecha.dayOfMonth} de ${Meses[fecha.monthValue - 1]} de ${fecha.year}"

/** «12 de septiembre» */
fun diaYMes(fecha: LocalDate): String = "${fecha.dayOfMonth} de ${Meses[fecha.monthValue - 1]}"

/** «jueves» */
fun nombreDia(fecha: LocalDate): String = DiasLargos[fecha.dayOfWeek.value - 1]

/** «6:10 p. m.», como se escribe la hora en Colombia. */
fun horaCorta(hora: LocalTime): String {
    val h = hora.hour % 12
    val sufijo = if (hora.hour < 12) "a. m." else "p. m."
    return "${if (h == 0) 12 else h}:${hora.minute.toString().padStart(2, '0')}\u00A0$sufijo"
}

/** «1 h 12 min», «45 min» o «2 h». */
fun duracionMinutos(minutos: Int): String {
    val horas = minutos / 60
    val resto = minutos % 60
    return when {
        horas == 0 -> "$resto min"
        resto == 0 -> "$horas h"
        else -> "$horas h $resto min"
    }
}

fun esDiaHabil(fecha: LocalDate): Boolean = fecha.dayOfWeek != DayOfWeek.SATURDAY && fecha.dayOfWeek != DayOfWeek.SUNDAY

/** Los [cantidad] días hábiles que siguen a [desde], sin contarlo. Los festivos no se descuentan. */
fun siguientesDiasHabiles(desde: LocalDate, cantidad: Int): List<LocalDate> =
    generateSequence(desde.plusDays(1)) { it.plusDays(1) }.filter(::esDiaHabil).take(cantidad).toList()

/** El día hábil que queda [dias] días hábiles antes de [desde]. */
fun diaHabilAnterior(desde: LocalDate, dias: Int): LocalDate =
    generateSequence(desde.minusDays(1)) { it.minusDays(1) }.filter(::esDiaHabil).drop(dias - 1).first()

/** Semana de lunes a domingo, como en el calendario escolar. */
fun mismaSemana(a: LocalDate, b: LocalDate): Boolean = a.with(DayOfWeek.MONDAY) == b.with(DayOfWeek.MONDAY)
