package com.reciclakids.models.dto

import com.reciclakids.models.CategoriaResiduo
import com.reciclakids.models.Dificultad
import com.reciclakids.models.Insignia
import kotlinx.serialization.Serializable

// Cuerpos de la API del Modo Niño (/api/v1/nino). Las fechas viajan en ISO-8601 («2026-10-16»).

/** El reto que juega el niño. [residuos] son los id del catálogo, en el orden del juego. */
@Serializable
data class RetoNinoDto(
    val id: String,
    val codigo: String,
    val fecha: String,
    val dificultad: Dificultad,
    val categorias: List<CategoriaResiduo>,
    val residuos: List<String>,
)

/** Datos mínimos del niño: nombre con la inicial del apellido y avatar (Ley 1581 de 2012). */
@Serializable
data class NinoDto(val id: String, val nombre: String, val avatar: String)

/** Respuesta a un código válido: el reto y los niños del grupo para que cada uno elija su avatar (UI-09). */
@Serializable
data class CodigoRetoDto(val reto: RetoNinoDto, val ninos: List<NinoDto>)

@Serializable
data class SesionNinoSolicitud(val codigo: String, val ninoId: String)

/** El token del niño dura una semana para poder sincronizar lo que jugó sin conexión. */
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

/**
 * Una partida tal como la guardó el teléfono. [idLocal] lo genera la app y no cambia entre
 * reenvíos: el servidor no duplica una partida que ya recibió, solo le suma los intentos nuevos.
 */
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
data class LotePartidasDto(val recibidas: Int, val insigniasNuevas: List<Insignia>)

/** Lo que el servidor sabe del niño: sirve para recuperar el acuario si cambia de teléfono. */
@Serializable
data class ProgresoNinoDto(
    val retosCompletados: Int,
    val rachaEntreRetos: Int,
    val mejorRachaEntreRetos: Int,
    val insignias: List<Insignia>,
    val limiteMinutosDiarios: Int,
)
