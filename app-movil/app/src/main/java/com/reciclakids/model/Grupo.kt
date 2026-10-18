package com.reciclakids.model

import java.time.LocalDate

// Datos mínimos del niño (Ley 1581 de 2012): nombre con la inicial del apellido, avatar y resultados.

/** Cómo va un niño con el reto de hoy. */
sealed interface EstadoNinoHoy {
    data class Completo(val minutos: Int, val aciertos: Int, val total: Int) : EstadoNinoHoy
    data class EnCurso(val residuoActual: Int) : EstadoNinoHoy
    data object SinEmpezar : EstadoNinoHoy
}

/** [nombre] ya viene como «Salomé M.»: nunca el apellido completo. */
data class NinoHoy(val id: String, val nombre: String, val estado: EstadoNinoHoy)

/** «Salomé» y «m» → «Salomé M.»: es lo único que se guarda del nombre de un niño. */
fun nombreConInicial(nombre: String, inicialApellido: String): String =
    "${nombre.trim()} ${inicialApellido.trim().uppercase()}."

/** Lo que se escribe en el campo de la inicial del apellido: solo su primera letra, en mayúscula. */
fun inicialDe(texto: String): String = texto.firstOrNull { it.isLetter() }?.uppercase().orEmpty()

/** Un niño recién agregado al grupo, con su propio código para que su acudiente se vincule. */
data class NinoAgregado(val id: String, val nombre: String, val codigoVinculacion: String)

data class GrupoHoy(val nombreGrupo: String, val retoHoy: String?, val ninos: List<NinoHoy>)

data class InsigniaNino(val nombre: String, val ganada: Boolean)

/** El residuo que más confunde: lo lleva a [elegida] cuando va en [correcta]. */
data class Confusion(val residuo: String, val correcta: CategoriaResiduo, val elegida: CategoriaResiduo)

data class DetalleNino(
    val id: String,
    val nombre: String,
    /** Aciertos (%) de las últimas semanas, de la más antigua a la más reciente. */
    val aciertosPorSemana: List<Int>,
    val aciertosPorCategoria: Map<CategoriaResiduo, Int>,
    val confusion: Confusion?,
    val insignias: List<InsigniaNino>,
    /** Lo usa su acudiente una vez para quedar vinculado solo a este niño («JB-2M91»). */
    val codigoVinculacion: String,
    val acudienteVinculado: Boolean,
) {
    val sinResultados: Boolean get() = aciertosPorSemana.isEmpty()
}

/** UI-21: si hay reto hoy y cómo va el grupo con él. */
data class TableroDocente(
    val hoy: LocalDate,
    val nombreDocente: String,
    val grupo: String,
    val retoHoy: RetoDocente?,
    val completaron: Int,
    val totalNinos: Int,
    /** `null` mientras ningún niño haya terminado. */
    val aciertosPromedio: Int?,
)

data class BibliotecaDocente(val hoy: LocalDate, val retos: List<RetoDocente>)

data class ValorDia(val fecha: LocalDate, val porcentaje: Int)

data class ValorNino(val nombre: String, val porcentaje: Int)

/** UI-26: la semana escolar anterior, comparada con la previa. Los cambios van en puntos. */
data class ReporteSemanal(
    val inicio: LocalDate,
    val fin: LocalDate,
    val completadosPct: Int,
    val cambioCompletados: Int,
    val aciertosPct: Int,
    val cambioAciertos: Int,
    val retosPublicados: Int,
    val retosReutilizados: Int,
    val insignias: Int,
    val cambioInsignias: Int,
    val participacionPorDia: List<ValorDia>,
    val completadosPorNino: List<ValorNino>,
) {
    val sinDatos: Boolean get() = completadosPorNino.isEmpty()
}

data class AvisosDocente(
    val confirmarPublicacion: Boolean = true,
    val resumenDiario: Boolean = true,
    val ninosSinJugar: Boolean = false,
)

data class PerfilDocente(
    val nombre: String,
    val correo: String,
    val jardin: String,
    val grupo: String,
    val totalNinos: Int,
    val avisos: AvisosDocente,
)
