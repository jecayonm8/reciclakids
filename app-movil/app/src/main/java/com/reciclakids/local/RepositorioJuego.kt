package com.reciclakids.local

import androidx.room.withTransaction
import com.reciclakids.model.CategoriaResiduo
import com.reciclakids.model.EstadoPartida
import com.reciclakids.model.Insignia
import com.reciclakids.model.Intento
import com.reciclakids.model.LimiteDificilMs
import com.reciclakids.model.ProgresoNino
import com.reciclakids.model.Reto
import com.reciclakids.model.RetoCompletado
import com.reciclakids.model.ResultadoReto
import com.reciclakids.model.insigniasNuevas
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Partida abierta: la nueva o la que quedó a medias. */
data class PartidaAbierta(val id: Long, val estado: EstadoPartida, val tiempoMs: Long)

/**
 * Partidas, intentos e insignias guardados en el teléfono. Cada intento se escribe de inmediato
 * para que un cierre inesperado no pierda nada (RNF: 0 % de pérdida).
 */
class RepositorioJuego(
    private val baseDatos: BaseDatosReciclaKids,
    private val ahora: () -> Long = System::currentTimeMillis,
) {
    private val dao = baseDatos.juegoDao()
    private val progresoDao = baseDatos.progresoDao()

    /** Retoma la partida sin terminar de este reto, o empieza una nueva. */
    suspend fun abrirPartida(ninoId: String, reto: Reto): PartidaAbierta {
        val fecha = reto.fecha.toString()
        val partida = dao.partidaEnCurso(ninoId, reto.codigo, fecha)
            ?: PartidaEntity(ninoId = ninoId, codigoReto = reto.codigo, fecha = fecha).let { nueva ->
                nueva.copy(id = dao.crearPartida(nueva))
            }
        val estado = EstadoPartida(
            residuos = reto.residuos,
            indice = partida.indice.coerceAtMost(reto.residuos.size),
            aciertos = partida.aciertos,
            errores = partida.errores,
            racha = partida.racha,
            rachaMaxima = partida.rachaMaxima,
            puntaje = partida.puntaje,
        )
        return PartidaAbierta(partida.id, estado, partida.tiempoMs)
    }

    /** Deja la partida actual y empieza otra desde el primer residuo (botón «Reiniciar»). */
    suspend fun reiniciarPartida(partidaId: Long, ninoId: String, reto: Reto): PartidaAbierta {
        dao.abandonarPartida(partidaId)
        return abrirPartida(ninoId, reto)
    }

    suspend fun registrarIntento(partidaId: Long, intento: Intento, elegida: CategoriaResiduo, tiempoMs: Long) {
        baseDatos.withTransaction {
            dao.registrarIntento(
                IntentoEntity(
                    partidaId = partidaId,
                    residuoId = intento.residuo.id,
                    categoriaElegida = elegida.name,
                    correcto = intento.correcto,
                    momentoMs = ahora(),
                )
            )
            guardarAvance(partidaId, intento.estado, tiempoMs)
        }
    }

    /**
     * Cierra la partida: suma el reto al acuario la primera vez que se completa en el día y otorga
     * las insignias que correspondan.
     */
    suspend fun terminarPartida(
        partidaId: Long,
        ninoId: String,
        reto: Reto,
        estado: EstadoPartida,
        tiempoMs: Long,
    ): ResultadoReto = baseDatos.withTransaction {
        guardarAvance(partidaId, estado, tiempoMs)
        dao.terminarPartida(partidaId)

        val anterior = progresoDao.leer(ninoId)?.aModelo() ?: ProgresoNino(ninoId)
        val sumoAlAcuario = !anterior.completo(reto)
        val progreso = if (sumoAlAcuario) {
            anterior.copy(
                retosCompletados = anterior.retosCompletados + 1,
                ultimoReto = RetoCompletado(reto.codigo, reto.fecha),
            )
        } else {
            anterior
        }
        progresoDao.guardar(progreso.aEntidad())

        val nuevas = insigniasNuevas(
            estado = estado,
            dificultad = reto.dificultad,
            aTiempo = tiempoMs <= LimiteDificilMs,
            progreso = progreso,
            diasConReto = dao.diasConRetoTerminado(ninoId),
            yaGanadas = dao.leerInsigniasGanadas(ninoId).mapNotNull { it.aInsignia() }.toSet(),
        )
        dao.otorgarInsignias(nuevas.map { InsigniaGanadaEntity(ninoId, it.name, reto.fecha.toString()) })

        ResultadoReto(
            residuosSeparados = estado.aciertos,
            errores = estado.errores,
            rachaMaxima = estado.rachaMaxima,
            sumoAlAcuario = sumoAlAcuario,
            insigniasNuevas = nuevas,
        )
    }

    fun insigniasGanadas(ninoId: String): Flow<Set<Insignia>> =
        dao.insigniasGanadas(ninoId).map { ganadas -> ganadas.mapNotNull { it.aInsignia() }.toSet() }

    /** Días distintos en los que el niño terminó un reto (insignia «5 retos diarios»). */
    fun diasConReto(ninoId: String): Flow<Int> = dao.observarDiasConRetoTerminado(ninoId)

    /** Intentos guardados en el teléfono que todavía no llegan al backend. */
    fun intentosPendientes(): Flow<Int> = dao.intentosPendientes()

    suspend fun intentosSinSincronizar(limite: Int): List<IntentoEntity> = dao.intentosSinSincronizar(limite)

    suspend fun marcarSincronizados(ids: List<Long>) = dao.marcarSincronizados(ids)

    private suspend fun guardarAvance(partidaId: Long, estado: EstadoPartida, tiempoMs: Long) {
        dao.guardarAvance(
            id = partidaId,
            indice = estado.indice,
            aciertos = estado.aciertos,
            errores = estado.errores,
            racha = estado.racha,
            rachaMaxima = estado.rachaMaxima,
            puntaje = estado.puntaje,
            tiempoMs = tiempoMs,
        )
    }
}

private fun InsigniaGanadaEntity.aInsignia(): Insignia? = Insignia.entries.firstOrNull { it.name == insignia }
