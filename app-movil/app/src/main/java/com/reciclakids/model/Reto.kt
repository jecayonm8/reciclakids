package com.reciclakids.model

import java.time.LocalDate

/** Las tres categorías de la Resolución 2184 de 2019. Cada una tiene su caneca. */
enum class CategoriaResiduo { Aprovechable, NoAprovechable, Organico }

data class Residuo(val id: String, val nombre: String, val categoria: CategoriaResiduo)

/**
 * Qué tan difícil es un reto: cuántos residuos trae y si tiene tiempo. No decide las canecas: esas
 * las elige la docente en el paso 1 del asistente.
 */
enum class Dificultad(val conTiempo: Boolean) {
    /** Pocos residuos, sin tiempo. */
    Facil(conTiempo = false),

    /** Más residuos que Fácil, sin tiempo. */
    Medio(conTiempo = false),

    /** Todavía más residuos y un temporizador visual sin números. */
    Dificil(conTiempo = true),
}

/** Residuos que trae un reto de cada dificultad. Lo usan el juego y el resumen del asistente docente. */
val Dificultad.residuosPorReto: Int
    get() = when (this) {
        Dificultad.Facil -> 4
        Dificultad.Medio -> 6
        Dificultad.Dificil -> 8
    }

/**
 * Reto diario publicado por la docente. El [codigo] de cuatro dígitos no identifica a ningún
 * niño y solo sirve el día [fecha]: vence a medianoche.
 */
data class Reto(
    val codigo: String,
    val fecha: LocalDate,
    val dificultad: Dificultad,
    val residuos: List<Residuo>,
    /** Las canecas que eligió la docente: de ellas salen los [residuos]. */
    val categorias: Set<CategoriaResiduo> = residuos.mapTo(mutableSetOf()) { it.categoria },
    /** Id del reto en el backend: con él se sincronizan las partidas (el código se repite otros días). */
    val id: String = codigo,
) {
    /** Las canecas que ve el niño mientras juega este reto. */
    val canecas: List<CategoriaResiduo> get() = canecasDelJuego(categorias)
}

/**
 * Canecas que se muestran en el juego para un reto de [categorias]: las de esas categorías. Si
 * el reto es de una sola, salen las tres para que el niño tenga que elegir.
 */
fun canecasDelJuego(categorias: Set<CategoriaResiduo>): List<CategoriaResiduo> =
    if (categorias.size < 2) CategoriaResiduo.entries else CategoriaResiduo.entries.filter { it in categorias }

/** Niño o niña del grupo. Datos mínimos: nombre e inicial del apellido (Ley 1581 de 2012). */
data class Nino(val id: String, val nombre: String)

/** Residuos que conoce el juego. Las ilustraciones las entrega el ilustrador. */
object CatalogoResiduos {
    val botellaPlastica = Residuo("botella", "botella plástica", CategoriaResiduo.Aprovechable)
    val lata = Residuo("lata", "lata de jugo", CategoriaResiduo.Aprovechable)
    val papel = Residuo("papel", "papel", CategoriaResiduo.Aprovechable)
    val carton = Residuo("carton", "cartón", CategoriaResiduo.Aprovechable)
    val cascaraBanano = Residuo("cascara", "cáscara de banano", CategoriaResiduo.Organico)
    val restosComida = Residuo("restos", "resto de comida", CategoriaResiduo.Organico)
    val servilleta = Residuo("servilleta", "servilleta usada", CategoriaResiduo.NoAprovechable)
    val empaqueMetalizado = Residuo("empaque", "empaque metalizado", CategoriaResiduo.NoAprovechable)

    val todos = listOf(
        botellaPlastica, cascaraBanano, servilleta, lata, restosComida, empaqueMetalizado, papel, carton,
    )

    fun porId(id: String): Residuo? = todos.firstOrNull { it.id == id }
}
