package com.reciclakids.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.reciclakids.config.ConfiguracionJwt
import com.reciclakids.models.Rol
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.Date

// Autenticación centralizada en el backend (ADR-09). Los adultos usan un token de acceso corto y
// uno de refresco que rota; el niño entra «tipo Kahoot» (código del reto + su nombre) y recibe un
// token propio que solo sirve para su reto y sus resultados.

/** Proveedores de autenticación de las rutas. */
object Proveedor {
    const val Docente = "docente"
    const val Acudiente = "acudiente"
    const val Nino = "nino"
}

data class SesionAdulto(val usuarioId: String, val rol: Rol)
data class SesionNino(val ninoId: String, val grupoId: String)

/** Firma y verifica los tokens JWT. */
class Tokens(private val config: ConfiguracionJwt, private val ahora: () -> Instant = Instant::now) {
    private val algoritmo = Algorithm.HMAC256(config.secreto)
    private val azar = SecureRandom()

    /** Verifica con el mismo reloj que firma: así el vencimiento es coherente (y comprobable en pruebas). */
    val verificador: com.auth0.jwt.interfaces.JWTVerifier =
        (JWT.require(algoritmo).withIssuer(config.emisor).withAudience(config.audiencia) as com.auth0.jwt.JWTVerifier.BaseVerification)
            .build(object : java.time.Clock() {
                override fun getZone(): java.time.ZoneId = java.time.ZoneOffset.UTC
                override fun withZone(zone: java.time.ZoneId): java.time.Clock = this
                override fun instant(): Instant = ahora()
            })

    fun accesoAdulto(usuarioId: String, rol: Rol): TokenFirmado = firmar(DuracionAcceso) {
        withSubject(usuarioId).withClaim(ClaimTipo, TipoAdulto).withClaim(ClaimRol, rol.name)
    }

    fun accesoNino(ninoId: String, grupoId: String): TokenFirmado = firmar(DuracionNino) {
        withSubject(ninoId).withClaim(ClaimTipo, TipoNino).withClaim(ClaimGrupo, grupoId)
    }

    /** Firma del enlace «Dejar de recibir» de los correos: sirve sin iniciar sesión, solo para ese niño. */
    fun baja(acudienteId: String, ninoId: String): String = firmar(DuracionBaja) {
        withSubject(acudienteId).withClaim(ClaimTipo, TipoBaja).withClaim(ClaimNino, ninoId)
    }.token

    /** (acudienteId, ninoId) de un enlace de baja válido, o `null`. */
    fun leerBaja(token: String): Pair<String, String>? = runCatching {
        val payload = verificador.verify(token)
        if (payload.getClaim(ClaimTipo).asString() != TipoBaja) return null
        payload.subject to payload.getClaim(ClaimNino).asString()
    }.getOrNull()

    /** Token opaco de refresco: 256 bits al azar. En la base solo se guarda [hash]. */
    fun refresco(): String {
        val bytes = ByteArray(32).also(azar::nextBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun firmar(duracion: Duration, claims: com.auth0.jwt.JWTCreator.Builder.() -> com.auth0.jwt.JWTCreator.Builder): TokenFirmado {
        val vence = ahora().plus(duracion)
        val token = JWT.create()
            .withIssuer(config.emisor)
            .withAudience(config.audiencia)
            .withIssuedAt(Date.from(ahora()))
            .withExpiresAt(Date.from(vence))
            .claims()
            .sign(algoritmo)
        return TokenFirmado(token, duracion.seconds)
    }

    companion object {
        val DuracionAcceso: Duration = Duration.ofHours(1)
        val DuracionRefresco: Duration = Duration.ofDays(30)
        /** El niño puede jugar sin conexión y sincronizar días después: su token dura una semana. */
        val DuracionNino: Duration = Duration.ofDays(7)

        const val ClaimTipo = "tipo"
        const val ClaimRol = "rol"
        val DuracionBaja: Duration = Duration.ofDays(90)

        const val ClaimGrupo = "grupo"
        const val ClaimNino = "nino"
        const val TipoAdulto = "adulto"
        const val TipoNino = "nino"
        const val TipoBaja = "baja"

        /** SHA-256 en hexadecimal: para guardar tokens de refresco y de recuperación sin poder usarlos. */
        fun hash(valor: String): String =
            MessageDigest.getInstance("SHA-256").digest(valor.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}

data class TokenFirmado(val token: String, val segundos: Long)

fun Application.configurarSeguridad(tokens: Tokens) {
    authentication {
        for (rol in Rol.entries) {
            jwt(if (rol == Rol.DOCENTE) Proveedor.Docente else Proveedor.Acudiente) {
                verifier(tokens.verificador)
                validate { credencial ->
                    val payload = credencial.payload
                    val esAdulto = payload.getClaim(Tokens.ClaimTipo).asString() == Tokens.TipoAdulto
                    if (esAdulto && payload.getClaim(Tokens.ClaimRol).asString() == rol.name && payload.subject != null) {
                        JWTPrincipal(payload)
                    } else {
                        null
                    }
                }
            }
        }
        jwt(Proveedor.Nino) {
            verifier(tokens.verificador)
            validate { credencial ->
                val payload = credencial.payload
                val esNino = payload.getClaim(Tokens.ClaimTipo).asString() == Tokens.TipoNino
                if (esNino && payload.subject != null && payload.getClaim(Tokens.ClaimGrupo).asString() != null) {
                    JWTPrincipal(payload)
                } else {
                    null
                }
            }
        }
    }
}

/** La sesión del adulto de la petición. Solo se llama dentro de `authenticate`. */
fun ApplicationCall.sesionAdulto(): SesionAdulto {
    val payload = principal<JWTPrincipal>()?.payload ?: throw NoAutenticado()
    return SesionAdulto(payload.subject, Rol.valueOf(payload.getClaim(Tokens.ClaimRol).asString()))
}

fun ApplicationCall.sesionNino(): SesionNino {
    val payload = principal<JWTPrincipal>()?.payload ?: throw NoAutenticado()
    return SesionNino(payload.subject, payload.getClaim(Tokens.ClaimGrupo).asString())
}
