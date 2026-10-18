plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(ktorLibs.plugins.ktor)
    alias(libs.plugins.kover)
}

group = "com.reciclakids"
version = "1.0.0"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(21)
}

ktor {
    fatJar {
        archiveFileName.set("reciclakids-backend.jar")
    }
}

dependencies {
    implementation(ktorLibs.server.config.yaml)
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.netty)
    implementation(ktorLibs.server.contentNegotiation)
    implementation(ktorLibs.serialization.kotlinx.json)
    implementation(ktorLibs.server.auth)
    implementation(ktorLibs.server.auth.jwt)
    implementation(ktorLibs.server.statusPages)
    implementation(ktorLibs.server.callLogging)
    implementation(ktorLibs.server.callId)
    implementation(ktorLibs.server.rateLimit)
    implementation(ktorLibs.server.defaultHeaders)
    implementation(ktorLibs.server.forwardedHeader)
    implementation(libs.logback.classic)

    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.javaTime)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)
    implementation(libs.postgresql)
    implementation(libs.hikari)
    // Conexión a Cloud SQL por el conector de Google (ADR-05); solo se usa si se configura.
    runtimeOnly(libs.cloudSql.postgresSocketFactory)
    implementation(libs.bcrypt)
    // Amazon SES para los correos a acudientes (ADR-10), con el cliente HTTP liviano del JDK.
    implementation(platform(libs.aws.bom))
    implementation(libs.aws.sesv2) {
        exclude(group = "software.amazon.awssdk", module = "apache-client")
        exclude(group = "software.amazon.awssdk", module = "netty-nio-client")
    }
    implementation(libs.aws.urlConnectionClient)

    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.server.testHost)
    testImplementation(ktorLibs.client.contentNegotiation)
    testImplementation(libs.testcontainers.postgresql)
}

tasks.test {
    // Las pruebas de integración levantan PostgreSQL con Testcontainers: necesitan Docker.
    useJUnitPlatform()
}

// Cobertura (plan de calidad: al menos 70 % en la lógica de negocio del backend).
// Informe: ./gradlew koverHtmlReport → build/reports/kover/html/index.html
kover {
    reports {
        filters {
            includes {
                classes("com.reciclakids.services.*", "com.reciclakids.models.*", "com.reciclakids.repositories.*")
            }
        }
        verify {
            rule("Lógica de negocio") {
                minBound(70)
            }
        }
    }
}
