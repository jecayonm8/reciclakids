package com.reciclakids.di

import android.content.Context
import com.reciclakids.local.BaseDatosReciclaKids
import com.reciclakids.local.RepositorioProgreso
import com.reciclakids.network.ServicioAcceso
import com.reciclakids.network.ServicioAccesoEnMemoria
import com.reciclakids.network.ServicioRetos
import com.reciclakids.network.ServicioRetosEnMemoria

/**
 * Dependencias de la app con el ciclo de vida del proceso. Inyección manual: con pocas piezas,
 * un contenedor explícito se entiende mejor que un framework. Las pruebas arman las suyas.
 */
class ContenedorApp(
    val servicioAcceso: ServicioAcceso,
    val servicioRetos: ServicioRetos,
    val baseDatos: BaseDatosReciclaKids,
) {
    val progreso by lazy { RepositorioProgreso(baseDatos.progresoDao()) }

    companion object {
        fun produccion(context: Context) = ContenedorApp(
            // PROVISIONALES mientras el backend no exponga autenticación ni retos.
            servicioAcceso = ServicioAccesoEnMemoria(),
            servicioRetos = ServicioRetosEnMemoria(),
            baseDatos = BaseDatosReciclaKids.crear(context),
        )
    }
}
