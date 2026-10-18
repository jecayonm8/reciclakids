package com.reciclakids.plugins

import com.reciclakids.Contenedor
import com.reciclakids.routes.rutasAcudiente
import com.reciclakids.routes.rutasAuth
import com.reciclakids.routes.rutasDocente
import com.reciclakids.routes.rutasNino
import com.reciclakids.routes.rutasPaginas
import com.reciclakids.routes.rutasTareas
import com.reciclakids.routes.rutasVistaPreviaCorreos
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.configurarRutas(contenedor: Contenedor) {
    val config = contenedor.configuracion
    routing {
        // Cloud Run y los balanceadores consultan esta ruta para saber si el servicio está vivo.
        get("/salud") { call.respond(HttpStatusCode.OK, mapOf("estado" to "ok")) }

        route("/api/v1") {
            rutasAuth(contenedor.cuentas)
            authenticate(Proveedor.Docente, Proveedor.Acudiente) {
                // La app la llama al abrir para saber qué modo mostrar (ADR-08).
                get("/cuenta") { call.respond(contenedor.cuentas.cuenta(call.sesionAdulto().usuarioId)) }
            }
            rutasNino(contenedor.nino)
            rutasDocente(contenedor.docente, contenedor.notificaciones, contenedor.reloj)
            rutasAcudiente(contenedor.padres)
        }

        rutasPaginas(contenedor.cuentas, contenedor.notificaciones)
        if (config.claveTareas.isNotBlank()) rutasTareas(config.claveTareas, contenedor.notificaciones, contenedor.reloj)
        if (config.correos.vistaPrevia) rutasVistaPreviaCorreos()
    }
}
