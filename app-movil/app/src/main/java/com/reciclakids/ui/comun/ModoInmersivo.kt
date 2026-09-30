package com.reciclakids.ui.comun

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * El Modo Niño corre sin barras del sistema visibles; los modos adultos las muestran.
 * Se llama una sola vez, por encima de la navegación, para que no parpadeen entre pantallas.
 */
@Composable
fun ModoInmersivo(activo: Boolean) {
    val view = LocalView.current
    LaunchedEffect(view, activo) {
        val ventana = view.context.actividad()?.window
        if (ventana != null) {
            val controlador = WindowCompat.getInsetsController(ventana, view)
            if (activo) {
                controlador.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controlador.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                controlador.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
}

private tailrec fun Context.actividad(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.actividad()
    else -> null
}
