package com.reciclakids.ui.comun

import android.net.ConnectivityManager
import android.net.Network
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode

/** `true` mientras el teléfono tiene una red activa. En vistas previas siempre es `true`. */
@Composable
fun rememberHayConexion(): State<Boolean> {
    val context = LocalContext.current
    val enVistaPrevia = LocalInspectionMode.current
    return produceState(initialValue = true, context) {
        if (enVistaPrevia) return@produceState
        val conectividad = context.getSystemService(ConnectivityManager::class.java) ?: return@produceState
        val escucha = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                value = true
            }

            override fun onLost(network: Network) {
                value = conectividad.activeNetwork != null
            }
        }
        value = conectividad.activeNetwork != null
        conectividad.registerDefaultNetworkCallback(escucha)
        awaitDispose { conectividad.unregisterNetworkCallback(escucha) }
    }
}
