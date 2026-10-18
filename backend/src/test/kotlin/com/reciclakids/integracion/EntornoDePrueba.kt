package com.reciclakids.integracion

import com.reciclakids.Contenedor
import com.reciclakids.config.BaseDatos
import com.reciclakids.config.Configuracion
import com.reciclakids.config.ConfiguracionBaseDatos
import com.reciclakids.config.ConfiguracionCorreos
import com.reciclakids.config.ConfiguracionJwt
import com.reciclakids.models.dto.SesionDto
import com.reciclakids.moduloCon
import com.reciclakids.plugins.JsonApi
import com.reciclakids.services.notificaciones.correos.EnviadorCorreoEnMemoria
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.testcontainers.postgresql.PostgreSQLContainer
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicInteger

val ZonaPiloto: ZoneId = ZoneId.of("America/Bogota")

/** PostgreSQL real en Docker, uno para toda la corrida. Cada prueba empieza con las tablas vacías. */
object PostgresDePrueba {
    val contenedor: PostgreSQLContainer by lazy {
        PostgreSQLContainer("postgres:17-alpine").apply { start() }
    }

    val bd: BaseDatos by lazy {
        BaseDatos.conectar(ConfiguracionBaseDatos(contenedor.jdbcUrl, contenedor.username, contenedor.password, 4))
    }

    fun limpiar() {
        transaction(bd.database) {
            exec(
                "TRUNCATE usuarios, sesiones, recuperaciones, grupos, ninos, vinculaciones, controles_parentales, " +
                    "retos, reto_residuos, ingresos, partidas, intentos, insignias_ganadas, correos_enviados, " +
                    "solicitudes_eliminacion CASCADE",
            )
        }
    }
}

/** Reloj que las pruebas mueven a voluntad: así se prueba que el código vence a medianoche. */
class RelojDePrueba(var instante: Instant) : Clock() {
    override fun getZone(): ZoneId = ZonaPiloto
    override fun withZone(zone: ZoneId): Clock = this
    override fun instant(): Instant = instante

    fun hoy(): LocalDate = LocalDate.ofInstant(instante, ZonaPiloto)
    fun avanzarDias(dias: Long) { instante = instante.plusSeconds(dias * 86_400) }

    companion object {
        /** Martes 13 de octubre de 2026 a las 9:00 en Bogotá. */
        fun martes() = RelojDePrueba(LocalDate.of(2026, 10, 13).atTime(LocalTime.of(9, 0)).atZone(ZonaPiloto).toInstant())
    }
}

fun configuracionDePrueba() = Configuracion(
    produccion = false,
    zona = ZonaPiloto,
    urlPublica = "https://reciclakids.test",
    baseDatos = ConfiguracionBaseDatos("", "", "", 1),
    jwt = ConfiguracionJwt("secreto-solo-para-las-pruebas-de-integracion", "reciclakids-backend", "reciclakids-app"),
    correos = ConfiguracionCorreos("memoria", "ReciclaKids <no-responder@reciclakids.test>", "us-east-1", vistaPrevia = false),
    claveTareas = "clave-de-tareas-de-prueba-larga",
)

private val ipsDePrueba = AtomicInteger(1)

/**
 * Servidor completo contra PostgreSQL. Cada cliente sale de una IP distinta para no chocar con el
 * límite de intentos, salvo que la prueba pida una fija.
 */
fun probarApi(reloj: RelojDePrueba = RelojDePrueba.martes(), bloque: suspend ApiDePrueba.() -> Unit) = testApplication {
    PostgresDePrueba.limpiar()
    val enviador = EnviadorCorreoEnMemoria()
    val contenedor = Contenedor(configuracionDePrueba(), PostgresDePrueba.bd, reloj, enviador)
    application { moduloCon(contenedor) }
    fun cliente(ip: String) = createClient {
        install(ContentNegotiation) { json(JsonApi) }
        defaultRequest {
            header("X-Forwarded-For", ip)
            contentType(ContentType.Application.Json)
        }
    }
    ApiDePrueba({ cliente(it ?: "10.0.${ipsDePrueba.get() / 250}.${ipsDePrueba.getAndIncrement() % 250 + 1}") }, reloj, enviador).bloque()
}

class ApiDePrueba(
    private val nuevoCliente: (ip: String?) -> HttpClient,
    val reloj: RelojDePrueba,
    val correos: EnviadorCorreoEnMemoria,
) {
    /** Cliente nuevo para cada llamada: cada una sale de una IP distinta. */
    val http: HttpClient get() = nuevoCliente(null)

    fun clienteDesde(ip: String): HttpClient = nuevoCliente(ip)

    suspend fun get(ruta: String, token: String? = null): HttpResponse = http.get(ruta) { autorizar(token) }
    suspend fun delete(ruta: String, token: String? = null): HttpResponse = http.delete(ruta) { autorizar(token) }
    suspend fun post(ruta: String, cuerpo: Any? = null, token: String? = null): HttpResponse =
        http.post(ruta) { autorizar(token); if (cuerpo != null) setBody(cuerpo) }
    suspend fun put(ruta: String, cuerpo: Any, token: String? = null): HttpResponse =
        http.put(ruta) { autorizar(token); setBody(cuerpo) }

    private fun HttpRequestBuilder.autorizar(token: String?) { if (token != null) bearerAuth(token) }
}

typealias Sesion = SesionDto
