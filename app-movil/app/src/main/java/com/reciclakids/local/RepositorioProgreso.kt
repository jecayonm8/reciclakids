package com.reciclakids.local

import com.reciclakids.model.ProgresoNino
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Progreso de cada niño guardado en el teléfono. */
class RepositorioProgreso(private val dao: ProgresoNinoDao) {

    fun observar(ninoId: String): Flow<ProgresoNino> =
        dao.observar(ninoId).map { it?.aModelo() ?: ProgresoNino(ninoId) }

    suspend fun leer(ninoId: String): ProgresoNino = dao.leer(ninoId)?.aModelo() ?: ProgresoNino(ninoId)

    suspend fun marcarTutorialVisto(ninoId: String) {
        dao.guardar(leer(ninoId).copy(tutorialVisto = true).aEntidad())
    }
}
