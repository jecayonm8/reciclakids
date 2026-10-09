package com.reciclakids.services.notificaciones.correos

import org.slf4j.LoggerFactory
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Entrega un correo ya renderizado. Según el ADR-10 la implementación real usa Amazon SES desde
 * el backend: las credenciales viven solo aquí y el dominio necesita SPF, DKIM y DMARC.
 */
interface EnviadorCorreo {
    suspend fun enviar(destinatario: String, correo: CorreoRenderizado)
}

/**
 * Implementación PROVISIONAL mientras no está la integración con Amazon SES: guarda los correos
 * en memoria y no envía nada. El log no lleva el destinatario ni el asunto, que nombra al niño
 * (Ley 1581 de 2012). Se reemplaza por el enviador de SES.
 */
class EnviadorCorreoEnMemoria : EnviadorCorreo {

    data class CorreoGuardado(val destinatario: String, val correo: CorreoRenderizado)

    private val log = LoggerFactory.getLogger(EnviadorCorreoEnMemoria::class.java)
    private val guardados = CopyOnWriteArrayList<CorreoGuardado>()

    val enviados: List<CorreoGuardado> get() = guardados.toList()

    override suspend fun enviar(destinatario: String, correo: CorreoRenderizado) {
        guardados += CorreoGuardado(destinatario, correo)
        log.info("Correo guardado en memoria sin enviar (enviador PROVISIONAL); van {}", guardados.size)
    }
}
