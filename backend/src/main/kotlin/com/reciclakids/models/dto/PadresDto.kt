package com.reciclakids.models.dto

import com.reciclakids.models.CategoriaResiduo
import com.reciclakids.models.Dificultad
import com.reciclakids.models.Insignia
import kotlinx.serialization.Serializable

// Cuerpos de la API del Modo Padres (/api/v1/acudiente), espejo de model/Padres.kt en la app.
// La app arma los mensajes («mejoró separando orgánicos») con estos datos: el servidor no los repite.

@Serializable
data class HijoDto(
    val id: String,
    val nombre: String,
    val grupo: String,
    val edad: Int?,
    val avatar: String,
    val vinculadoEl: String,
)

@Serializable
data class ConteoDto(val aciertos: Int, val total: Int)

/** [confusiones] son id de residuos que el niño echó en la caneca equivocada ese día. */
@Serializable
data class DiaDeJuegoDto(
    val fecha: String,
    val minutos: Int,
    val completo: Boolean,
    val porCategoria: Map<CategoriaResiduo, ConteoDto>,
    val rachaMaxima: Int,
    val confusiones: List<String>,
)

@Serializable
data class LogroDto(val insignia: Insignia, val ganadaEl: String?)

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
    val anterior: SemanaHijoDto?,
    val ultimoLogro: LogroDto?,
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
    val anterior: SemanaHijoDto?,
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

/** [recordatorioSinJugar]: aviso si lleva tres días seguidos sin jugar (se guarda; el correo aún no sale). */
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
    val solicitudEliminacion: String?,
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
