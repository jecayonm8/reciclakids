package com.reciclakids.local

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import com.reciclakids.model.ProgresoNino
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
        .addMigrations(*BaseDatosReciclaKids.Migraciones)
        .allowMainThreadQueries()
        .build()

    /** Crea la base actual con un progreso, dos partidas y sus intentos sin sincronizar. */
    private fun llenar() = runBlocking {
        abrir().run {
            progresoDao().guardar(ProgresoNino("nino-1", retosCompletados = 3).aEntidad())
            for (codigo in listOf("4729", "1111")) {
                val partida = juegoDao().crearPartida(PartidaEntity(ninoId = "nino-1", codigoReto = codigo, fecha = "2026-10-08"))
                juegoDao().registrarIntento(
                    IntentoEntity(partidaId = partida, residuoId = "botella", categoriaElegida = "Aprovechable", correcto = true, momentoMs = 0)
                )
            }
            close()
        }
    }

    private fun conSqlite(bloque: (SQLiteDatabase) -> Unit) =
        SQLiteDatabase.openDatabase(contexto.getDatabasePath(nombre).path, null, SQLiteDatabase.OPEN_READWRITE).use(bloque)

    /** La tabla de partidas y el resto del esquema vuelven a como eran en la versión 4. */
    private fun bajarAVersion4(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE partida_v4 (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, ninoId TEXT NOT NULL, " +
                "codigoReto TEXT NOT NULL, fecha TEXT NOT NULL, indice INTEGER NOT NULL, aciertos INTEGER NOT NULL, " +
                "errores INTEGER NOT NULL, racha INTEGER NOT NULL, rachaMaxima INTEGER NOT NULL, puntaje INTEGER NOT NULL, " +
                "tiempoMs INTEGER NOT NULL, terminada INTEGER NOT NULL, abandonada INTEGER NOT NULL)"
        )
        db.execSQL(
            "INSERT INTO partida_v4 SELECT id, ninoId, codigoReto, fecha, indice, aciertos, errores, racha, " +
                "rachaMaxima, puntaje, tiempoMs, terminada, abandonada FROM partida"
        )
        db.execSQL("DROP TABLE partida")
        db.execSQL("ALTER TABLE partida_v4 RENAME TO partida")
        db.execSQL("CREATE INDEX index_partida_ninoId_codigoReto_fecha ON partida (ninoId, codigoReto, fecha)")
        db.execSQL("DROP TABLE limite_juego")
        db.version = 4
    }

    @Test
    fun deLaVersion3ALa5SeConservanElProgresoYLosIntentosSinSincronizar() = runBlocking {
        llenar()
        conSqlite { db ->
            bajarAVersion4(db)
            // La tabla de progreso vuelve a como era en la versión 3, sin las columnas de la racha.
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
            assertEquals(2, juegoDao().intentosSinSincronizar(limite = 10).size)
            close()
        }
    }

    @Test
    fun deLaVersion4ALa5CadaPartidaRecibeSuIdLocalYNadaSeBorra() = runBlocking {
        llenar()
        conSqlite(::bajarAVersion4)

        abrir().run {
            val dao = juegoDao()
            val partidas = listOf(checkNotNull(dao.leerPartida(1)), checkNotNull(dao.leerPartida(2)))
            // Un UUID nuevo y distinto por partida: el backend no las confunde al recibirlas.
            assertTrue(partidas.all { Regex("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$").matches(it.idLocal) })
            assertNotEquals(partidas[0].idLocal, partidas[1].idLocal)
            // Las partidas viejas no saben de qué reto del backend son: se guardan pero no se envían.
            assertTrue(partidas.all { it.retoId == null && !it.terminadaEnviada })
            assertEquals(2, dao.intentosSinSincronizar(limite = 10).size)
            assertTrue(dao.partidasPorSincronizar(emptyList(), 10).isEmpty())
            assertEquals(0, dao.intentosPendientes().first())
            assertEquals(3, progresoDao().leer("nino-1")?.retosCompletados)

            // La tabla nueva del límite diario funciona.
            assertNull(dao.observarLimite("nino-1").first())
            dao.guardarLimite(LimiteJuegoEntity("nino-1", 15))
            assertEquals(15, dao.observarLimite("nino-1").first())

            // Y las partidas nuevas ya llegan con su reto.
            val nueva = dao.crearPartida(PartidaEntity(ninoId = "nino-1", codigoReto = "4729", fecha = "2026-10-16", retoId = "reto-9"))
            assertFalse(checkNotNull(dao.leerPartida(nueva)).idLocal.isEmpty())
            close()
        }
    }
}
