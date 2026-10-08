package com.reciclakids.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Persistencia local del Modo Niño (ADR-04): el juego funciona sin conexión y cada resultado se
 * guarda en el teléfono antes de sincronizarse con el backend.
 */
@Database(
    entities = [ProgresoNinoEntity::class],
    version = 1,
    // Piloto: sin historial de migraciones todavía. Al publicar la primera versión se exporta el esquema.
    exportSchema = false,
)
abstract class BaseDatosReciclaKids : RoomDatabase() {
    abstract fun progresoDao(): ProgresoNinoDao

    companion object {
        fun crear(context: Context): BaseDatosReciclaKids =
            Room.databaseBuilder(context.applicationContext, BaseDatosReciclaKids::class.java, "reciclakids.db").build()
    }
}
