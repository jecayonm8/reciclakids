package com.reciclakids.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Persistencia local del Modo Niño (ADR-04): el juego funciona sin conexión y cada resultado se
 * guarda en el teléfono antes de sincronizarse con el backend.
 */
@Database(
    entities = [
        ProgresoNinoEntity::class,
        PartidaEntity::class,
        IntentoEntity::class,
        InsigniaGanadaEntity::class,
        AjustesNinoEntity::class,
        TiempoJuegoEntity::class,
        LimiteJuegoEntity::class,
    ],
    version = 5,
    // Piloto: sin esquemas exportados todavía. Al publicar la primera versión se exportan.
    exportSchema = false,
)
abstract class BaseDatosReciclaKids : RoomDatabase() {
    abstract fun progresoDao(): ProgresoNinoDao

    abstract fun juegoDao(): JuegoDao

    companion object {
        /**
         * 3 → 4: la racha entre retos del niño. Se migra en vez de borrar para no perder intentos
         * que todavía no llegan al backend.
         */
        val Migracion3a4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE progreso_nino ADD COLUMN rachaEntreRetos INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE progreso_nino ADD COLUMN mejorRachaEntreRetos INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * 4 → 5: la sincronización con el backend. Cada partida recibe su id local (un UUID nuevo
         * para las que ya existían) y el id del reto, que las partidas viejas no tienen: esas
         * quedan guardadas pero no se envían. Más la tabla del límite diario de cada niño.
         */
        val Migracion4a5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE partida ADD COLUMN idLocal TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE partida ADD COLUMN retoId TEXT")
                db.execSQL("ALTER TABLE partida ADD COLUMN terminadaEnviada INTEGER NOT NULL DEFAULT 0")
                // Un UUID versión 4 hecho con SQLite para cada partida que ya existía.
                db.execSQL(
                    "UPDATE partida SET idLocal = lower(hex(randomblob(4))) || '-' || lower(hex(randomblob(2))) || '-4' || " +
                        "substr(lower(hex(randomblob(2))), 2) || '-' || substr('89ab', 1 + (abs(random()) % 4), 1) || " +
                        "substr(lower(hex(randomblob(2))), 2) || '-' || lower(hex(randomblob(6))) WHERE idLocal = ''"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_partida_idLocal ON partida (idLocal)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS limite_juego " +
                        "(ninoId TEXT NOT NULL, limiteMinutosDiarios INTEGER NOT NULL, PRIMARY KEY(ninoId))"
                )
            }
        }

        val Migraciones = arrayOf(Migracion3a4, Migracion4a5)

        fun crear(context: Context): BaseDatosReciclaKids =
            Room.databaseBuilder(context.applicationContext, BaseDatosReciclaKids::class.java, "reciclakids.db")
                .addMigrations(*Migraciones)
                // Las versiones 1 y 2 fueron de desarrollo, antes de guardar resultados. Desde la 3 cada
                // cambio de esquema lleva su migración: los intentos sin enviar nunca se borran (ADR-04).
                .fallbackToDestructiveMigrationFrom(true, 1, 2)
                .build()
    }
}
