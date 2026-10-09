package com.reciclakids.viewmodel

import com.reciclakids.model.CategoriaResiduo
import com.reciclakids.model.Dificultad
import com.reciclakids.model.Reto
import com.reciclakids.util.mismaSemana
import java.time.LocalDate

/** Filtros de la biblioteca (UI-23). Se aplica uno a la vez, como en el prototipo. */
enum class FiltroReto {
    Todos,
    Aprovechables,
    Organicos,
    NoAprovechables,
    Facil,
    Medio,
    EstaSemana;

    fun coincide(reto: Reto, hoy: LocalDate): Boolean = when (this) {
        Todos -> true
        Aprovechables -> CategoriaResiduo.Aprovechables in reto.categorias
        Organicos -> CategoriaResiduo.Organicos in reto.categorias
        NoAprovechables -> CategoriaResiduo.NoAprovechables in reto.categorias
        Facil -> reto.dificultad == Dificultad.Facil
        Medio -> reto.dificultad == Dificultad.Medio
        EstaSemana -> reto.fecha?.let { mismaSemana(it, hoy) } == true
    }
}

fun List<Reto>.filtrados(filtro: FiltroReto, hoy: LocalDate): List<Reto> = filter { filtro.coincide(it, hoy) }
