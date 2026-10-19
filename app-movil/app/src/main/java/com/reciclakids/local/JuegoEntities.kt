package com.reciclakids.local

import androidx.room.ColumnInfo
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
import java.util.UUID

/** Una partida de un reto. Se guarda tras cada intento para retomarla si la app se cierra. */
@Entity(
    tableName = "partida",
    indices = [Index("ninoId", "codigoReto", "fecha"), Index(value = ["idLocal"], unique = true)],
)
data class PartidaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ninoId: String,
    val codigoReto: String,
    /** Fecha ISO-8601 del reto. */
    val fecha: String,
    /**
     * Id con el que el backend reconoce la partida (UUID). No cambia entre reenvíos: así el
     * backend no la duplica (ADR-04). Columna de la versión 5.
     */
    @ColumnInfo(defaultValue = "''") val idLocal: String = UUID.randomUUID().toString(),
    /** Id del reto en el backend; `null` en las partidas de antes de la versión 5, que no se pueden enviar. */
    val retoId: String? = null,
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
    /** El backend ya recibió la partida terminada. Columna de la versión 5. */
    @ColumnInfo(defaultValue = "0") val terminadaEnviada: Boolean = false,
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

/** Tiempo que el niño lleva jugando cada día, para el límite del control parental (UI-19). */
@Entity(tableName = "tiempo_juego", primaryKeys = ["ninoId", "fecha"])
data class TiempoJuegoEntity(
    val ninoId: String,
    /** Fecha ISO-8601 del día. */
    val fecha: String,
    val ms: Long,
)

/**
 * Límite diario de juego de cada niño (UI-19). Lo da el backend al abrir la sesión del niño y con
 * su progreso, y se guarda para jugar sin conexión. Tabla de la versión 5.
 */
@Entity(tableName = "limite_juego")
data class LimiteJuegoEntity(
    @PrimaryKey val ninoId: String,
    val limiteMinutosDiarios: Int,
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

    @Query("SELECT * FROM partida WHERE id = :id")
    suspend fun leerPartida(id: Long): PartidaEntity?

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

    @Query("SELECT COUNT(DISTINCT fecha) FROM partida WHERE ninoId = :ninoId AND terminada = 1")
    fun observarDiasConRetoTerminado(ninoId: String): Flow<Int>

    /** Las partidas anteriores a la versión 5 no tienen reto en el backend: no cuentan como pendientes. */
    @Query(
        "SELECT COUNT(*) FROM intento INNER JOIN partida ON partida.id = intento.partidaId " +
            "WHERE intento.sincronizado = 0 AND partida.retoId IS NOT NULL"
    )
    fun intentosPendientes(): Flow<Int>

    @Query("SELECT * FROM intento WHERE sincronizado = 0 ORDER BY id LIMIT :limite")
    suspend fun intentosSinSincronizar(limite: Int): List<IntentoEntity>

    @Query("UPDATE intento SET sincronizado = 1 WHERE id IN (:ids)")
    suspend fun marcarSincronizados(ids: List<Long>)

    /**
     * Partidas que el backend todavía no tiene completas: con intentos sin enviar o terminadas sin
     * que el backend lo sepa. Las de [excluidos] (niños sin sesión en este teléfono) esperan.
     */
    @Query(
        "SELECT * FROM partida WHERE retoId IS NOT NULL AND ninoId NOT IN (:excluidos) AND (" +
            "(terminada = 1 AND terminadaEnviada = 0) OR " +
            "EXISTS (SELECT 1 FROM intento WHERE intento.partidaId = partida.id AND intento.sincronizado = 0)" +
            ") ORDER BY id LIMIT :limite"
    )
    suspend fun partidasPorSincronizar(excluidos: List<String>, limite: Int): List<PartidaEntity>

    @Query("SELECT * FROM intento WHERE partidaId = :partidaId ORDER BY id")
    suspend fun intentosDePartida(partidaId: Long): List<IntentoEntity>

    @Query("UPDATE partida SET terminadaEnviada = 1 WHERE id IN (:ids) AND terminada = 1")
    suspend fun marcarTerminadasEnviadas(ids: List<Long>)

    @Query("SELECT limiteMinutosDiarios FROM limite_juego WHERE ninoId = :ninoId")
    fun observarLimite(ninoId: String): Flow<Int?>

    @Upsert
    suspend fun guardarLimite(limite: LimiteJuegoEntity)

    @Query("SELECT ms FROM tiempo_juego WHERE ninoId = :ninoId AND fecha = :fecha")
    fun observarTiempo(ninoId: String, fecha: String): Flow<Long?>

    @Query("SELECT ms FROM tiempo_juego WHERE ninoId = :ninoId AND fecha = :fecha")
    suspend fun leerTiempo(ninoId: String, fecha: String): Long?

    @Upsert
    suspend fun guardarTiempo(tiempo: TiempoJuegoEntity)

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
