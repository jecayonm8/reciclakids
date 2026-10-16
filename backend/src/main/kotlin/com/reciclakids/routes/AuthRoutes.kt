package com.reciclakids.routes

import com.reciclakids.models.dto.InicioSesionSolicitud
import com.reciclakids.models.dto.RecuperacionSolicitud
import com.reciclakids.models.dto.RefrescoSolicitud
import com.reciclakids.models.dto.RegistroAcudienteSolicitud
import com.reciclakids.models.dto.RegistroDocenteSolicitud
import com.reciclakids.models.dto.RestablecerSolicitud
import com.reciclakids.plugins.LimiteAcceso
import com.reciclakids.services.CuentasServicio
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

/** API de Autenticación (C4 N3). Respuestas: 201 creada, 409 correo en uso, 422 datos inválidos. */
fun Route.rutasAuth(cuentas: CuentasServicio) {
    route("/auth") {
        rateLimit(LimiteAcceso) {
            post("/registro/docente") {
                cuentas.registrarDocente(call.receive<RegistroDocenteSolicitud>())
                call.respond(HttpStatusCode.Created)
            }
            post("/registro/acudiente") {
                cuentas.registrarAcudiente(call.receive<RegistroAcudienteSolicitud>())
                call.respond(HttpStatusCode.Created)
            }
            post("/sesion") {
                val datos = call.receive<InicioSesionSolicitud>()
                call.respond(cuentas.iniciarSesion(datos.correo, datos.contrasena))
            }
            post("/recuperacion") {
                cuentas.solicitarRecuperacion(call.receive<RecuperacionSolicitud>().correo)
                // Siempre 202: la respuesta no revela si el correo tiene cuenta.
                call.respond(HttpStatusCode.Accepted)
            }
            post("/restablecer") {
                val datos = call.receive<RestablecerSolicitud>()
                cuentas.restablecer(datos.token, datos.contrasena)
                call.respond(HttpStatusCode.NoContent)
            }
        }
        post("/refresco") {
            call.respond(cuentas.refrescar(call.receive<RefrescoSolicitud>().tokenRefresco))
        }
        post("/cierre") {
            cuentas.cerrarSesion(call.receive<RefrescoSolicitud>().tokenRefresco)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
