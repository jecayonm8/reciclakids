package com.reciclakids.network

import com.reciclakids.model.CatalogoResiduos
import com.reciclakids.model.CategoriaResiduo
import com.reciclakids.model.Conteo
import com.reciclakids.model.DiaDeJuego
import com.reciclakids.model.Dificultad
import com.reciclakids.model.EstadoPartida
import com.reciclakids.model.HijoVinculado
import com.reciclakids.model.Insignia
import com.reciclakids.model.ProgresoNino
import com.reciclakids.model.Residuo
import com.reciclakids.model.inicioSemana
import com.reciclakids.model.insigniasNuevas
import java.time.LocalDate

/**
 * Familia de demostración PROVISIONAL, tomada de los prototipos de la Fase 4, para recorrer el
 * Modo Padres mientras el backend no expone resultados. Salomé es la misma niña del grupo de la
 * docente (mismo id); Martín está vinculado pero todavía no ha jugado, así que muestra los estados
 * vacíos. Las fechas se calculan desde hoy: la semana actual solo trae los días que ya pasaron.
 */
internal object DatosDemoPadres {

    /** Así juega un día: aciertos al primer intento de cada caneca, sobre 2 residuos de cada una (Medio). */
    private class Jugada(
        val minutos: Int,
        val aprovechables: Int,
        val organicos: Int,
        val noAprovechables: Int,
        val racha: Int,
        /** El no aprovechable que confunde ese día. */
        val confunde: Residuo = CatalogoResiduos.empaqueMetalizado,
    )

    private const val ResiduosPorCaneca = 2

    /** Cuatro semanas de lunes a viernes, de la más antigua a la actual; `null` si ese día no jugó. */
    private val semanasSalome: List<List<Jugada?>> = listOf(
        listOf(Jugada(12, 2, 1, 1, 3), null, Jugada(15, 1, 2, 0, 3, CatalogoResiduos.servilleta), null, Jugada(14, 2, 1, 1, 2)),
        listOf(Jugada(16, 2, 2, 1, 5), Jugada(13, 2, 1, 1, 3), null, Jugada(18, 2, 2, 1, 4), null),
        listOf(Jugada(15, 2, 1, 1, 3), null, Jugada(17, 2, 1, 1, 2), Jugada(20, 2, 2, 1, 4), null),
        listOf(Jugada(16, 2, 2, 1, 4), Jugada(20, 2, 2, 1, 5), null, Jugada(18, 2, 2, 2, 6), Jugada(18, 2, 1, 1, 3)),
    )

    const val IdSalome = "nino-1"
    const val IdMartin = "nino-a7"
    const val IdEmilio = "nino-4"

    /** Lunes de la semana más antigua de la demo. */
    fun primeraSemana(hoy: LocalDate): LocalDate = inicioSemana(hoy).minusWeeks(semanasSalome.size - 1L)

    private fun vinculadoEl(hoy: LocalDate): LocalDate = primeraSemana(hoy).minusDays(3)

    fun salome(hoy: LocalDate) = HijoVinculado(IdSalome, "Salomé M.", "Jardín B", 5, "Pulpo azul", vinculadoEl(hoy))
    fun martin(hoy: LocalDate) = HijoVinculado(IdMartin, "Martín M.", "Jardín A", 4, "Tortuga verde", vinculadoEl(hoy))
    fun emilio(hoy: LocalDate) = HijoVinculado(IdEmilio, "Emilio R.", "Jardín B", 5, "Pez amarillo", hoy)

    /** Códigos de vinculación que entregan las docentes, uno por niño. */
    val codigos: Map<String, String> = mapOf("JB-2M91" to IdSalome, "JA-7K3P" to IdMartin, "JB-5H8W" to IdEmilio)

    fun porId(id: String, hoy: LocalDate): HijoVinculado? = when (id) {
        IdSalome -> salome(hoy)
        IdMartin -> martin(hoy)
        IdEmilio -> emilio(hoy)
        else -> null
    }

    /** Los días que Salomé jugó hasta hoy. */
    fun diasSalome(hoy: LocalDate): List<DiaDeJuego> {
        val inicio = primeraSemana(hoy)
        return semanasSalome.flatMapIndexed { semana, jugadas ->
            jugadas.mapIndexedNotNull { dia, jugada ->
                val fecha = inicio.plusWeeks(semana.toLong()).plusDays(dia.toLong())
                jugada?.takeUnless { fecha.isAfter(hoy) }?.let { diaDeJuego(fecha, it) }
            }
        }
    }

    private fun diaDeJuego(fecha: LocalDate, jugada: Jugada): DiaDeJuego {
        val aciertos = mapOf(
            CategoriaResiduo.Aprovechable to jugada.aprovechables,
            CategoriaResiduo.Organico to jugada.organicos,
            CategoriaResiduo.NoAprovechable to jugada.noAprovechables,
        )
        val confundidos = mapOf(
            CategoriaResiduo.Aprovechable to CatalogoResiduos.papel,
            CategoriaResiduo.Organico to CatalogoResiduos.restosComida,
            CategoriaResiduo.NoAprovechable to jugada.confunde,
        )
        return DiaDeJuego(
            fecha = fecha,
            minutos = jugada.minutos,
            completo = true,
            porCategoria = aciertos.mapValues { Conteo(it.value, ResiduosPorCaneca) },
            rachaMaxima = jugada.racha,
            confusiones = aciertos.flatMap { (categoria, bien) -> List(ResiduosPorCaneca - bien) { confundidos.getValue(categoria) } },
        )
    }

    /**
     * Repite las reglas del juego sobre los días jugados para saber qué insignia ganó y cuándo: así
     * la demo nunca muestra una insignia que el niño no habría podido ganar.
     */
    fun insignias(ninoId: String, dias: List<DiaDeJuego>): Map<Insignia, LocalDate> {
        val ganadas = linkedMapOf<Insignia, LocalDate>()
        var completados = 0
        dias.sortedBy { it.fecha }.filter { it.completo }.forEachIndexed { i, dia ->
            completados++
            val nuevas = insigniasNuevas(
                estado = EstadoPartida(residuos = emptyList(), errores = dia.errores, rachaMaxima = dia.rachaMaxima),
                dificultad = Dificultad.Medio,
                aTiempo = false,
                progreso = ProgresoNino(ninoId, retosCompletados = completados),
                diasConReto = i + 1,
                yaGanadas = ganadas.keys,
            )
            nuevas.forEach { ganadas[it] = dia.fecha }
        }
        return ganadas
    }
}
