package com.reciclakids.services.notificaciones.correos

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sesv2.SesV2Client
import software.amazon.awssdk.services.sesv2.model.Body
import software.amazon.awssdk.services.sesv2.model.Content
import software.amazon.awssdk.services.sesv2.model.Destination
import software.amazon.awssdk.services.sesv2.model.EmailContent
import software.amazon.awssdk.services.sesv2.model.Message
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest

/**
 * Envío real por Amazon SES (ADR-10). Las credenciales salen de la cadena estándar de AWS
 * (AWS_ACCESS_KEY_ID y AWS_SECRET_ACCESS_KEY, inyectadas desde Secret Manager en Cloud Run).
 * El dominio del remitente necesita SPF, DKIM y DMARC verificados en SES.
 */
class EnviadorCorreoSes(private val remitente: String, region: String) : EnviadorCorreo, AutoCloseable {

    private val log = LoggerFactory.getLogger(EnviadorCorreoSes::class.java)
    private val cliente = SesV2Client.builder()
        .region(Region.of(region))
        .httpClient(UrlConnectionHttpClient.create())
        .build()

    override suspend fun enviar(destinatario: String, correo: CorreoRenderizado) = withContext(Dispatchers.IO) {
        fun contenido(texto: String) = Content.builder().data(texto).charset("UTF-8").build()
        val peticion = SendEmailRequest.builder()
            .fromEmailAddress(remitente)
            .destination(Destination.builder().toAddresses(destinatario).build())
            .content(
                EmailContent.builder().simple(
                    Message.builder()
                        .subject(contenido(correo.asunto))
                        .body(Body.builder().html(contenido(correo.html)).text(contenido(correo.textoPlano)).build())
                        .build(),
                ).build(),
            )
            .build()
        val respuesta = cliente.sendEmail(peticion)
        // Sin destinatario ni asunto en el log: nombran al niño (Ley 1581 de 2012).
        log.info("Correo enviado por SES (id {})", respuesta.messageId())
    }

    override fun close() = cliente.close()
}
