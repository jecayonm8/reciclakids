package com.reciclakids.plugins

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.serialization.json.Json

/** Formato JSON de la API: tolera campos nuevos de versiones posteriores de la app. */
val JsonApi = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}

fun Application.configurarSerializacion() {
    install(ContentNegotiation) { json(JsonApi) }
}
