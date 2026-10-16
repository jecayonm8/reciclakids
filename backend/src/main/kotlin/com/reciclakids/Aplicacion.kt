package com.reciclakids

import com.reciclakids.config.BaseDatos
import com.reciclakids.config.Configuracion
import com.reciclakids.plugins.configurarErrores
import com.reciclakids.plugins.configurarLimites
import com.reciclakids.plugins.configurarMonitoreo
import com.reciclakids.plugins.configurarRutas
import com.reciclakids.plugins.configurarSeguridad
import com.reciclakids.plugins.configurarSerializacion
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

/** Módulo de Ktor que arranca el backend con la configuración de application.yaml. */
fun Application.module() {
    val configuracion = Configuracion.desde(environment.config)
    val bd = BaseDatos.conectar(configuracion.baseDatos)
    monitor.subscribe(ApplicationStopped) { bd.close() }
    module(Contenedor(configuracion, bd))
}

/** Arma el servidor con un [contenedor] ya construido: las pruebas pasan el suyo. */
fun Application.module(contenedor: Contenedor) {
    configurarMonitoreo()
    configurarSerializacion()
    configurarErrores()
    configurarLimites()
    configurarSeguridad(contenedor.tokens)
    configurarRutas(contenedor)
}
