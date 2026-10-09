package com.reciclakids.viewmodel

import com.reciclakids.model.CategoriaResiduo
import com.reciclakids.model.Dificultad
import com.reciclakids.model.Reto
import com.reciclakids.model.RetoBorrador
import com.reciclakids.util.siguientesDiasHabiles
import java.time.LocalDate

/**
 * Estado del asistente «Crear reto» (UI-22): categorías → dificultad → fecha → vista previa →
 * publicar. Es inmutable y no sabe de red; cada cambio devuelve un asistente nuevo.
 */
data class AsistenteReto(
    val hoy: LocalDate,
    val paso: Int = 1,
    // Como en el prototipo, arranca con las dos canecas de Fácil y en Medio, el más usado.
    val categorias: Set<CategoriaResiduo> = setOf(CategoriaResiduo.Aprovechables, CategoriaResiduo.Organicos),
    val dificultad: Dificultad = Dificultad.Medio,
    val programado: Boolean = false,
    val diasProgramables: List<LocalDate> = siguientesDiasHabiles(hoy, DiasProgramables),
    val diaProgramado: LocalDate = diasProgramables.first(),
    val idExistente: String? = null,
    val nombre: String? = null,
    val reutilizadoDe: String? = null,
) {
    /** Sin canecas no hay reto: no se pasa del paso 1 ni se publica. */
    val tieneCategorias: Boolean get() = categorias.isNotEmpty()

    val fecha: LocalDate get() = if (programado) diaProgramado else hoy

    val esUltimoPaso: Boolean get() = paso == TotalPasos

    fun alternar(categoria: CategoriaResiduo): AsistenteReto =
        copy(categorias = if (categoria in categorias) categorias - categoria else categorias + categoria)

    fun conDificultad(dificultad: Dificultad): AsistenteReto = copy(dificultad = dificultad)

    fun paraHoy(): AsistenteReto = copy(programado = false)

    fun programar(dia: LocalDate = diaProgramado): AsistenteReto =
        if (dia in diasProgramables) copy(programado = true, diaProgramado = dia) else this

    fun siguiente(): AsistenteReto = irA(paso + 1)

    fun atras(): AsistenteReto = irA(paso - 1)

    /** Los chips de paso dejan saltar a cualquier paso, pero no dejar atrás el paso 1 sin canecas. */
    fun irA(destino: Int): AsistenteReto = when {
        destino !in 1..TotalPasos -> this
        destino > 1 && !tieneCategorias -> this
        else -> copy(paso = destino)
    }

    /** «Crear a partir de un reto anterior» y «Duplicar»: misma configuración, fecha de hoy y directo a la vista previa. */
    fun reutilizando(reto: Reto): AsistenteReto = copy(
        paso = PasoVistaPrevia,
        categorias = reto.categorias,
        dificultad = reto.dificultad,
        programado = false,
        idExistente = null,
        nombre = reto.nombre,
        reutilizadoDe = reto.id,
    )

    /** Editar un reto programado o un borrador: se conserva su id y, si sigue vigente, su día. */
    fun editando(reto: Reto): AsistenteReto {
        val futura: LocalDate? = reto.fecha?.takeIf { it.isAfter(hoy) }
        val dias = if (futura != null && futura !in diasProgramables) (diasProgramables + futura).sorted() else diasProgramables
        return copy(
            paso = 1,
            categorias = reto.categorias,
            dificultad = reto.dificultad,
            programado = futura != null,
            diasProgramables = dias,
            diaProgramado = futura ?: dias.first(),
            idExistente = reto.id,
            nombre = reto.nombre,
            reutilizadoDe = reto.reutilizadoDe,
        )
    }

    fun aBorrador(): RetoBorrador = RetoBorrador(
        categorias = categorias,
        dificultad = dificultad,
        fecha = fecha,
        idExistente = idExistente,
        nombre = nombre,
        reutilizadoDe = reutilizadoDe,
    )

    companion object {
        const val TotalPasos = 5
        const val PasoVistaPrevia = 4
        const val DiasProgramables = 5
    }
}
