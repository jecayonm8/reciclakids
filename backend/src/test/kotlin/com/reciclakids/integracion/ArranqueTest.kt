package com.reciclakids.integracion

import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.config.mergeWith
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

/** Arranca el servidor como en Cloud Run: desde application.yaml, sin armar nada a mano. */
class ArranqueTest {

    @Test
    fun arrancaDesdeLaConfiguracion() = testApplication {
        PostgresDePrueba.bd // levanta el contenedor y aplica las migraciones
        val baseDatos = PostgresDePrueba.contenedor
        environment {
            config = ApplicationConfig("application.yaml").mergeWith(
                MapApplicationConfig(
                    "reciclakids.baseDatos.url" to baseDatos.jdbcUrl,
                    "reciclakids.baseDatos.usuario" to baseDatos.username,
                    "reciclakids.baseDatos.contrasena" to baseDatos.password,
                ),
            )
        }
        assertEquals(HttpStatusCode.OK, client.get("/salud").status)
        // En desarrollo la vista previa de correos queda encendida.
        assertEquals(HttpStatusCode.OK, client.get("/correos/vista-previa/logro").status)
        assertEquals(HttpStatusCode.NotFound, client.get("/tareas/reporte-semanal").status)
    }
}
