package com.reciclakids.routes

import com.reciclakids.models.dto.ControlParentalDto
import com.reciclakids.models.dto.PerfilAcudienteSolicitud
import com.reciclakids.models.dto.SolicitudEliminacionDto
import com.reciclakids.models.dto.VincularSolicitud
import com.reciclakids.plugins.DatosInvalidos
import com.reciclakids.plugins.Proveedor
import com.reciclakids.plugins.sesionAdulto
import com.reciclakids.services.PadresServicio
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import java.time.LocalDate

/** API del Modo Padres (UI-29 a UI-35). Solo con el token de una cuenta de acudiente. */
fun Route.rutasAcudiente(servicio: PadresServicio) {
    authenticate(Proveedor.Acudiente) {
        route("/acudiente") {
            get("/hijos") { call.respond(servicio.hijos(call.acudiente())) }
            post("/vinculaciones") {
                call.respond(servicio.vincular(call.acudiente(), call.receive<VincularSolicitud>().codigo))
            }

            route("/hijos/{id}") {
                get("/inicio") { call.respond(servicio.inicio(call.acudiente(), call.id())) }
                get("/reporte") {
                    val semana = call.request.queryParameters["semana"]?.let {
                        runCatching { LocalDate.parse(it) }.getOrElse { throw DatosInvalidos("Semana inválida") }
                    }
                    call.respond(servicio.reporte(call.acudiente(), call.id(), semana))
                }
                get("/logros") { call.respond(servicio.logros(call.acudiente(), call.id())) }
                get("/correos") { call.respond(servicio.correos(call.acudiente(), call.id())) }
                route("/control-parental") {
                    get { call.respond(servicio.controlParental(call.acudiente(), call.id())) }
                    put {
                        servicio.guardarControlParental(call.acudiente(), call.id(), call.receive<ControlParentalDto>())
                        call.respond(HttpStatusCode.NoContent)
                    }
                }
                route("/privacidad") {
                    get { call.respond(servicio.privacidad(call.acudiente(), call.id())) }
                    post("/solicitud-eliminacion") {
                        val fecha = servicio.solicitarEliminacion(call.acudiente(), call.id())
                        call.respond(HttpStatusCode.Created, SolicitudEliminacionDto(fecha.toString()))
                    }
                    delete("/solicitud-eliminacion") {
                        servicio.cancelarEliminacion(call.acudiente(), call.id())
                        call.respond(HttpStatusCode.NoContent)
                    }
                }
            }

            route("/perfil") {
                get { call.respond(servicio.perfil(call.acudiente())) }
                put { call.respond(servicio.actualizarPerfil(call.acudiente(), call.receive<PerfilAcudienteSolicitud>())) }
            }
        }
    }
}

private fun ApplicationCall.acudiente(): String = sesionAdulto().usuarioId
