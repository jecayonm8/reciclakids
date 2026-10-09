package com.reciclakids.model

import java.time.LocalDate

/** Las tres canecas del código de colores colombiano (Resolución 2184 de 2019). */
enum class CategoriaResiduo { Aprovechables, NoAprovechables, Organicos }

/** Fácil: 2 canecas sin tiempo. Medio: 3 canecas sin tiempo. Difícil: 3 canecas con temporizador y más residuos. */
enum class Dificultad(val canecas: Int, val residuos: Int, val conTiempo: Boolean) {
    Facil(canecas = 2, residuos = 8, conTiempo = false),
    Medio(canecas = 3, residuos = 8, conTiempo = false),
    Dificil(canecas = 3, residuos = 12, conTiempo = true),
}

enum class EstadoReto { Publicado, Programado, Borrador }

/** Un reto diario de la biblioteca de la docente. */
data class Reto(
    val id: String,
    val nombre: String,
    val categorias: Set<CategoriaResiduo>,
    val dificultad: Dificultad,
    /** Día en que lo juega el grupo; `null` en un borrador que todavía no tiene fecha. */
    val fecha: LocalDate?,
    val estado: EstadoReto,
    /** Código de 4 dígitos que se genera al publicar. Vale hasta la medianoche de [fecha]. */
    val codigo: String? = null,
    /** Aciertos promedio del grupo (%), cuando ya se jugó. */
    val aciertosPromedio: Int? = null,
    /** Reto de la biblioteca del que salió esta copia. */
    val reutilizadoDe: String? = null,
) {
    /** Solo un reto publicado para [hoy] tiene un código con el que los niños pueden entrar. */
    fun codigoActivo(hoy: LocalDate): String? = codigo?.takeIf { estado == EstadoReto.Publicado && fecha == hoy }

    /** Lo ya publicado no se edita: se duplica. */
    val editable: Boolean get() = estado != EstadoReto.Publicado
}

/** Lo que la docente arma en el asistente (UI-22) antes de publicar. */
data class RetoBorrador(
    val categorias: Set<CategoriaResiduo>,
    val dificultad: Dificultad,
    /** Hoy publica ya; un día posterior deja el reto programado. */
    val fecha: LocalDate,
    /** Reto programado o borrador que se está editando; `null` si es nuevo. */
    val idExistente: String? = null,
    val nombre: String? = null,
    val reutilizadoDe: String? = null,
)
