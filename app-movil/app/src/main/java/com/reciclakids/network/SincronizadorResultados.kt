package com.reciclakids.network

import android.content.Context
import androidx.room.withTransaction
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
import com.reciclakids.local.InsigniaGanadaEntity
import com.reciclakids.local.IntentoEntity
import com.reciclakids.local.PartidaEntity
import com.reciclakids.model.CategoriaResiduo
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/** Partidas que se leen por vuelta; el backend acepta hasta 100 por lote. */
private const val TamanoLote = 20

/**
 * Lleva al backend las partidas guardadas en el teléfono (ADR-04): completas, con todos sus
 * intentos y por niño, cada uno con su token (un teléfono puede ser de varios niños). Nunca borra
 * nada: solo marca como sincronizado lo que el backend confirmó.
 */
class SincronizadorResultados(
    private val baseDatos: BaseDatosReciclaKids,
    private val servicio: ServicioResultados,
    private val hoy: () -> LocalDate = LocalDate::now,
) {
    /**
     * `false` si falló la red o el servidor y hay que reintentar. `true` si ya no queda nada que se
     * pueda enviar ahora: lo de los niños sin sesión espera a que vuelvan a entrar con un código.
     */
    suspend fun sincronizar(): Boolean {
        val dao = baseDatos.juegoDao()
        val enEspera = mutableSetOf<String>()
        while (true) {
            val pendientes = dao.partidasPorSincronizar(enEspera.toList(), TamanoLote)
            if (pendientes.isEmpty()) return true
            val ninoId = pendientes.first().ninoId
            val partidas = pendientes.filter { it.ninoId == ninoId }.map { it to dao.intentosDePartida(it.id) }
            val codigoHoy = partidas.firstOrNull { (partida, _) -> partida.fecha == hoy().toString() }?.first?.codigoReto
            when (val resultado = servicio.enviar(ninoId, partidas.map { (partida, intentos) -> partida.aEnviada(intentos) }, codigoHoy)) {
                is ResultadoEnvio.Recibido -> baseDatos.withTransaction {
                    // Solo lo que iba en el lote: lo que se jugó mientras tanto queda para la próxima vuelta.
                    dao.marcarSincronizados(partidas.flatMap { (_, intentos) -> intentos.map { it.id } })
                    dao.marcarTerminadasEnviadas(partidas.filter { (partida, _) -> partida.terminada }.map { it.first.id })
                    dao.otorgarInsignias(resultado.insigniasNuevas.map { InsigniaGanadaEntity(ninoId, it.name, hoy().toString()) })
                }
                ResultadoEnvio.SinSesion, ResultadoEnvio.Rechazado -> enEspera += ninoId
                ResultadoEnvio.SinConexion -> return false
            }
        }
    }
}

private fun PartidaEntity.aEnviada(intentos: List<IntentoEntity>) = PartidaEnviada(
    idLocal = idLocal,
    retoId = checkNotNull(retoId) { "La partida $id no tiene reto" },
    terminada = terminada,
    aciertos = aciertos,
    errores = errores,
    rachaMaxima = rachaMaxima,
    tiempoMs = tiempoMs,
    intentos = intentos.mapNotNull { intento ->
        val elegida = CategoriaResiduo.entries.firstOrNull { it.name == intento.categoriaElegida } ?: return@mapNotNull null
        IntentoEnviado(intento.residuoId, elegida, intento.correcto, intento.momentoMs)
    },
)

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
