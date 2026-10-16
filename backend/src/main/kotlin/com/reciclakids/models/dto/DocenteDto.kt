package com.reciclakids.models.dto

import com.reciclakids.models.CategoriaResiduo
import com.reciclakids.models.Dificultad
import com.reciclakids.models.EstadoReto
import kotlinx.serialization.Serializable

// Cuerpos de la API del Modo Docente (/api/v1/docente), espejo de los modelos de la app
// (model/Grupo.kt y model/RetoDocente.kt).

@Serializable
data class RetoDocenteDto(
    val id: String,
    val nombre: String,
    val categorias: List<CategoriaResiduo>,
    val dificultad: Dificultad,
    val fecha: String?,
    val estado: EstadoReto,
    val codigo: String? = null,
    val aciertosPromedio: Int? = null,
    val reutilizadoDe: String? = null,
)

/** Hoy publica ya; un día posterior lo deja programado. [idExistente] edita un programado. */
@Serializable
data class RetoBorradorSolicitud(
    val categorias: List<CategoriaResiduo>,
    val dificultad: Dificultad,
    val fecha: String,
    val idExistente: String? = null,
    val nombre: String? = null,
    val reutilizadoDe: String? = null,
)

@Serializable
data class TableroDto(
    val hoy: String,
    val nombreDocente: String,
    val grupo: String,
    val retoHoy: RetoDocenteDto?,
    val completaron: Int,
    val totalNinos: Int,
    val aciertosPromedio: Int?,
)

@Serializable
data class BibliotecaDto(val hoy: String, val retos: List<RetoDocenteDto>)

@Serializable
data class IngresosDto(val ninosQueEntraron: Int)

/** Estado de un niño con el reto de hoy. Según [tipo] vienen unos campos u otros. */
@Serializable
data class EstadoNinoHoyDto(
    val tipo: TipoEstadoNino,
    val minutos: Int? = null,
    val aciertos: Int? = null,
    val total: Int? = null,
    val residuoActual: Int? = null,
)

@Serializable
enum class TipoEstadoNino { Completo, EnCurso, SinEmpezar }

@Serializable
data class NinoHoyDto(val id: String, val nombre: String, val estado: EstadoNinoHoyDto)

@Serializable
data class GrupoHoyDto(val nombreGrupo: String, val retoHoy: String?, val ninos: List<NinoHoyDto>)

@Serializable
data class InsigniaNinoDto(val nombre: String, val ganada: Boolean)

@Serializable
data class ConfusionDto(val residuo: String, val correcta: CategoriaResiduo, val elegida: CategoriaResiduo)

@Serializable
data class DetalleNinoDto(
    val id: String,
    val nombre: String,
    val aciertosPorSemana: List<Int>,
    val aciertosPorCategoria: Map<CategoriaResiduo, Int>,
    val confusion: ConfusionDto?,
    val insignias: List<InsigniaNinoDto>,
    val codigoVinculacion: String,
    val acudienteVinculado: Boolean,
)

@Serializable
data class AgregarNinoSolicitud(val nombre: String, val inicialApellido: String, val edad: Int? = null)

@Serializable
data class NinoAgregadoDto(val id: String, val nombre: String, val codigoVinculacion: String)

@Serializable
data class CodigoVinculacionDto(val codigoVinculacion: String)

@Serializable
data class ValorDiaDto(val fecha: String, val porcentaje: Int)

@Serializable
data class ValorNinoDto(val nombre: String, val porcentaje: Int)

@Serializable
data class ReporteSemanalDto(
    val inicio: String,
    val fin: String,
    val completadosPct: Int,
    val cambioCompletados: Int,
    val aciertosPct: Int,
    val cambioAciertos: Int,
    val retosPublicados: Int,
    val retosReutilizados: Int,
    val insignias: Int,
    val cambioInsignias: Int,
    val participacionPorDia: List<ValorDiaDto>,
    val completadosPorNino: List<ValorNinoDto>,
)

@Serializable
data class AvisosDocenteDto(
    val confirmarPublicacion: Boolean = true,
    val resumenDiario: Boolean = true,
    val ninosSinJugar: Boolean = false,
)

@Serializable
data class PerfilDocenteDto(
    val nombre: String,
    val correo: String,
    val jardin: String,
    val grupo: String,
    val totalNinos: Int,
    val avisos: AvisosDocenteDto,
)

@Serializable
data class PerfilDocenteSolicitud(val nombre: String, val correo: String, val jardin: String, val grupo: String)

@Serializable
data class EnvioReporteDto(val correosEnviados: Int)
