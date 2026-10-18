package com.reciclakids.network.api

import com.reciclakids.model.CategoriaResiduo
import com.reciclakids.model.Dificultad
import com.reciclakids.model.EstadoReto
import com.reciclakids.model.Insignia
import com.reciclakids.model.RolAdulto
import kotlinx.serialization.Serializable

// Cuerpos de la API REST /api/v1, copia de backend/src/main/kotlin/com/reciclakids/models/dto. Los
// enums viajan con el nombre del enum de la app y las fechas en ISO-8601 («2026-10-16»).

/** Todo error de la API: la app decide por [error], un código estable. */
@Serializable
data class ErrorDto(val error: String, val mensaje: String = "")

// --- Autenticación (/auth y /cuenta) ---

@Serializable
data class RegistroDocenteSolicitud(
    val nombre: String,
    val correo: String,
    val jardin: String,
    val grupo: String,
    val contrasena: String,
)

@Serializable
data class RegistroAcudienteSolicitud(
    val nombre: String,
    val correo: String,
    val contrasena: String,
    val codigoVinculacion: String,
    val autorizaDatosMenor: Boolean,
)

@Serializable
data class InicioSesionSolicitud(val correo: String, val contrasena: String)

@Serializable
data class RefrescoSolicitud(val tokenRefresco: String)

@Serializable
data class RecuperacionSolicitud(val correo: String)

@Serializable
data class CuentaDto(
    val nombre: String,
    val correo: String,
    val rol: RolAdulto,
    val jardin: String? = null,
    val grupo: String? = null,
)

@Serializable
data class SesionDto(
    val tokenAcceso: String,
    val venceEnSegundos: Long,
    val tokenRefresco: String,
    val cuenta: CuentaDto,
)

// --- Modo Niño (/nino) ---

@Serializable
data class RetoNinoDto(
    val id: String,
    val codigo: String,
    val fecha: String,
    val dificultad: Dificultad,
    val categorias: List<CategoriaResiduo>,
    val residuos: List<String>,
)

@Serializable
data class NinoDto(val id: String, val nombre: String, val avatar: String = "")

@Serializable
data class CodigoRetoDto(val reto: RetoNinoDto, val ninos: List<NinoDto>)

@Serializable
data class SesionNinoSolicitud(val codigo: String, val ninoId: String)

@Serializable
data class SesionNinoDto(
    val token: String,
    val venceEnSegundos: Long,
    val nino: NinoDto,
    val reto: RetoNinoDto,
    val limiteMinutosDiarios: Int,
)

@Serializable
data class IntentoDto(
    val residuoId: String,
    val categoriaElegida: CategoriaResiduo,
    val correcto: Boolean,
    val momentoMs: Long,
)

@Serializable
data class PartidaDto(
    val idLocal: String,
    val retoId: String,
    val terminada: Boolean,
    val aciertos: Int,
    val errores: Int,
    val rachaMaxima: Int,
    val tiempoMs: Long,
    val intentos: List<IntentoDto>,
)

@Serializable
data class LotePartidasSolicitud(val partidas: List<PartidaDto>)

@Serializable
data class LotePartidasDto(val recibidas: Int, val insigniasNuevas: List<Insignia> = emptyList())

@Serializable
data class ProgresoNinoDto(
    val retosCompletados: Int,
    val rachaEntreRetos: Int,
    val mejorRachaEntreRetos: Int,
    val insignias: List<Insignia>,
    val limiteMinutosDiarios: Int,
)

// --- Modo Docente (/docente) ---

@Serializable
data class RetoDocenteDto(
    val id: String,
    val nombre: String,
    val categorias: List<CategoriaResiduo>,
    val dificultad: Dificultad,
    val fecha: String? = null,
    val estado: EstadoReto,
    val codigo: String? = null,
    val aciertosPromedio: Int? = null,
    val reutilizadoDe: String? = null,
)

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
    val retoHoy: RetoDocenteDto? = null,
    val completaron: Int,
    val totalNinos: Int,
    val aciertosPromedio: Int? = null,
)

@Serializable
data class BibliotecaDto(val hoy: String, val retos: List<RetoDocenteDto>)

@Serializable
data class IngresosDto(val ninosQueEntraron: Int)

@Serializable
enum class TipoEstadoNino { Completo, EnCurso, SinEmpezar }

