package com.reciclakids.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import com.reciclakids.model.ProgresoNino
import com.reciclakids.model.RetoCompletado
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Entity(tableName = "progreso_nino")
data class ProgresoNinoEntity(
    @PrimaryKey val ninoId: String,
    val tutorialVisto: Boolean,
    val retosCompletados: Int,
    val ultimoRetoCodigo: String?,
    /** Fecha ISO-8601 (aaaa-mm-dd) del último reto completado. */
    val ultimoRetoFecha: String?,
)

@Dao
interface ProgresoNinoDao {
    @Query("SELECT * FROM progreso_nino WHERE ninoId = :ninoId")
    fun observar(ninoId: String): Flow<ProgresoNinoEntity?>

    @Query("SELECT * FROM progreso_nino WHERE ninoId = :ninoId")
    suspend fun leer(ninoId: String): ProgresoNinoEntity?

    @Upsert
    suspend fun guardar(progreso: ProgresoNinoEntity)
}

fun ProgresoNinoEntity.aModelo() = ProgresoNino(
    ninoId = ninoId,
    tutorialVisto = tutorialVisto,
    retosCompletados = retosCompletados,
    ultimoReto = if (ultimoRetoCodigo != null && ultimoRetoFecha != null) {
        RetoCompletado(ultimoRetoCodigo, LocalDate.parse(ultimoRetoFecha))
    } else {
        null
    },
)

fun ProgresoNino.aEntidad() = ProgresoNinoEntity(
    ninoId = ninoId,
    tutorialVisto = tutorialVisto,
    retosCompletados = retosCompletados,
    ultimoRetoCodigo = ultimoReto?.codigo,
    ultimoRetoFecha = ultimoReto?.fecha?.toString(),
)
