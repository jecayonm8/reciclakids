package com.reciclakids.config

import io.ktor.server.config.ApplicationConfig
import java.time.ZoneId

/** Configuración del backend. Todo valor sensible llega por variable de entorno (ver application.yaml). */
data class Configuracion(
    val produccion: Boolean,
    val zona: ZoneId,
    val urlPublica: String,
    val baseDatos: ConfiguracionBaseDatos,
    val jwt: ConfiguracionJwt,
    val correos: ConfiguracionCorreos,
    val claveTareas: String,
) {
    companion object {
        const val SecretoJwtDeEjemplo = "solo-para-desarrollo-no-usar-en-produccion"

        fun desde(config: ApplicationConfig): Configuracion {
            fun texto(ruta: String) = config.property("reciclakids.$ruta").getString()
            val produccion = texto("entorno").equals("produccion", ignoreCase = true)
            val configuracion = Configuracion(
                produccion = produccion,
                zona = ZoneId.of(texto("zonaHoraria")),
                urlPublica = texto("urlPublica").trimEnd('/'),
                baseDatos = ConfiguracionBaseDatos(
                    url = texto("baseDatos.url"),
                    usuario = texto("baseDatos.usuario"),
                    contrasena = texto("baseDatos.contrasena"),
                    maxConexiones = texto("baseDatos.maxConexiones").toInt(),
                ),
                jwt = ConfiguracionJwt(
                    secreto = texto("jwt.secreto"),
                    emisor = texto("jwt.emisor"),
                    audiencia = texto("jwt.audiencia"),
                ),
                correos = ConfiguracionCorreos(
                    proveedor = texto("correos.proveedor").lowercase(),
                    remitente = texto("correos.remitente"),
                    regionSes = texto("correos.regionSes"),
                    vistaPrevia = texto("correos.vistaPrevia").toBoolean(),
                ),
                claveTareas = texto("tareas.clave"),
            )
            if (produccion) configuracion.validarProduccion()
            return configuracion
        }
    }

    /** En producción no se arranca con valores de ejemplo: mejor fallar al desplegar que exponer datos de menores. */
    private fun validarProduccion() {
        check(jwt.secreto != SecretoJwtDeEjemplo && jwt.secreto.length >= 32) {
            "JWT_SECRET debe tener al menos 32 caracteres en producción"
        }
        check(urlPublica.startsWith("https://")) { "RECICLAKIDS_URL_PUBLICA debe usar HTTPS en producción (ADR-07)" }
        check(!correos.vistaPrevia) { "Apaga la vista previa de correos en producción (RECICLAKIDS_VISTA_PREVIA_CORREOS=false)" }
        check(correos.proveedor == "ses") { "En producción los correos salen por Amazon SES (CORREO_PROVEEDOR=ses)" }
        check(claveTareas.length >= 24) { "TAREAS_CLAVE debe tener al menos 24 caracteres en producción" }
    }
}

data class ConfiguracionBaseDatos(val url: String, val usuario: String, val contrasena: String, val maxConexiones: Int)

data class ConfiguracionJwt(val secreto: String, val emisor: String, val audiencia: String)

data class ConfiguracionCorreos(val proveedor: String, val remitente: String, val regionSes: String, val vistaPrevia: Boolean)
