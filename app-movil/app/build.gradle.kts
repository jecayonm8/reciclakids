import java.net.URI

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// Dirección de la API (ADR-07). En debug, el backend de `docker compose up` en el PC: 10.0.2.2 es
// el PC visto desde el emulador; un teléfono en la misma red usa la IP del PC con
// -Preciclakids.apiUrl=http://192.168.1.5:8080/api/v1/. En release, la URL HTTPS de Cloud Run por
// la misma propiedad o por la variable de entorno RECICLAKIDS_API_URL (docs/despliegue.md, paso 8).
val urlApiDada: String? = providers.gradleProperty("reciclakids.apiUrl")
    .orElse(providers.environmentVariable("RECICLAKIDS_API_URL"))
    .orNull?.trim()?.let { if (it.endsWith("/")) it else "$it/" }
val urlApiDebug = urlApiDada ?: "http://10.0.2.2:8080/api/v1/"

/** Release sin URL: compila (lo hace la CI), pero no se puede firmar para distribuir. */
val urlApiSinConfigurar = "https://reciclakids.invalid/api/v1/"
val urlApiRelease = urlApiDada ?: urlApiSinConfigurar

// El keystore de release y sus claves solo existen en la máquina que publica: nunca en el repositorio.
val firmaRelease: Map<String, String>? = listOf(
    "RECICLAKIDS_KEYSTORE",
    "RECICLAKIDS_KEYSTORE_CLAVE",
    "RECICLAKIDS_KEY_ALIAS",
    "RECICLAKIDS_KEY_CLAVE",
).associateWith { providers.environmentVariable(it).orNull.orEmpty() }.takeIf { variables -> variables.values.all { it.isNotBlank() } }

/** Direcciones de desarrollo a las que la app debug puede hablar sin TLS. */
val hostsDesarrollo: Set<String> = buildSet {
    addAll(listOf("10.0.2.2", "localhost", "127.0.0.1"))
    val uri = URI(urlApiDebug)
    if (uri.scheme == "http" && uri.host != null) add(uri.host)
}

android {
    namespace = "com.reciclakids"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.reciclakids"
        minSdk = 28
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (firmaRelease != null) {
            create("release") {
                storeFile = file(firmaRelease.getValue("RECICLAKIDS_KEYSTORE"))
                storePassword = firmaRelease.getValue("RECICLAKIDS_KEYSTORE_CLAVE")
                keyAlias = firmaRelease.getValue("RECICLAKIDS_KEY_ALIAS")
                keyPassword = firmaRelease.getValue("RECICLAKIDS_KEY_CLAVE")
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_URL", "\"$urlApiDebug\"")
            manifestPlaceholders["configuracionRed"] = "@xml/red_desarrollo"
        }
        release {
            buildConfigField("String", "API_URL", "\"$urlApiRelease\"")
            manifestPlaceholders["configuracionRed"] = "@xml/red_produccion"
            signingConfig = signingConfigs.findByName("release")
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests {
            // Robolectric necesita los recursos (fuentes, vectores) para probar composables en la JVM.
            isIncludeAndroidResources = true
        }
    }
}

/**
 * Network security config de debug: texto plano solo hacia las direcciones locales de
 * [hostsDesarrollo]. Se genera porque la IP del PC cambia según la red del teléfono.
 */
abstract class GenerarRedDesarrollo : DefaultTask() {
    @get:Input
    abstract val hosts: SetProperty<String>

    @get:OutputDirectory
    abstract val salida: DirectoryProperty

    @TaskAction
    fun generar() {
        val dominios = hosts.get().sorted().joinToString("\n") { "        <domain includeSubdomains=\"false\">$it</domain>" }
        val archivo = salida.get().file("xml/red_desarrollo.xml").asFile
        archivo.parentFile.mkdirs()
        archivo.writeText(
            """
            |<?xml version="1.0" encoding="utf-8"?>
            |<!-- Generado por la tarea generarRedDesarrollo: solo existe en debug. -->
            |<network-security-config>
            |    <base-config cleartextTrafficPermitted="false" />
            |    <domain-config cleartextTrafficPermitted="true">
            |$dominios
            |    </domain-config>
            |</network-security-config>
            |""".trimMargin()
        )
    }
}

androidComponents {
    onVariants(selector().withBuildType("debug")) { variant ->
        val tarea = tasks.register<GenerarRedDesarrollo>("generarRedDesarrollo${variant.name.replaceFirstChar { it.uppercase() }}") {
            hosts.set(hostsDesarrollo)
        }
        variant.sources.res?.addGeneratedSourceDirectory(tarea, GenerarRedDesarrollo::salida)
    }
}

// Un APK de release firmado siempre habla HTTPS con una URL real; sin firma solo es una compilación.
val validarUrlApiRelease = tasks.register("validarUrlApiRelease") {
    val url = urlApiRelease
    val sinConfigurar = urlApiSinConfigurar
    val firmada = firmaRelease != null
    doLast {
        if (!url.startsWith("https://")) {
            throw GradleException("La URL de la API de release debe ser HTTPS: $url")
        }
        if (firmada && url == sinConfigurar) {
            throw GradleException(
                "Falta la URL de la API para la release firmada: -Preciclakids.apiUrl=https://… o RECICLAKIDS_API_URL"
            )
        }
    }
}
tasks.matching { it.name == "preReleaseBuild" }.configureEach { dependsOn(validarUrlApiRelease) }

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    testImplementation(libs.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.client.mock)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
