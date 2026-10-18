package com.reciclakids.routes

import com.reciclakids.plugins.ErrorApi
import com.reciclakids.services.CuentasServicio
import com.reciclakids.services.NotificacionesServicio
import com.reciclakids.services.notificaciones.correos.escaparHtml
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.withCharset
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

// Páginas web mínimas a las que llevan los enlaces de los correos. Todo lo demás pasa en la app.

fun Route.rutasPaginas(cuentas: CuentasServicio, notificaciones: NotificacionesServicio) {
    get("/restablecer") {
        val token = call.request.queryParameters["token"].orEmpty()
        call.pagina("Crea tu contraseña nueva", formularioRestablecer(token, error = null))
    }
    post("/restablecer") {
        val datos = call.receiveParameters()
        val token = datos["token"].orEmpty()
        val contrasena = datos["contrasena"].orEmpty()
        if (contrasena != datos["confirmacion"]) {
            call.pagina("Crea tu contraseña nueva", formularioRestablecer(token, "Las dos contraseñas no coinciden."), HttpStatusCode.UnprocessableEntity)
            return@post
        }
        try {
            cuentas.restablecer(token, contrasena)
            call.pagina("Listo", "<p>Tu contraseña cambió. Vuelve a la app de ReciclaKids e inicia sesión con la nueva.</p>")
        } catch (error: ErrorApi) {
            call.pagina("Crea tu contraseña nueva", formularioRestablecer(token, error.message), error.estado)
        }
    }
    get("/correos/baja") {
        val listo = notificaciones.darDeBaja(call.request.queryParameters["token"].orEmpty())
        if (listo) {
            call.pagina(
                "Ya no recibirás correos",
                "<p>Dejaste de recibir los correos de logros y el reporte semanal de este niño o niña. " +
                    "Puedes volver a activarlos en la app: Cuenta › Control parental.</p>",
            )
        } else {
            call.pagina("Enlace vencido", "<p>Este enlace venció. Cambia tus preferencias en la app: Cuenta › Control parental.</p>", HttpStatusCode.Gone)
        }
    }
    get("/padres") {
        call.pagina("Abre ReciclaKids", "<p>Para ver el progreso de tu hijo o hija, abre la app de ReciclaKids e inicia sesión.</p>")
    }
    get("/padres/preferencias") {
        call.pagina("Tus preferencias", "<p>Elige qué correos recibes en la app de ReciclaKids: Cuenta › Control parental.</p>")
    }
    get("/privacidad") {
        call.pagina(
            "Tratamiento de datos",
            "<p>ReciclaKids guarda de cada niño o niña solo su nombre con la inicial del apellido, su avatar y los " +
                "resultados del juego. No pedimos documento, dirección, fotos ni ubicación (Ley 1581 de 2012).</p>" +
                "<p>La política completa está en la app: Cuenta › Privacidad y datos del menor.</p>",
        )
    }
}

private fun formularioRestablecer(token: String, error: String?): String = buildString {
    if (error != null) append("<p class=\"error\" role=\"alert\">${escaparHtml(error)}</p>")
    append(
        """
        <form method="post" action="/restablecer">
          <input type="hidden" name="token" value="${escaparHtml(token)}">
          <label for="contrasena">Contraseña nueva (mínimo 8 caracteres)</label>
          <input id="contrasena" name="contrasena" type="password" minlength="8" maxlength="72" required autocomplete="new-password">
          <label for="confirmacion">Escríbela otra vez</label>
          <input id="confirmacion" name="confirmacion" type="password" minlength="8" maxlength="72" required autocomplete="new-password">
          <button type="submit">Guardar contraseña</button>
        </form>
        """.trimIndent(),
    )
}

private suspend fun ApplicationCall.pagina(titulo: String, contenido: String, estado: HttpStatusCode = HttpStatusCode.OK) {
    response.headers.append("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'; form-action 'self'")
    response.headers.append("X-Frame-Options", "DENY")
    respondText(
        """
        <!doctype html>
        <html lang="es">
        <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <meta name="robots" content="noindex">
        <title>${escaparHtml(titulo)} · ReciclaKids</title>
        <style>
          body { margin: 0; font-family: Roboto, system-ui, sans-serif; background: #F5FAFC; color: #171C1E; }
          main { max-width: 420px; margin: 0 auto; padding: 48px 20px; display: flex; flex-direction: column; gap: 16px; }
          h1 { margin: 0; font-size: 24px; color: #00546A; }
          p { margin: 0; line-height: 1.5; }
          form { display: flex; flex-direction: column; gap: 8px; }
          label { font-weight: 500; margin-top: 8px; }
          input { font: inherit; padding: 12px; border: 1px solid #6F797B; border-radius: 8px; }
          button { font: inherit; font-weight: 500; margin-top: 16px; padding: 14px; border: 0; border-radius: 24px; background: #00546A; color: #FFFFFF; }
          .error { color: #BA1A1A; }
        </style>
        </head>
        <body><main><h1>${escaparHtml(titulo)}</h1>$contenido</main></body>
        </html>
        """.trimIndent(),
        ContentType.Text.Html.withCharset(Charsets.UTF_8),
        estado,
    )
}
