package com.reciclakids.ui.common

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode

/**
 * La red por defecto del teléfono según los avisos del sistema. Al perderla no se vuelve a
 * preguntar cuál es la activa: en ese instante el sistema todavía puede devolver la que se está
 * perdiendo, y por eso en modo avión la app seguía diciendo «Conectado» (hallazgo 3 de la
 * auditoría). Una red sin internet validado (un Wi-Fi sin salida) tampoco cuenta.
 */
internal class EstadoRed {
    private var red: Any? = null
    private var conInternet = false

    val hayConexion: Boolean get() = red != null && conInternet

    /** La red por defecto es [red] y ahora [internet] dice si llega a internet. */
    fun capacidades(red: Any, internet: Boolean) {
        this.red = red
        conInternet = internet
    }

    /** Se perdió [red]. Si era la de por defecto, no hay conexión hasta que el sistema avise otra. */
    fun perdida(red: Any) {
        if (red != this.red) return
        this.red = null
        conInternet = false
    }
}

/** `true` mientras el teléfono tiene una red con internet. En vistas previas siempre es `true`. */
@Composable
fun rememberHayConexion(): State<Boolean> {
    val context = LocalContext.current
    val enVistaPrevia = LocalInspectionMode.current
    return produceState(initialValue = true, context) {
        if (enVistaPrevia) return@produceState
        val conectividad = context.getSystemService(ConnectivityManager::class.java) ?: return@produceState
        val estado = EstadoRed()
        conectividad.activeNetwork?.let { red ->
            estado.capacidades(red, conectividad.getNetworkCapabilities(red)?.tieneInternet() == true)
        }
        value = estado.hayConexion
        val escucha = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                synchronized(estado) { estado.capacidades(network, networkCapabilities.tieneInternet()) }
                value = synchronized(estado) { estado.hayConexion }
            }

            override fun onLost(network: Network) {
                synchronized(estado) { estado.perdida(network) }
                value = synchronized(estado) { estado.hayConexion }
            }
        }
        conectividad.registerDefaultNetworkCallback(escucha)
        awaitDispose { conectividad.unregisterNetworkCallback(escucha) }
    }
}

private fun NetworkCapabilities.tieneInternet(): Boolean =
    hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
