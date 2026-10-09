package com.reciclakids

import androidx.room.Room
import com.reciclakids.di.ContenedorApp
import com.reciclakids.local.BaseDatosReciclaKids
import com.reciclakids.model.ControlParental
import com.reciclakids.network.ServicioAccesoEnMemoria
import com.reciclakids.network.ServicioResultadosEnMemoria
import com.reciclakids.network.ServicioRetosEnMemoria
import org.robolectric.RuntimeEnvironment

/**
 * Contenedor para pruebas con Robolectric: servicios sin latencia y una base de datos en memoria
 * que responde en el mismo hilo, para que las pantallas reciban los datos sin esperas.
 */
fun contenedorDePrueba(controlParental: ControlParental = ControlParental()): ContenedorApp = ContenedorApp(
    servicioAcceso = ServicioAccesoEnMemoria(latenciaMs = 0),
    servicioRetos = ServicioRetosEnMemoria(latenciaMs = 0),
    servicioResultados = ServicioResultadosEnMemoria(latenciaMs = 0),
    baseDatos = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), BaseDatosReciclaKids::class.java)
        .allowMainThreadQueries()
        .setQueryExecutor { it.run() }
        .setTransactionExecutor { it.run() }
        .build(),
    controlParental = controlParental,
)
