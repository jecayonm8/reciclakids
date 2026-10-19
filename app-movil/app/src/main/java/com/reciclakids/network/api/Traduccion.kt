package com.reciclakids.network.api

import com.reciclakids.network.Respuesta
import java.time.Clock
import java.time.LocalDate

/**
 * Los datos traducidos al modelo de la app. Cualquier otra respuesta —o una que no se pueda
 * traducir— es [Respuesta.SinConexion]: la pantalla muestra su estado de error y deja reintentar.
 */
fun <T, R> RespuestaApi<T>.comoRespuesta(traducir: (T) -> R): Respuesta<R> = when (this) {
    is RespuestaApi.Exito -> runCatching { traducir(dato) }.fold({ Respuesta.Ok(it) }, { Respuesta.SinConexion })
    is RespuestaApi.Rechazo, RespuestaApi.SinConexion, RespuestaApi.SesionVencida -> Respuesta.SinConexion
}

/** Como [comoRespuesta], pero un 404 es «no existe» (`null`) y no un error. */
fun <T, R> RespuestaApi<T>.comoRespuestaOpcional(traducir: (T) -> R): Respuesta<R?> =
    if (this is RespuestaApi.Rechazo && estado == 404) Respuesta.Ok(null) else comoRespuesta(traducir)

val RespuestaApi<*>.exitosa: Boolean get() = this is RespuestaApi.Exito

/**
 * El «hoy» del backend, que es el que vence los códigos (RNF-05). Vale mientras el teléfono siga
 * en el mismo día en que lo supo; antes de saberlo, o al cambiar de día, es el del teléfono.
 */
class HoyServidor(private val reloj: Clock) {
    private class Visto(val servidor: LocalDate, val telefono: LocalDate)

    @Volatile
    private var visto: Visto? = null

    fun hoy(): LocalDate {
        val telefono = LocalDate.now(reloj)
        return visto?.takeIf { it.telefono == telefono }?.servidor ?: telefono
    }

    fun recordar(hoy: String) {
        runCatching { LocalDate.parse(hoy) }.onSuccess { visto = Visto(it, LocalDate.now(reloj)) }
    }
}
