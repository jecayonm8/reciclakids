package com.reciclakids.config

import com.reciclakids.models.Rol
import com.reciclakids.plugins.Tokens
import io.ktor.server.config.MapApplicationConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConfiguracionTest {

    private fun config(vararg cambios: Pair<String, String>) = MapApplicationConfig(
        "reciclakids.entorno" to "desarrollo",
        "reciclakids.zonaHoraria" to "America/Bogota",
        "reciclakids.urlPublica" to "http://localhost:8080/",
        "reciclakids.baseDatos.url" to "jdbc:postgresql://localhost/reciclakids",
        "reciclakids.baseDatos.usuario" to "u",
        "reciclakids.baseDatos.contrasena" to "c",
        "reciclakids.baseDatos.maxConexiones" to "5",
        "reciclakids.jwt.secreto" to Configuracion.SecretoJwtDeEjemplo,
        "reciclakids.jwt.emisor" to "e",
        "reciclakids.jwt.audiencia" to "a",
        "reciclakids.correos.proveedor" to "memoria",
        "reciclakids.correos.remitente" to "r@x.co",
        "reciclakids.correos.regionSes" to "us-east-1",
        "reciclakids.correos.vistaPrevia" to "true",
        "reciclakids.tareas.clave" to "",
    ).apply { cambios.forEach { (clave, valor) -> put("reciclakids.$clave", valor) } }

    @Test
    fun enDesarrolloAceptaLosValoresDeEjemplo() {
        val configuracion = Configuracion.desde(config())
        assertFalse(configuracion.produccion)
        assertEquals("http://localhost:8080", configuracion.urlPublica)
    }

    @Test
    fun enProduccionNoArrancaConSecretosDeEjemplo() {
        assertFailsWith<IllegalStateException> { Configuracion.desde(config("entorno" to "produccion")) }
        val segura = config(
            "entorno" to "produccion",
            "jwt.secreto" to "x".repeat(40),
            "urlPublica" to "https://api.reciclakids.app",
            "correos.vistaPrevia" to "false",
            "correos.proveedor" to "ses",
            "tareas.clave" to "y".repeat(30),
        )
        assertTrue(Configuracion.desde(segura).produccion)
        assertFailsWith<IllegalStateException> {
            Configuracion.desde(config("entorno" to "produccion", "jwt.secreto" to "x".repeat(40), "urlPublica" to "http://inseguro"))
        }
    }

    @Test
    fun losTokensNoSeConfundenEntreSi() {
        val tokens = Tokens(ConfiguracionJwt("secreto-de-prueba", "e", "a"))
        val baja = tokens.baja("acudiente-1", "nino-1")
        assertEquals("acudiente-1" to "nino-1", tokens.leerBaja(baja))
        // Un token de acceso no sirve como enlace de baja.
        assertNull(tokens.leerBaja(tokens.accesoAdulto("acudiente-1", Rol.ACUDIENTE).token))
        assertNull(tokens.leerBaja("basura"))
        assertEquals(64, Tokens.hash("abc").length)
    }
}
