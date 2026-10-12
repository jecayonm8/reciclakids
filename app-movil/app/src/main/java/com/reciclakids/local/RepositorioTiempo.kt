package com.reciclakids.local

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** Tiempo de juego por niño y por día, guardado en el teléfono. */
class RepositorioTiempo(private val baseDatos: BaseDatosReciclaKids) {
    private val dao = baseDatos.juegoDao()

    fun observar(ninoId: String, dia: LocalDate): Flow<Long> =
        dao.observarTiempo(ninoId, dia.toString()).map { it ?: 0L }

    suspend fun sumar(ninoId: String, dia: LocalDate, ms: Long) {
        baseDatos.withTransaction {
            val actual = dao.leerTiempo(ninoId, dia.toString()) ?: 0L
            dao.guardarTiempo(TiempoJuegoEntity(ninoId, dia.toString(), actual + ms))
        }
    }
}
