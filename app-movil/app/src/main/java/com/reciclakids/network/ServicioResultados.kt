package com.reciclakids.network

import com.reciclakids.model.CategoriaResiduo
import com.reciclakids.model.Insignia
import kotlinx.coroutines.delay

/** Intento de clasificación tal como viaja al backend (API de Resultados). */
data class IntentoEnviado(
    val residuoId: String,
    val categoriaElegida: CategoriaResiduo,
    val correcto: Boolean,
    val momentoMs: Long,
)

/**
 * Una partida completa tal como viaja al backend: su estado y todos sus intentos. [idLocal] no
 * cambia entre reenvíos, así que reenviarla no la duplica (ADR-04).
 */
data class PartidaEnviada(
    val idLocal: String,
    val retoId: String,
    val terminada: Boolean,
    val aciertos: Int,
    val errores: Int,
    val rachaMaxima: Int,
    val tiempoMs: Long,
    val intentos: List<IntentoEnviado>,
)

sealed interface ResultadoEnvio {
    /** El backend guardó todo el lote. [insigniasNuevas] son las que otorgó él al evaluarlo. */
    data class Recibido(val insigniasNuevas: List<Insignia> = emptyList()) : ResultadoEnvio

    /**
     * El niño no tiene sesión en este teléfono y no se le pudo abrir (su código ya venció): sus
     * partidas esperan guardadas a que vuelva a entrar con el código de otro día.
     */
    data object SinSesion : ResultadoEnvio

    /** El backend no acepta estas partidas (por ejemplo, el niño ya no está en el grupo). Se conservan. */
    data object Rechazado : ResultadoEnvio

    /** Sin red o falla del servidor: se reintenta más tarde. */
    data object SinConexion : ResultadoEnvio
}

/** Envía al backend los resultados que el juego guardó en el teléfono. */
interface ServicioResultados {
    /**
     * Envía las partidas de un niño con su propio token. [codigoHoy] es el código de un reto de
     * hoy que jugó: sirve para abrirle la sesión si no tiene una vigente.
     */
    suspend fun enviar(ninoId: String, partidas: List<PartidaEnviada>, codigoHoy: String?): ResultadoEnvio
}

/** Acepta todo sin guardarlo en ninguna parte: para la demostración sin backend y las pruebas. */
class ServicioResultadosEnMemoria(private val latenciaMs: Long = 300) : ServicioResultados {
    override suspend fun enviar(ninoId: String, partidas: List<PartidaEnviada>, codigoHoy: String?): ResultadoEnvio {
        delay(latenciaMs)
        return ResultadoEnvio.Recibido()
    }
}
