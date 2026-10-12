package com.reciclakids.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.roundToInt

// Modo Padres (UI-29 a UI-35). Un acudiente solo ve a su propio hijo o hija y, de él o ella,
// solo los datos mínimos: nombre con la inicial del apellido, avatar y resultados (Ley 1581 de 2012).

/** Hijo o hija vinculado a la cuenta. [id] es el mismo con el que juega en su grupo. */
data class HijoVinculado(
    val id: String,
    /** Ya viene como «Salomé M.»: nunca el apellido completo. */
    val nombre: String,
    val grupo: String,
    val edad: Int,
    val avatar: String,
    val vinculadoEl: LocalDate,
)

/** Residuos acertados al primer intento sobre los que traía el reto, para una caneca. */
data class Conteo(val aciertos: Int, val total: Int) {
    operator fun plus(otro: Conteo) = Conteo(aciertos + otro.aciertos, total + otro.total)

    /** `null` si no hubo residuos de esa caneca. */
    val porcentaje: Int? get() = if (total == 0) null else (aciertos * 100f / total).roundToInt()
}

/** Lo que el niño jugó un día escolar. Si ese día no jugó, no hay registro. */
data class DiaDeJuego(
    val fecha: LocalDate,
    val minutos: Int,
    val completo: Boolean,
    val porCategoria: Map<CategoriaResiduo, Conteo>,
    val rachaMaxima: Int,
    /** Residuos que llevó primero a otra caneca. */
    val confusiones: List<Residuo> = emptyList(),
) {
    val errores: Int get() = porCategoria.values.sumOf { it.total - it.aciertos }
}

/** Insignia del niño: [ganadaEl] es `null` mientras está por descubrir. */
data class LogroHijo(val insignia: Insignia, val ganadaEl: LocalDate?) {
    val ganada: Boolean get() = ganadaEl != null
}

/** Lunes de la semana escolar de [fecha]. El sábado y el domingo cierran la semana que termina. */
fun inicioSemana(fecha: LocalDate): LocalDate = fecha.with(DayOfWeek.MONDAY)

/**
 * Una semana escolar (lunes a viernes) resumida para el acudiente. [retosPublicados] cuenta los
 * días con reto hasta hoy; [dias] solo trae los días que jugó.
 */
data class SemanaHijo(
    val inicio: LocalDate,
    val retosPublicados: Int,
    val dias: List<DiaDeJuego>,
    val insignias: List<LogroHijo>,
) {
    val fin: LocalDate get() = inicio.plusDays(4)
    val retosCompletados: Int get() = dias.count { it.completo }
    val jugo: Boolean get() = dias.isNotEmpty()

    val porCategoria: Map<CategoriaResiduo, Conteo>
        get() = CategoriaResiduo.entries
            .associateWith { categoria -> dias.mapNotNull { it.porCategoria[categoria] }.fold(Conteo(0, 0), Conteo::plus) }
            .filterValues { it.total > 0 }

    /** Porcentaje de aciertos al primer intento; `null` si no jugó. */
    val aciertos: Int? get() = porCategoria.values.fold(Conteo(0, 0), Conteo::plus).porcentaje

    val rachaMaxima: Int get() = dias.maxOfOrNull { it.rachaMaxima } ?: 0
    val minutos: Int get() = dias.sumOf { it.minutos }

    /** Promedio sobre los días con reto, incluidos los que no jugó. */
    val minutosPromedio: Int get() = if (retosPublicados == 0) 0 else (minutos.toFloat() / retosPublicados).roundToInt()

    /** Minutos de cada día hábil de la semana que ya pasó, con 0 si no jugó. */
    fun minutosPorDia(hasta: LocalDate): List<Pair<LocalDate, Int>> =
        (0L..4L).map { inicio.plusDays(it) }
            .filter { !it.isAfter(hasta) }
            .map { fecha -> fecha to (dias.firstOrNull { it.fecha == fecha }?.minutos ?: 0) }

    /** El residuo que más confundió en la semana. */
    val confusionFrecuente: Residuo?
        get() = dias.flatMap { it.confusiones }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
}

/** Desde aquí una caneca se considera dominada; coincide con el verde de las barras. */
const val UmbralAciertosPadres = 80

/** Mejora mínima, en puntos, para contarla en la frase de la semana. */
const val MejoraMinimaPuntos = 5

