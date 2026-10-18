package com.reciclakids.routes

import com.reciclakids.models.dto.LotePartidasSolicitud
import com.reciclakids.models.dto.SesionNinoSolicitud
import com.reciclakids.plugins.LimiteCodigoReto
import com.reciclakids.plugins.Proveedor
import com.reciclakids.plugins.sesionNino
import com.reciclakids.services.NinoServicio
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

/**
 * API de Retos y de Resultados para el Modo Niño. El código del reto y la elección del nombre
 * no piden contraseña («tipo Kahoot», ADR-09); lo demás va con el token del niño.
 */
fun Route.rutasNino(servicio: NinoServicio) {
    route("/nino") {
        rateLimit(LimiteCodigoReto) {
            // 200 con el reto y el grupo; 404 código que no existe; 410 código de otro día.
            get("/retos/{codigo}") {
                call.respond(servicio.buscarCodigo(call.parameters["codigo"].orEmpty()))
            }
            post("/sesiones") {
                val datos = call.receive<SesionNinoSolicitud>()
                call.respond(servicio.abrirSesion(datos.codigo, datos.ninoId))
            }
        }
        authenticate(Proveedor.Nino) {
            get("/progreso") {
                call.respond(servicio.progreso(call.sesionNino()))
            }
            post("/partidas") {
                call.respond(servicio.recibirPartidas(call.sesionNino(), call.receive<LotePartidasSolicitud>().partidas))
            }
        }
    }
}
