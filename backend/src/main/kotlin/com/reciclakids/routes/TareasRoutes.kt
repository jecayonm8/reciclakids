package com.reciclakids.routes

import com.reciclakids.plugins.NoAutenticado
import com.reciclakids.services.DocenteServicio
import com.reciclakids.services.NotificacionesServicio
import com.reciclakids.services.Reloj
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable
import java.security.MessageDigest

@Serializable
data class ResultadoTareaDto(val correosEnviados: Int)

/**
 * Tareas que dispara Cloud Scheduler (ver docs/despliegue.md). Cloud Run escala a cero, así que no
 * hay temporizadores dentro del servidor: el programador llama a estas rutas con la clave.
 */
fun Route.rutasTareas(clave: String, notificaciones: NotificacionesServicio, reloj: Reloj) {
    post("/tareas/reporte-semanal") {
        val recibida = call.request.headers["X-Tareas-Clave"].orEmpty()
        if (!MessageDigest.isEqual(recibida.toByteArray(), clave.toByteArray())) throw NoAutenticado()
        // Corre los viernes por la tarde: el reporte es de la semana en curso.
        val lunes = DocenteServicio.inicioSemana(reloj.hoy())
        call.respond(ResultadoTareaDto(notificaciones.reporteSemanalDeTodos(lunes)))
    }
}
