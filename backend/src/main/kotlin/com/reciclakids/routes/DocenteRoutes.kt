package com.reciclakids.routes

import com.reciclakids.models.dto.AgregarNinoSolicitud
import com.reciclakids.models.dto.AvisosDocenteDto
import com.reciclakids.models.dto.CodigoVinculacionDto
import com.reciclakids.models.dto.EnvioReporteDto
import com.reciclakids.models.dto.IngresosDto
import com.reciclakids.models.dto.PerfilDocenteSolicitud
import com.reciclakids.models.dto.RetoBorradorSolicitud
import com.reciclakids.plugins.Proveedor
import com.reciclakids.plugins.sesionAdulto
import com.reciclakids.services.DocenteServicio
import com.reciclakids.services.NotificacionesServicio
import com.reciclakids.services.Reloj
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

/** API del Modo Docente (UI-21 a UI-28). Solo con el token de una cuenta de docente. */
fun Route.rutasDocente(servicio: DocenteServicio, notificaciones: NotificacionesServicio, reloj: Reloj) {
    authenticate(Proveedor.Docente) {
        route("/docente") {
            get("/tablero") { call.respond(servicio.tablero(call.docente())) }

            route("/retos") {
                get { call.respond(servicio.biblioteca(call.docente())) }
                post { call.respond(HttpStatusCode.Created, servicio.publicar(call.docente(), call.receive<RetoBorradorSolicitud>())) }
                get("/{id}") { call.respond(servicio.reto(call.docente(), call.id())) }
                // La app la consulta cada pocos segundos mientras muestra el código (UI-24).
                get("/{id}/ingresos") { call.respond(IngresosDto(servicio.ninosQueEntraron(call.docente(), call.id()))) }
            }

            get("/grupo/hoy") { call.respond(servicio.grupoHoy(call.docente())) }
            delete("/grupo/datos") {
                servicio.eliminarDatosGrupo(call.docente())
                call.respond(HttpStatusCode.NoContent)
            }

            route("/ninos") {
                post { call.respond(HttpStatusCode.Created, servicio.agregarNino(call.docente(), call.receive<AgregarNinoSolicitud>())) }
                get("/{id}") { call.respond(servicio.detalleNino(call.docente(), call.id())) }
                delete("/{id}") {
                    servicio.quitarNino(call.docente(), call.id())
                    call.respond(HttpStatusCode.NoContent)
                }
                post("/{id}/codigo-vinculacion") {
                    call.respond(CodigoVinculacionDto(servicio.regenerarCodigo(call.docente(), call.id())))
                }
            }

            route("/reportes/semanal") {
                get { call.respond(servicio.reporteSemanal(call.docente())) }
                post("/envio") {
                    // La misma semana que muestra el reporte: la última semana escolar terminada.
                    val lunes = DocenteServicio.ultimaSemanaCompleta(reloj.hoy())
                    val enviados = notificaciones.reporteSemanalDeGrupo(servicio.grupoId(call.docente()), lunes)
                    call.respond(EnvioReporteDto(enviados))
                }
            }

            route("/perfil") {
                get { call.respond(servicio.perfil(call.docente())) }
                put { call.respond(servicio.actualizarPerfil(call.docente(), call.receive<PerfilDocenteSolicitud>())) }
            }
            put("/avisos") {
                servicio.guardarAvisos(call.docente(), call.receive<AvisosDocenteDto>())
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

private fun ApplicationCall.docente(): String = sesionAdulto().usuarioId

internal fun ApplicationCall.id(): String = parameters["id"].orEmpty()
