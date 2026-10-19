package com.reciclakids.local

import com.reciclakids.model.ControlParental
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** El límite diario de juego de cada niño, que el Modo Niño hace cumplir (UI-19). */
interface LimitesJuego {
    fun observar(ninoId: String): Flow<ControlParental>
}

/**
 * Los límites que da el backend al abrir la sesión del niño (y al sincronizar, con su progreso),
 * guardados en el teléfono para que valgan también sin conexión. Mientras el backend no haya dado
 * uno, vale el de [ControlParental] por defecto.
 */
class LimitesJuegoLocales(private val dao: JuegoDao) : LimitesJuego {

    override fun observar(ninoId: String): Flow<ControlParental> = dao.observarLimite(ninoId)
        .map { minutos -> ControlParental(limiteMinutosDiarios = minutos ?: ControlParental().limiteMinutosDiarios) }
        .distinctUntilChanged()

    suspend fun guardar(ninoId: String, limiteMinutosDiarios: Int) {
        dao.guardarLimite(LimiteJuegoEntity(ninoId, limiteMinutosDiarios))
    }
}
