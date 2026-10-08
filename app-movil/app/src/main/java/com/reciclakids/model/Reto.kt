package com.reciclakids.model

import java.time.LocalDate

/** Las tres categorías de la Resolución 2184 de 2019. Cada una tiene su caneca. */
enum class CategoriaResiduo { Aprovechable, NoAprovechable, Organico }

data class Residuo(val id: String, val nombre: String, val categoria: CategoriaResiduo)

enum class Dificultad(val categorias: List<CategoriaResiduo>, val conTiempo: Boolean) {
    /** Dos canecas (blanca y verde), sin tiempo. */
    Facil(listOf(CategoriaResiduo.Aprovechable, CategoriaResiduo.Organico), conTiempo = false),

    /** Las tres canecas, sin tiempo. */
    Medio(CategoriaResiduo.entries, conTiempo = false),

    /** Las tres canecas, más objetos y un temporizador visual sin números. */
    Dificil(CategoriaResiduo.entries, conTiempo = true),
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
)

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
