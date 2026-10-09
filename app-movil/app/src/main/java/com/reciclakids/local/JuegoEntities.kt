package com.reciclakids.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Una partida de un reto. Se guarda tras cada intento para retomarla si la app se cierra. */
@Entity(tableName = "partida", indices = [Index("ninoId", "codigoReto", "fecha")])
data class PartidaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ninoId: String,
    val codigoReto: String,
    /** Fecha ISO-8601 del reto. */
    val fecha: String,
    val indice: Int = 0,
    val aciertos: Int = 0,
    val errores: Int = 0,
    val racha: Int = 0,
    val rachaMaxima: Int = 0,
    val puntaje: Int = 0,
    /** Tiempo jugado sin contar las pausas. */
    val tiempoMs: Long = 0,
    val terminada: Boolean = false,
    /** Reiniciada desde la pausa: ya no se retoma. */
    val abandonada: Boolean = false,
)

/** Cada intento de clasificación, pendiente de enviarse al backend. */
@Entity(
    tableName = "intento",
    foreignKeys = [ForeignKey(PartidaEntity::class, ["id"], ["partidaId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("partidaId"), Index("sincronizado")],
)
data class IntentoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val partidaId: Long,
    val residuoId: String,
    /** Nombre de la CategoriaResiduo elegida. */
    val categoriaElegida: String,
    val correcto: Boolean,
    val momentoMs: Long,
    val sincronizado: Boolean = false,
)

@Entity(tableName = "insignia_ganada", primaryKeys = ["ninoId", "insignia"])
data class InsigniaGanadaEntity(
    val ninoId: String,
    /** Nombre de la Insignia. */
    val insignia: String,
    val fecha: String,
)

/** Volumen de la voz y de la música, de 0 a 5. Uno solo por teléfono. */
@Entity(tableName = "ajustes_nino")
data class AjustesNinoEntity(
    @PrimaryKey val id: Int = 0,
    val volumenSonidos: Int,
    val volumenMusica: Int,
)

@Dao
interface JuegoDao {
    @Query(
        "SELECT * FROM partida WHERE ninoId = :ninoId AND codigoReto = :codigo AND fecha = :fecha " +
            "AND terminada = 0 AND abandonada = 0 ORDER BY id DESC LIMIT 1"
    )
    suspend fun partidaEnCurso(ninoId: String, codigo: String, fecha: String): PartidaEntity?

    @Insert
    suspend fun crearPartida(partida: PartidaEntity): Long

    @Query(
        "UPDATE partida SET indice = :indice, aciertos = :aciertos, errores = :errores, racha = :racha, " +
            "rachaMaxima = :rachaMaxima, puntaje = :puntaje, tiempoMs = :tiempoMs WHERE id = :id"
    )
    suspend fun guardarAvance(
        id: Long,
        indice: Int,
        aciertos: Int,
        errores: Int,
        racha: Int,
        rachaMaxima: Int,
        puntaje: Int,
        tiempoMs: Long,
    )

    @Query("UPDATE partida SET terminada = 1 WHERE id = :id")
    suspend fun terminarPartida(id: Long)

    @Query("UPDATE partida SET abandonada = 1 WHERE id = :id")
    suspend fun abandonarPartida(id: Long)

    @Insert
    suspend fun registrarIntento(intento: IntentoEntity)

    @Query("SELECT COUNT(DISTINCT fecha) FROM partida WHERE ninoId = :ninoId AND terminada = 1")
    suspend fun diasConRetoTerminado(ninoId: String): Int

    @Query("SELECT COUNT(*) FROM intento WHERE sincronizado = 0")
    fun intentosPendientes(): Flow<Int>

    @Query("SELECT * FROM insignia_ganada WHERE ninoId = :ninoId")
    fun insigniasGanadas(ninoId: String): Flow<List<InsigniaGanadaEntity>>

    @Query("SELECT * FROM insignia_ganada WHERE ninoId = :ninoId")
    suspend fun leerInsigniasGanadas(ninoId: String): List<InsigniaGanadaEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun otorgarInsignias(insignias: List<InsigniaGanadaEntity>)

    @Query("SELECT * FROM ajustes_nino WHERE id = 0")
    fun ajustes(): Flow<AjustesNinoEntity?>

    @Upsert
    suspend fun guardarAjustes(ajustes: AjustesNinoEntity)
}
