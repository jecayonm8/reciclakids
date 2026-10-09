package com.reciclakids.services.notificaciones.correos

/**
 * Correo listo para entregarlo al servicio de envío: el asunto, el HTML con estilos en línea y
 * la alternativa en texto plano (la parte `text/plain` del mensaje).
 */
data class CorreoRenderizado(
    val asunto: String,
    val html: String,
    val textoPlano: String,
)

/**
 * Enlaces del pie de todo correo. Con ellos el acudiente decide qué recibe y consulta cómo
 * tratamos los datos de su hijo o hija (Ley 1581 de 2012). Solo se aceptan URL http o https.
 */
data class EnlacesPieCorreo(
    val preferencias: String,
    val privacidad: String,
    val baja: String,
)
