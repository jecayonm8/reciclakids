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
    ],
    version = 4,
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

        fun crear(context: Context): BaseDatosReciclaKids =
            Room.databaseBuilder(context.applicationContext, BaseDatosReciclaKids::class.java, "reciclakids.db")
                .addMigrations(Migracion3a4)
                // Solo mientras no haya una versión publicada: un salto de esquema sin migración borra
                // los datos locales. Antes del piloto hay que reemplazarlo por migraciones.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
