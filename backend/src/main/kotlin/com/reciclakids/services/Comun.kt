package com.reciclakids.services

import com.reciclakids.plugins.DatosInvalidos
import java.security.SecureRandom
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.UUID

/** Hora del servidor en la zona del piloto: de ella sale el «hoy» que vence los códigos (RNF-05). */
class Reloj(private val zona: ZoneId, private val reloj: Clock = Clock.system(zona)) {
    fun ahora(): OffsetDateTime = OffsetDateTime.now(reloj.withZone(zona))
    fun hoy(): LocalDate = LocalDate.now(reloj.withZone(zona))
    fun aFecha(momentoMs: Long): LocalDate = java.time.Instant.ofEpochMilli(momentoMs).atZone(zona).toLocalDate()
}

fun nuevoId(): String = UUID.randomUUID().toString()

/** Generador de códigos: 4 dígitos para el reto y «JB-2M91» para vincular a un acudiente. */
class GeneradorCodigos(private val azar: SecureRandom = SecureRandom()) {
    fun codigoReto(): String = "%04d".format(azar.nextInt(10_000))

    /** Sin letras ni dígitos que se confundan al dictarlos (O/0, I/1). */
    fun codigoVinculacion(): String {
        fun letra() = LetrasCodigo[azar.nextInt(LetrasCodigo.length)]
        fun caracter() = CaracteresCodigo[azar.nextInt(CaracteresCodigo.length)]
        return "${letra()}${letra()}-${caracter()}${caracter()}${caracter()}${caracter()}"
    }

    companion object {
        private const val LetrasCodigo = "ABCDEFGHJKLMNPQRSTUVWXYZ"
        private const val CaracteresCodigo = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    }
}

object Validacion {
    private val Correo = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    const val MinimoContrasena = 8

    fun texto(valor: String, campo: String, maximo: Int): String {
        val limpio = valor.trim()
        if (limpio.isEmpty()) throw DatosInvalidos("Completa $campo")
        if (limpio.length > maximo) throw DatosInvalidos("$campo es demasiado largo")
        return limpio
    }

    fun correo(valor: String): String {
        val limpio = valor.trim().lowercase()
        if (!Correo.matches(limpio) || limpio.length > 254) throw DatosInvalidos("Escribe un correo válido", "correo_invalido")
        return limpio
    }

    fun contrasena(valor: String): String {
        if (valor.length < MinimoContrasena) {
            throw DatosInvalidos("La contraseña debe tener al menos $MinimoContrasena caracteres", "contrasena_corta")
        }
        if (valor.length > 72) throw DatosInvalidos("La contraseña es demasiado larga", "contrasena_larga")
        return valor
    }

    /** «Salomé» + «m» → «Salomé M.»: del niño nunca se guarda el apellido completo (Ley 1581 de 2012). */
    fun nombreNino(nombre: String, inicialApellido: String): String {
        val primero = texto(nombre, "el nombre", 40).replaceFirstChar { it.uppercaseChar() }
        val inicial = inicialApellido.trim()
        if (inicial.length != 1 || !inicial[0].isLetter()) {
            throw DatosInvalidos("Escribe solo la inicial del apellido", "inicial_invalida")
        }
        return "$primero ${inicial.uppercase()}."
    }
}
