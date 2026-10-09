package com.reciclakids

import android.app.Application
import com.reciclakids.di.ContenedorApp

class ReciclaKidsApplication : Application() {
    val contenedor: ContenedorApp by lazy { ContenedorApp.produccion(this) }
}