/** La frase interpretativa de UI-29 y UI-30: el avance en lenguaje claro, no en métricas. */
sealed interface MensajeSemana {
    /** «Esta semana mejoró separando orgánicos: pasó de 74 % a 92 % de aciertos.» */
    data class Mejoro(val categoria: CategoriaResiduo, val antes: Int, val ahora: Int) : MensajeSemana

    /** Le cuesta una caneca y no mejoró: una idea para practicar en casa. */
    data class Practicar(val categoria: CategoriaResiduo, val aciertos: Int) : MensajeSemana

    data object VaMuyBien : MensajeSemana
}

/** La caneca que más mejoró frente a la semana anterior; si ninguna, la que más le cuesta. */
fun mensajeSemana(actual: SemanaHijo, anterior: SemanaHijo?): MensajeSemana? {
    val ahora = actual.porCategoria.mapValues { it.value.porcentaje ?: 0 }
    if (ahora.isEmpty()) return null
    val antes = anterior?.porCategoria.orEmpty().mapValues { it.value.porcentaje ?: 0 }
    val mejora = ahora.keys.filter { it in antes }
        .map { MensajeSemana.Mejoro(it, antes.getValue(it), ahora.getValue(it)) }
        .maxByOrNull { it.ahora - it.antes }
    if (mejora != null && mejora.ahora - mejora.antes >= MejoraMinimaPuntos) return mejora
    val dificil = ahora.minBy { it.value }
    return if (dificil.value < UmbralAciertosPadres) MensajeSemana.Practicar(dificil.key, dificil.value) else MensajeSemana.VaMuyBien
}

/** UI-29: la semana del hijo, legible en cinco segundos. */
data class InicioPadres(
    val hijo: HijoVinculado,
    val hoy: LocalDate,
    val semana: SemanaHijo,
    val anterior: SemanaHijo?,
    val ultimoLogro: LogroHijo?,
    val insigniasGanadas: Int,
    val limiteMinutos: Int,
    /** Nunca ha jugado: todavía no hay nada que reportar. */
    val sinDatos: Boolean,
) {
    val mensaje: MensajeSemana? get() = mensajeSemana(semana, anterior)
}

/** UI-30: una semana en detalle. [semanas] van de la más reciente a la más antigua. */
data class ReporteHijo(
    val hijo: HijoVinculado,
    val hoy: LocalDate,
    val semanas: List<LocalDate>,
    val semana: SemanaHijo,
    val anterior: SemanaHijo?,
    /** Aciertos de las semanas jugadas, de la más antigua a la más reciente. */
    val evolucion: List<Pair<LocalDate, Int>>,
    val limiteMinutos: Int,
) {
    val sinDatos: Boolean get() = evolucion.isEmpty()
    val mensaje: MensajeSemana? get() = mensajeSemana(semana, anterior)

    /** Puntos de aciertos frente a la semana anterior; `null` si falta alguna de las dos. */
    val cambio: Int?
        get() {
            val actual = semana.aciertos ?: return null
            val previa = anterior?.aciertos ?: return null
            return actual - previa
        }
}

/** UI-31. [retosCompletados] y [diasConReto] dicen cuánto falta para las insignias de constancia. */
data class LogrosHijo(
    val hijo: HijoVinculado,
    val hoy: LocalDate,
    val logros: List<LogroHijo>,
    val retosCompletados: Int,
    val diasConReto: Int,
) {
    val ganadas: Int get() = logros.count { it.ganada }
}

enum class TipoCorreo { Logro, ReporteSemanal }

/** UI-32: cada correo que se le envió al acudiente sobre este hijo o hija. */
data class CorreoEnviado(
    val tipo: TipoCorreo,
    val enviado: LocalDateTime,
    val destinatario: String,
    /** Solo en los correos de logro. */
    val insignia: Insignia? = null,
    /** Solo en el reporte semanal. */
    val retosCompletados: Int = 0,
    val retosPublicados: Int = 0,
    val aciertos: Int? = null,
)

data class HistorialCorreos(val hijo: HijoVinculado, val hoy: LocalDate, val correos: List<CorreoEnviado>)

/** UI-34: lo que se guarda del menor y el estado de la autorización. */
data class PrivacidadHijo(
    val hijo: HijoVinculado,
    val autorizadaEl: LocalDate,
    val autorizadaPor: String,
    val registros: Int,
    val insignias: Int,
    /** Fecha de la solicitud de eliminación pendiente, si la hay. */
    val solicitudEliminacion: LocalDate?,
)

/** UI-35. */
data class PerfilAcudiente(val nombre: String, val correo: String, val hijos: List<HijoVinculado>)
