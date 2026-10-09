package com.example.com.reciclakids.plugins

import com.reciclakids.routes.rutasVistaPreviaCorreos
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    val vistaPreviaCorreos = environment.config
        .propertyOrNull("reciclakids.correos.vistaPrevia")
        ?.getString()
        .toBoolean()

    routing {
        get("/") {
            call.respondText("Hello, World!")
        }
        if (vistaPreviaCorreos) {
            rutasVistaPreviaCorreos()
        }
    }
}
