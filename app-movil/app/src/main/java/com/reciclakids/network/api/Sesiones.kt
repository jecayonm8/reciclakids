package com.reciclakids.network.api

import com.reciclakids.local.AlmacenSeguro
import com.reciclakids.model.CuentaAdulto
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

/** Lo que queda en el teléfono de la sesión del adulto: los dos tokens y la cuenta para abrir su modo. */
@Serializable
data class SesionGuardada(val tokenAcceso: String, val tokenRefresco: String, val cuenta: CuentaDto)

/**
 * La sesión del adulto en [almacen] (cifrada con el Android Keystore). Hay una sola por teléfono:
 * iniciar sesión con otra cuenta reemplaza la anterior.
 */
class SesionAdultoGuardada(private val almacen: AlmacenSeguro) {
    @Volatile
    private var enMemoria: SesionGuardada? = null

    @Volatile
    private var leida = false

    fun leer(): SesionGuardada? {
        if (!leida) {
            enMemoria = almacen.leer(Clave)?.let { runCatching { JsonApi.decodeFromString<SesionGuardada>(it) }.getOrNull() }
            leida = true
        }
        return enMemoria
    }

    fun guardar(sesion: SesionDto) {
        val guardada = SesionGuardada(sesion.tokenAcceso, sesion.tokenRefresco, sesion.cuenta)
        enMemoria = guardada
        leida = true
        almacen.guardar(Clave, JsonApi.encodeToString(guardada))
    }

    /** La cuenta cambió (por ejemplo, el correo desde el perfil): los tokens siguen igual. */
    fun actualizarCuenta(cuenta: CuentaDto) {
        val actual = leer() ?: return
        val nueva = actual.copy(cuenta = cuenta)
        enMemoria = nueva
        almacen.guardar(Clave, JsonApi.encodeToString(nueva))
    }

    fun borrar() {
        enMemoria = null
        leida = true
        almacen.borrar(Clave)
    }

    private companion object {
        const val Clave = "sesion.adulto"
    }
}

@Serializable
private data class TokenNinoGuardado(val token: String, val venceEnMs: Long)

/**
 * El token de cada niño que jugó en este teléfono (puede ser compartido por varios niños de la
 * familia o del salón). Sirve para enviar sus resultados aunque se sincronicen días después.
 */
class TokensNino(
    private val almacen: AlmacenSeguro,
    private val ahora: () -> Long = System::currentTimeMillis,
) {
    /** `null` si el niño no tiene token o ya venció. */
    fun leer(ninoId: String): String? {
        val guardado = almacen.leer(clave(ninoId))
            ?.let { runCatching { JsonApi.decodeFromString<TokenNinoGuardado>(it) }.getOrNull() }
            ?: return null
        return guardado.token.takeIf { guardado.venceEnMs - MargenMs > ahora() }
    }

    fun guardar(ninoId: String, token: String, venceEnSegundos: Long) {
        almacen.guardar(clave(ninoId), JsonApi.encodeToString(TokenNinoGuardado(token, ahora() + venceEnSegundos * 1_000)))
    }

    fun borrar(ninoId: String) = almacen.borrar(clave(ninoId))

    private fun clave(ninoId: String) = "sesion.nino.$ninoId"

    private companion object {
        /** Un token que vence en menos de esto ya no se usa: el envío podría llegar tarde. */
        const val MargenMs = 60_000L
    }
}

fun CuentaDto.aModelo() = CuentaAdulto(nombre = nombre, correo = correo, rol = rol, jardin = jardin, grupo = grupo)
