package com.reciclakids.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import kotlin.time.Duration.Companion.minutes

// Límites contra fuerza bruta: las contraseñas y, sobre todo, el código del reto, que tiene solo
// 4 dígitos. Se cuentan por IP de origen.

val LimiteAcceso = RateLimitName("acceso")
val LimiteCodigoReto = RateLimitName("codigo-reto")

fun Application.configurarLimites() {
    install(RateLimit) {
        register(LimiteAcceso) {
            rateLimiter(limit = 10, refillPeriod = 1.minutes)
            requestKey { call -> call.request.origin.remoteAddress }
        }
        register(LimiteCodigoReto) {
            // Un salón entero entra por la misma red: el límite deja pasar a un grupo, no un barrido.
            rateLimiter(limit = 60, refillPeriod = 1.minutes)
            requestKey { call -> call.request.origin.remoteAddress }
        }
    }
}
