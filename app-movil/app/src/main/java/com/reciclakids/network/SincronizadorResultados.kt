package com.reciclakids.network

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.reciclakids.ReciclaKidsApplication
import com.reciclakids.local.BaseDatosReciclaKids
import java.util.concurrent.TimeUnit

/** Intentos que se envían por lote. */
private const val TamanoLote = 50

/**
 * Lleva al backend los intentos guardados en el teléfono (ADR-04). Nunca borra nada: solo marca
 * como sincronizado lo que el backend confirmó.
 */
class SincronizadorResultados(
    private val baseDatos: BaseDatosReciclaKids,
    private val servicio: ServicioResultados,
) {
    /** `true` si ya no queda nada pendiente; `false` si el envío falló y hay que reintentar. */
    suspend fun sincronizar(): Boolean {
        val dao = baseDatos.juegoDao()
        while (true) {
            val lote = dao.intentosSinSincronizar(TamanoLote)
            if (lote.isEmpty()) return true
            val partidas = lote.map { it.partidaId }.toSet().associateWith { id -> dao.leerPartida(id) }
            val enviados = lote.mapNotNull { intento ->
                val partida = partidas[intento.partidaId] ?: return@mapNotNull null
                IntentoEnviado(
                    ninoId = partida.ninoId,
                    codigoReto = partida.codigoReto,
                    residuoId = intento.residuoId,
                    categoriaElegida = intento.categoriaElegida,
                    correcto = intento.correcto,
                    momentoMs = intento.momentoMs,
                )
            }
            if (!servicio.enviar(enviados)) return false
            dao.marcarSincronizados(lote.map { it.id })
        }
    }
}

/** Corre el sincronizador en segundo plano cuando hay red, con reintentos. */
class SincronizacionWorker(contexto: Context, parametros: WorkerParameters) : CoroutineWorker(contexto, parametros) {
    override suspend fun doWork(): Result {
        val contenedor = (applicationContext as ReciclaKidsApplication).contenedor
        return if (contenedor.sincronizador.sincronizar()) Result.success() else Result.retry()
    }

    companion object {
        private const val Nombre = "sincronizar-resultados"

        /** Pide una sincronización en cuanto haya conexión. Llamarlo de más no duplica trabajo. */
        fun programar(context: Context) {
            val trabajo = OneTimeWorkRequestBuilder<SincronizacionWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(Nombre, ExistingWorkPolicy.APPEND_OR_REPLACE, trabajo)
        }
    }
}
