package com.reciclakids.network

import com.reciclakids.model.CategoriaResiduo
import com.reciclakids.model.CategoriaResiduo.Aprovechable
import com.reciclakids.model.CategoriaResiduo.NoAprovechable
import com.reciclakids.model.CategoriaResiduo.Organico
import com.reciclakids.model.Confusion
import com.reciclakids.model.Dificultad
import com.reciclakids.model.EstadoNinoHoy
import com.reciclakids.model.EstadoNinoHoy.Completo
import com.reciclakids.model.EstadoNinoHoy.EnCurso
import com.reciclakids.model.EstadoNinoHoy.SinEmpezar
import com.reciclakids.model.EstadoReto
import com.reciclakids.model.Insignia
import com.reciclakids.model.RetoDocente
import com.reciclakids.util.diaHabilAnterior
import com.reciclakids.util.siguientesDiasHabiles
import java.time.LocalDate

/** Un niño del grupo de demostración con todo lo que muestran UI-25, UI-26 y UI-27. */
internal class NinoDemo(
    val id: String,
    val nombre: String,
    val estadoHoy: EstadoNinoHoy,
    val aciertosPorSemana: List<Int>,
    val aciertosPorCategoria: Map<CategoriaResiduo, Int>,
    val confusion: Confusion?,
    val insigniasGanadas: Int,
    /** Retos completados la semana pasada (%). */
    val completadosSemana: Int,
)

/**
 * Grupo de demostración PROVISIONAL, tomado de los prototipos de la Fase 3, para recorrer el
 * Modo Docente mientras el backend no expone retos, resultados ni reportes. Las fechas se
 * calculan desde hoy para que el reto del día y el programado siempre tengan sentido.
 */
internal object DatosDemoDocente {
    const val IdRetoHoy = "reto-lonchera"
    const val CodigoRetoHoy = "4729"

    /** Las 8 insignias del juego, en el orden en que se suelen ganar. */
    val Insignias: List<String> = Insignia.entries.map { it.nombre }

    // Valores de la semana anterior a la del reporte y lo que el backend todavía no calcula.
    const val CompletadosSemanaPrevia = 77
    const val RetosPublicadosSemana = 5
    const val RetosReutilizadosSemana = 2
    const val InsigniasSemana = 31
    const val InsigniasSemanaPrevia = 31
    val ParticipacionPorDia = listOf(82, 91, 68, 95, 86)

    fun retos(hoy: LocalDate): List<RetoDocente> = listOf(
        RetoDocente(
            IdRetoHoy, "Clasificar la lonchera", setOf(Aprovechable, Organico), Dificultad.Medio,
            fecha = hoy, estado = EstadoReto.Publicado, codigo = CodigoRetoHoy, aciertosPromedio = 91,
        ),
        RetoDocente(
            "reto-refrigerio", "Después del refrigerio", setOf(Organico), Dificultad.Facil,
            fecha = siguientesDiasHabiles(hoy, 1).first(), estado = EstadoReto.Programado,
        ),
        RetoDocente(
            "reto-envases", "Envases del salón", setOf(Aprovechable), Dificultad.Medio,
            fecha = diaHabilAnterior(hoy, 2), estado = EstadoReto.Publicado, codigo = "5183", aciertosPromedio = 88,
        ),
        RetoDocente(
            "reto-no-reciclable", "Lo que no se puede reciclar", setOf(NoAprovechable), Dificultad.Dificil,
            fecha = diaHabilAnterior(hoy, 3), estado = EstadoReto.Publicado, codigo = "6030", aciertosPromedio = 72,
        ),
        RetoDocente(
            "reto-mezcla", "Mezcla del día", setOf(Aprovechable, NoAprovechable, Organico), Dificultad.Dificil,
            fecha = null, estado = EstadoReto.Borrador,
        ),
    )

    fun ninos(): List<NinoDemo> = listOf(
        nino("nino-1", "Salomé M.", Completo(3, 8, 8), listOf(78, 85, 92, 100), 100, 90, 100, null, 6, 100),
        nino(
            "nino-2", "Juan T.", Completo(5, 6, 8), listOf(62, 71, 68, 74), 94, 61, 88,
            Confusion("el empaque metalizado", correcta = NoAprovechable, elegida = Aprovechable), 4, 80,
        ),
        nino(
            "nino-3", "Ana L.", EnCurso(4), listOf(55, 60, 66, 70), 80, 58, 72,
            Confusion("la servilleta usada", correcta = NoAprovechable, elegida = Organico), 3, 60,
        ),
        nino("nino-4", "Emilio R.", Completo(4, 7, 8), listOf(70, 76, 80, 87), 90, 82, 88, null, 5, 100),
        nino(
            "nino-5", "Sara P.", SinEmpezar, listOf(40, 48, 52, 50), 62, 45, 55,
            Confusion("la cáscara de banano", correcta = Organico, elegida = NoAprovechable), 1, 40,
        ),
        nino(
            "nino-6", "Tomás G.", EnCurso(2), listOf(58, 64, 70, 69), 78, 60, 74,
            Confusion("el cartón", correcta = Aprovechable, elegida = NoAprovechable), 2, 80,
        ),
        nino("nino-7", "Valeria C.", Completo(3, 8, 8), listOf(85, 90, 95, 100), 100, 96, 100, null, 7, 100),
        nino(
            "nino-8", "Mateo S.", SinEmpezar, listOf(45, 50, 58, 62), 70, 52, 66,
            Confusion("la lata", correcta = Aprovechable, elegida = NoAprovechable), 2, 60,
        ),
        nino(
            "nino-9", "Luciana B.", Completo(6, 5, 8), listOf(60, 58, 66, 63), 72, 55, 70,
            Confusion("los restos de comida", correcta = Organico, elegida = NoAprovechable), 2, 80,
        ),
        nino("nino-10", "Simón H.", Completo(4, 7, 8), listOf(72, 80, 84, 88), 92, 80, 90, null, 5, 100),
    )

    @Suppress("LongParameterList")
    private fun nino(
        id: String,
        nombre: String,
        hoy: EstadoNinoHoy,
        semanas: List<Int>,
        aprovechables: Int,
        noAprovechables: Int,
        organicos: Int,
        confusion: Confusion?,
        insignias: Int,
        completados: Int,
    ) = NinoDemo(
        id, nombre, hoy, semanas,
        mapOf(Aprovechable to aprovechables, NoAprovechable to noAprovechables, Organico to organicos),
        confusion, insignias, completados,
    )
}
