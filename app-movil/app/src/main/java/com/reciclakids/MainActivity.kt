package com.reciclakids

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.reciclakids.ui.ReciclaKidsApp
import com.reciclakids.ui.theme.ReciclaKidsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val contenedor = (application as ReciclaKidsApplication).contenedor
        // Lo que quedó sin enviar en la sesión anterior sale en cuanto haya red.
        contenedor.programarSincronizacion()
        setContent {
            ReciclaKidsTheme {
                ReciclaKidsApp(contenedor = contenedor)
            }
        }
    }
}
