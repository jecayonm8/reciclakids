package com.reciclakids.services.notificaciones.correos

import java.net.URI
import java.net.URISyntaxException

/**
 * Escapa texto para insertarlo en HTML, tanto en el contenido como dentro de un atributo entre
 * comillas. Todo dato que llega de afuera (nombre del niño, jardín, insignia…) pasa por aquí.
 */
fun escaparHtml(texto: String): String = buildString(texto.length + 16) {
    for (caracter in texto) {
        when (caracter) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '"' -> append("&quot;")
            '\'' -> append("&#39;")
            else -> append(caracter)
        }
    }
}

private val EsquemasPermitidos = setOf("http", "https")

/**
 * Devuelve [enlace] sin espacios a los lados si es una URL absoluta http o https con servidor.
 * Un enlace roto en un correo (sobre todo el de baja) es un error del backend, así que se rechaza
 * en vez de reemplazarlo en silencio. El mensaje no repite la URL porque puede llevar un token.
 */
fun validarEnlace(enlace: String): String {
    val limpio = enlace.trim()
    val uri = try {
        URI(limpio)
    } catch (_: URISyntaxException) {
        null
    }
    require(uri != null && uri.scheme?.lowercase() in EsquemasPermitidos && !uri.host.isNullOrBlank()) {
        "Los enlaces del correo deben ser URL completas con http o https"
    }
    return limpio
}

/** Enlace listo para ir dentro de `href="…"`: validado y escapado para atributo. */
internal fun atributoEnlace(enlace: String): String = escaparHtml(validarEnlace(enlace))

private val SaltosYEspacios = Regex("[\\s\\p{Cc}\\p{Zl}\\p{Zp}]+")

/**
 * Junta saltos de línea, caracteres de control y espacios repetidos en un solo espacio. Los datos
 * del correo son de una línea: así nadie cuela renglones en el asunto ni enlaces falsos en el
 * texto plano. El espacio fijo (U+00A0) se respeta.
 */
internal fun enUnaLinea(texto: String): String = sinSaltos(texto).trim()

/** Igual que [enUnaLinea], pero sin recortar los extremos: para fragmentos que se pegan entre sí. */
internal fun sinSaltos(texto: String): String = texto.replace(SaltosYEspacios, " ")
