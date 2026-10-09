package com.reciclakids.model

import java.time.LocalDate

enum class EstadoReto { Publicado, Programado, Borrador }

/** Un reto de la biblioteca de la docente. Lo que juega el niño es su [Reto], que sale de este. */
data class RetoDocente(
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
