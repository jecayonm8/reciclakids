package com.reciclakids.routes

import com.reciclakids.services.notificaciones.correos.CorreoRenderizado
import com.reciclakids.services.notificaciones.correos.EjemplosCorreo
import com.reciclakids.services.notificaciones.correos.PlantillaCorreoLogro
import com.reciclakids.services.notificaciones.correos.PlantillaCorreoReporteSemanal
import io.ktor.http.ContentType
import io.ktor.http.withCharset
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

/**
 * Vista previa de las plantillas de correo (UI-36 y UI-37) con datos ficticios, para revisarlas
 * en el navegador durante el desarrollo. Con `?formato=texto` muestra el asunto y la
 * alternativa en texto plano. Se activa con `reciclakids.correos.vistaPrevia` en la configuración.
 */
fun Route.rutasVistaPreviaCorreos() {
    route("/correos/vista-previa") {
        get("/logro") {
            call.responderVistaPrevia(PlantillaCorreoLogro.renderizar(EjemplosCorreo.logro))
        }
        get("/reporte-semanal") {
            call.responderVistaPrevia(PlantillaCorreoReporteSemanal.renderizar(EjemplosCorreo.reporteSemanal))
        }
    }
}

private suspend fun ApplicationCall.responderVistaPrevia(correo: CorreoRenderizado) {
    if (request.queryParameters["formato"] == "texto") {
        respondText(
            "Asunto: ${correo.asunto}\n\n${correo.textoPlano}\n",
            ContentType.Text.Plain.withCharset(Charsets.UTF_8),
        )
    } else {
        respondText(correo.html, ContentType.Text.Html.withCharset(Charsets.UTF_8))
    }
}
