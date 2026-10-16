package com.reciclakids.models

import kotlinx.serialization.Serializable

// Catálogo y reglas del juego. Deben coincidir con los de la app (app-movil/.../model): los
// nombres de los enums viajan tal cual en el JSON para que la app los lea con valueOf.

/** Las tres categorías de la Resolución 2184 de 2019. Cada una tiene su caneca. */
@Serializable
enum class CategoriaResiduo { Aprovechable, NoAprovechable, Organico }

data class Residuo(val id: String, val nombre: String, val categoria: CategoriaResiduo)

/** Residuos que conoce el juego, con los mismos id que la app. */
object CatalogoResiduos {
    val todos = listOf(
        Residuo("botella", "botella plástica", CategoriaResiduo.Aprovechable),
        Residuo("cascara", "cáscara de banano", CategoriaResiduo.Organico),
        Residuo("servilleta", "servilleta usada", CategoriaResiduo.NoAprovechable),
        Residuo("lata", "lata de jugo", CategoriaResiduo.Aprovechable),
        Residuo("restos", "resto de comida", CategoriaResiduo.Organico),
        Residuo("empaque", "empaque metalizado", CategoriaResiduo.NoAprovechable),
        Residuo("papel", "papel", CategoriaResiduo.Aprovechable),
        Residuo("carton", "cartón", CategoriaResiduo.Aprovechable),
    )

    fun porId(id: String): Residuo? = todos.firstOrNull { it.id == id }
}

/** Cuántos residuos trae un reto y si tiene tiempo. Las canecas las elige la docente. */
@Serializable
enum class Dificultad(val residuosPorReto: Int, val conTiempo: Boolean) {
    Facil(4, false),
    Medio(6, false),
    Dificil(8, true),
}

/** Tiempo del temporizador visual en dificultad difícil. */
const val LimiteDificilMs = 90_000L

/** Retos completados que llevan el acuario al 100 % («Agua cristalina»). */
const val RetosParaAcuarioLimpio = 15
const val RetosParaPulpoOrdenado = 10
const val RetosParaGuardian = 20
const val DiasParaCincoRetos = 5

@Serializable
enum class EstadoReto { Publicado, Programado, Borrador }

/** Las 8 insignias del diseño, con los mismos nombres de enum que la app. */
@Serializable
enum class Insignia(val nombre: String, val logro: String) {
    AmigaTortuga("Amiga tortuga", "Terminó el reto sin ninguna ayuda"),
    RapidoComoPez("Rápido como pez", "Terminó el reto difícil a tiempo"),
    RachaDeCinco("Racha de 5", "Separó cinco residuos seguidos sin error"),
    CincoRetosDiarios("5 retos diarios", "Jugó el reto en cinco días distintos"),
    PulpoOrdenado("Pulpo ordenado", "Completó 10 retos"),
    AguaCristalina("Agua cristalina", "Dejó su acuario limpio"),
    CoralFeliz("Coral feliz", "Separó diez residuos seguidos sin error"),
    GuardianDelMar("Guardián del mar", "Completó 20 retos"),
}

/** Lo que el servidor sabe de un niño al terminar una partida, para decidir sus insignias. */
data class HistorialNino(
    val retosCompletados: Int,
    val diasConReto: Int,
    val mejorRachaEntreRetos: Int,
)

/**
 * Insignias que cumple una partida terminada. Mismas reglas que `insigniasNuevas` en la app; el
 * servidor las recalcula para que Docente y Padres vean lo mismo aunque se cambie de teléfono.
 */
fun insigniasCumplidas(
    errores: Int,
    dificultad: Dificultad,
    tiempoMs: Long,
    historial: HistorialNino,
): Set<Insignia> = buildSet {
    if (errores == 0) add(Insignia.AmigaTortuga)
    if (dificultad == Dificultad.Dificil && tiempoMs in 1..LimiteDificilMs) add(Insignia.RapidoComoPez)
    if (historial.mejorRachaEntreRetos >= 5) add(Insignia.RachaDeCinco)
    if (historial.diasConReto >= DiasParaCincoRetos) add(Insignia.CincoRetosDiarios)
    if (historial.retosCompletados >= RetosParaPulpoOrdenado) add(Insignia.PulpoOrdenado)
    if (historial.retosCompletados >= RetosParaAcuarioLimpio) add(Insignia.AguaCristalina)
    if (historial.mejorRachaEntreRetos >= 10) add(Insignia.CoralFeliz)
    if (historial.retosCompletados >= RetosParaGuardian) add(Insignia.GuardianDelMar)
}

/**
 * Residuos de un reto: de las categorías elegidas, mezclados, tantos como pide la dificultad. Si
 * las categorías tienen menos residuos que los que pide la dificultad, se repiten.
 */
fun residuosDelReto(categorias: Set<CategoriaResiduo>, dificultad: Dificultad, azar: kotlin.random.Random): List<Residuo> {
    val disponibles = CatalogoResiduos.todos.filter { it.categoria in categorias }
    require(disponibles.isNotEmpty()) { "El reto necesita al menos una categoría" }
    return buildList {
        while (size < dificultad.residuosPorReto) addAll(disponibles.shuffled(azar))
    }.take(dificultad.residuosPorReto)
}

/** Las categorías se guardan en una sola columna, separadas por coma y en orden fijo. */
fun Set<CategoriaResiduo>.aColumna(): String = CategoriaResiduo.entries.filter { it in this }.joinToString(",") { it.name }

fun categoriasDeColumna(valor: String): Set<CategoriaResiduo> =
    valor.split(',').filter { it.isNotBlank() }.mapTo(linkedSetOf()) { CategoriaResiduo.valueOf(it) }

/** En la base los enums se guardan en mayúsculas (FACIL, PUBLICADO…). */
fun Dificultad.aColumna(): String = name.uppercase()
fun dificultadDeColumna(valor: String): Dificultad = Dificultad.entries.first { it.name.equals(valor, ignoreCase = true) }
fun EstadoReto.aColumna(): String = name.uppercase()
fun estadoDeColumna(valor: String): EstadoReto = EstadoReto.entries.first { it.name.equals(valor, ignoreCase = true) }

enum class Rol { DOCENTE, ACUDIENTE }
