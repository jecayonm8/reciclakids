package com.reciclakids.network

import com.reciclakids.model.CategoriaResiduo
import com.reciclakids.model.CategoriaResiduo.Aprovechables
import com.reciclakids.model.CategoriaResiduo.NoAprovechables
import com.reciclakids.model.CategoriaResiduo.Organicos
import com.reciclakids.model.Confusion
import com.reciclakids.model.Dificultad
import com.reciclakids.model.EstadoNinoHoy
import com.reciclakids.model.EstadoNinoHoy.Completo
import com.reciclakids.model.EstadoNinoHoy.EnCurso
import com.reciclakids.model.EstadoNinoHoy.SinEmpezar
import com.reciclakids.model.EstadoReto
import com.reciclakids.model.Reto
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

    val Insignias = listOf(
        "Amiga tortuga", "Rápido como pez", "Racha de 5", "5 retos diarios",
        "Pulpo ordenado", "Agua cristalina", "Coral feliz", "Guardián del mar",
    )

    // Valores de la semana anterior a la del reporte y lo que el backend todavía no calcula.
    const val CompletadosSemanaPrevia = 77
    const val RetosPublicadosSemana = 5
    const val RetosReutilizadosSemana = 2
    const val InsigniasSemana = 31
    const val InsigniasSemanaPrevia = 31
    val ParticipacionPorDia = listOf(82, 91, 68, 95, 86)

    fun retos(hoy: LocalDate): List<Reto> = listOf(
        Reto(
            IdRetoHoy, "Clasificar la lonchera", setOf(Aprovechables, Organicos), Dificultad.Medio,
            fecha = hoy, estado = EstadoReto.Publicado, codigo = CodigoRetoHoy, aciertosPromedio = 91,
        ),
        Reto(
            "reto-refrigerio", "Después del refrigerio", setOf(Organicos), Dificultad.Facil,
            fecha = siguientesDiasHabiles(hoy, 1).first(), estado = EstadoReto.Programado,
        ),
        Reto(
            "reto-envases", "Envases del salón", setOf(Aprovechables), Dificultad.Medio,
            fecha = diaHabilAnterior(hoy, 2), estado = EstadoReto.Publicado, codigo = "5183", aciertosPromedio = 88,
        ),
        Reto(
            "reto-no-reciclable", "Lo que no se puede reciclar", setOf(NoAprovechables), Dificultad.Dificil,
            fecha = diaHabilAnterior(hoy, 3), estado = EstadoReto.Publicado, codigo = "6030", aciertosPromedio = 72,
        ),
        Reto(
            "reto-mezcla", "Mezcla del día", setOf(Aprovechables, NoAprovechables, Organicos), Dificultad.Dificil,
            fecha = null, estado = EstadoReto.Borrador,
        ),
    )

    fun ninos(): List<NinoDemo> = listOf(
        nino("n01", "Salomé M.", Completo(3, 8, 8), listOf(78, 85, 92, 100), 100, 90, 100, null, 6, 100),
        nino(
            "n02", "Juan T.", Completo(5, 6, 8), listOf(62, 71, 68, 74), 94, 61, 88,
            Confusion("el empaque metalizado", correcta = NoAprovechables, elegida = Aprovechables), 4, 80,
        ),
        nino(
            "n03", "Ana L.", EnCurso(4), listOf(55, 60, 66, 70), 80, 58, 72,
            Confusion("la servilleta usada", correcta = NoAprovechables, elegida = Organicos), 3, 60,
        ),
        nino("n04", "Emilio R.", Completo(4, 7, 8), listOf(70, 76, 80, 87), 90, 82, 88, null, 5, 100),
        nino(
            "n05", "Sara P.", SinEmpezar, listOf(40, 48, 52, 50), 62, 45, 55,
            Confusion("la cáscara de banano", correcta = Organicos, elegida = NoAprovechables), 1, 40,
        ),
        nino(
            "n06", "Tomás G.", EnCurso(2), listOf(58, 64, 70, 69), 78, 60, 74,
            Confusion("el cartón", correcta = Aprovechables, elegida = NoAprovechables), 2, 80,
        ),
        nino("n07", "Valeria C.", Completo(3, 8, 8), listOf(85, 90, 95, 100), 100, 96, 100, null, 7, 100),
        nino(
            "n08", "Mateo B.", SinEmpezar, listOf(45, 50, 58, 62), 70, 52, 66,
            Confusion("la lata", correcta = Aprovechables, elegida = NoAprovechables), 2, 60,
        ),
        nino(
            "n09", "Luciana D.", Completo(6, 5, 8), listOf(60, 58, 66, 63), 72, 55, 70,
            Confusion("los restos de comida", correcta = Organicos, elegida = NoAprovechables), 2, 80,
        ),
        nino("n10", "Simón V.", Completo(4, 7, 8), listOf(72, 80, 84, 88), 92, 80, 90, null, 5, 100),
        nino("n11", "Isabela F.", Completo(5, 7, 8), listOf(68, 74, 79, 86), 88, 78, 90, null, 4, 100),
        nino("n12", "Martín H.", Completo(4, 8, 8), listOf(80, 86, 90, 97), 98, 90, 96, null, 6, 100),
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
        mapOf(Aprovechables to aprovechables, NoAprovechables to noAprovechables, Organicos to organicos),
        confusion, insignias, completados,
    )
}
