package com.reciclakids.network

import com.reciclakids.model.Nino
import com.reciclakids.model.Reto
import java.time.LocalDate

sealed interface ResultadoCodigo {
    data class Valido(val reto: Reto) : ResultadoCodigo

    /** Ningún reto tiene ese código. */
    data object Invalido : ResultadoCodigo

    /** El código existe pero es de otro día (RNF-05: solo vale el día en que se creó). */
    data object Vencido : ResultadoCodigo
}

/** Retos del día para el Modo Niño. Al ingresar el código, el reto queda descargado para jugar sin conexión. */
interface ServicioRetos {
    suspend fun buscarReto(codigo: String, hoy: LocalDate): ResultadoCodigo

    /** Niños del grupo que publicó el reto, para que cada uno elija su avatar (UI-09). */
    suspend fun ninosDelGrupo(reto: Reto): List<Nino>
}
