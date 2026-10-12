package com.reciclakids.network

import kotlinx.coroutines.delay

/** Intento de clasificación tal como viaja al backend (API de Resultados). */
data class IntentoEnviado(
    val ninoId: String,
    val codigoReto: String,
    val residuoId: String,
    val categoriaElegida: String,
    val correcto: Boolean,
    val momentoMs: Long,
)

/** Envía al backend los resultados que el juego guardó en el teléfono. */
interface ServicioResultados {
    /** `true` si el backend recibió el lote completo; `false` para reintentar más tarde. */
    suspend fun enviar(intentos: List<IntentoEnviado>): Boolean
}

/**
 * Implementación PROVISIONAL mientras el backend no exponga la API de resultados: acepta todo y
 * no lo guarda en ninguna parte. Sirve para recorrer el ciclo guardado local → sincronizado.
 */
class ServicioResultadosEnMemoria(private val latenciaMs: Long = 300) : ServicioResultados {
    override suspend fun enviar(intentos: List<IntentoEnviado>): Boolean {
        delay(latenciaMs)
        return true
    }
}
