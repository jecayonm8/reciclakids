package com.reciclakids.local

import com.reciclakids.model.AjustesNino
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Volumen de la voz y de la música del Modo Niño, guardado en el teléfono. */
class RepositorioAjustes(private val dao: JuegoDao) {

    fun observar(): Flow<AjustesNino> = dao.ajustes().map { entidad ->
        if (entidad == null) AjustesNino() else AjustesNino(entidad.volumenSonidos, entidad.volumenMusica)
    }

    suspend fun guardar(ajustes: AjustesNino) {
        dao.guardarAjustes(AjustesNinoEntity(volumenSonidos = ajustes.volumenSonidos, volumenMusica = ajustes.volumenMusica))
    }
}