@Serializable
data class EstadoNinoHoyDto(
    val tipo: TipoEstadoNino,
    val minutos: Int? = null,
    val aciertos: Int? = null,
    val total: Int? = null,
    val residuoActual: Int? = null,
)

@Serializable
data class NinoHoyDto(val id: String, val nombre: String, val estado: EstadoNinoHoyDto)

@Serializable
data class GrupoHoyDto(val nombreGrupo: String, val retoHoy: String? = null, val ninos: List<NinoHoyDto>)

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
    val confusion: ConfusionDto? = null,
    val insignias: List<InsigniaNinoDto>,
    val codigoVinculacion: String,
    val acudienteVinculado: Boolean,
)

@Serializable
data class AgregarNinoSolicitud(val nombre: String, val inicialApellido: String)

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

// --- Modo Padres (/acudiente) ---

@Serializable
data class HijoDto(
    val id: String,
    val nombre: String,
    val grupo: String,
    val edad: Int? = null,
    val avatar: String = "",
    val vinculadoEl: String,
)

@Serializable
data class ConteoDto(val aciertos: Int, val total: Int)

@Serializable
data class DiaDeJuegoDto(
    val fecha: String,
    val minutos: Int,
    val completo: Boolean,
    val porCategoria: Map<CategoriaResiduo, ConteoDto>,
    val rachaMaxima: Int,
    val confusiones: List<String> = emptyList(),
)

@Serializable
data class LogroDto(val insignia: Insignia, val ganadaEl: String? = null)

@Serializable
data class SemanaHijoDto(
    val inicio: String,
    val retosPublicados: Int,
    val dias: List<DiaDeJuegoDto>,
    val insignias: List<LogroDto>,
)

@Serializable
data class InicioPadresDto(
    val hijo: HijoDto,
    val hoy: String,
    val semana: SemanaHijoDto,
    val anterior: SemanaHijoDto? = null,
    val ultimoLogro: LogroDto? = null,
    val insigniasGanadas: Int,
    val limiteMinutos: Int,
    val sinDatos: Boolean,
)

@Serializable
data class PuntoEvolucionDto(val semana: String, val aciertos: Int)

@Serializable
data class ReporteHijoDto(
    val hijo: HijoDto,
    val hoy: String,
    val semanas: List<String>,
    val semana: SemanaHijoDto,
    val anterior: SemanaHijoDto? = null,
    val evolucion: List<PuntoEvolucionDto>,
    val limiteMinutos: Int,
)

@Serializable
data class LogrosHijoDto(
    val hijo: HijoDto,
    val hoy: String,
    val logros: List<LogroDto>,
    val retosCompletados: Int,
    val diasConReto: Int,
)

@Serializable
enum class TipoCorreoDto { Logro, ReporteSemanal }

@Serializable
data class CorreoEnviadoDto(
    val tipo: TipoCorreoDto,
    val enviado: String,
    val destinatario: String,
    val insignia: Insignia? = null,
    val retosCompletados: Int = 0,
    val retosPublicados: Int = 0,
    val aciertos: Int? = null,
)

@Serializable
data class HistorialCorreosDto(val hijo: HijoDto, val hoy: String, val correos: List<CorreoEnviadoDto>)

@Serializable
data class ControlParentalDto(
    val limiteMinutosDiarios: Int = 20,
    val dificultadSugerida: Dificultad = Dificultad.Medio,
    val correosLogro: Boolean = true,
    val reporteSemanal: Boolean = true,
    val recordatorioSinJugar: Boolean = false,
)

@Serializable
data class PrivacidadHijoDto(
    val hijo: HijoDto,
    val autorizadaEl: String,
    val autorizadaPor: String,
    val registros: Int,
    val insignias: Int,
    val solicitudEliminacion: String? = null,
)

@Serializable
data class SolicitudEliminacionDto(val solicitadaEl: String)

@Serializable
data class PerfilAcudienteDto(val nombre: String, val correo: String, val hijos: List<HijoDto>)

@Serializable
data class PerfilAcudienteSolicitud(val nombre: String, val correo: String)

@Serializable
data class VincularSolicitud(val codigo: String)

@Serializable
data class VinculacionDto(val hijo: HijoDto, val yaEstaba: Boolean)
