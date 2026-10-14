package com.reciclakids.local

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import com.reciclakids.model.ProgresoNino
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class MigracionBaseDatosTest {

    private val contexto = RuntimeEnvironment.getApplication()
    private val nombre = "migracion-prueba.db"

    @After
    fun borrar() {
        contexto.deleteDatabase(nombre)
    }

    // Sin el borrado destructivo de la app: si la migración no deja el esquema como Room lo espera,
    // la prueba falla en vez de empezar de cero.
    private fun abrir() = Room.databaseBuilder(contexto, BaseDatosReciclaKids::class.java, nombre)
        .addMigrations(BaseDatosReciclaKids.Migracion3a4)
        .allowMainThreadQueries()
        .build()

    @Test
    fun deLaVersion3ALa4SeConservanElProgresoYLosIntentosSinSincronizar() = runBlocking {
        abrir().run {
            progresoDao().guardar(ProgresoNino("nino-1", retosCompletados = 3).aEntidad())
            val partida = juegoDao().crearPartida(PartidaEntity(ninoId = "nino-1", codigoReto = "4729", fecha = "2026-10-08"))
            juegoDao().registrarIntento(
                IntentoEntity(partidaId = partida, residuoId = "botella", categoriaElegida = "Aprovechable", correcto = true, momentoMs = 0)
            )
            close()
        }
        // La tabla de progreso vuelve a como era en la versión 3, sin las columnas de la racha.
        SQLiteDatabase.openDatabase(contexto.getDatabasePath(nombre).path, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
            db.execSQL(
                "CREATE TABLE progreso_v3 (ninoId TEXT NOT NULL, tutorialVisto INTEGER NOT NULL, " +
                    "retosCompletados INTEGER NOT NULL, ultimoRetoCodigo TEXT, ultimoRetoFecha TEXT, PRIMARY KEY(ninoId))"
            )
            db.execSQL(
                "INSERT INTO progreso_v3 SELECT ninoId, tutorialVisto, retosCompletados, ultimoRetoCodigo, ultimoRetoFecha FROM progreso_nino"
            )
            db.execSQL("DROP TABLE progreso_nino")
            db.execSQL("ALTER TABLE progreso_v3 RENAME TO progreso_nino")
            db.version = 3
        }

        abrir().run {
            val progreso = checkNotNull(progresoDao().leer("nino-1")).aModelo()
            assertEquals(3, progreso.retosCompletados)
            assertEquals(0, progreso.rachaEntreRetos)
            assertEquals(0, progreso.mejorRachaEntreRetos)
            assertEquals(1, juegoDao().intentosSinSincronizar(limite = 10).size)
            close()
        }
    }
}
