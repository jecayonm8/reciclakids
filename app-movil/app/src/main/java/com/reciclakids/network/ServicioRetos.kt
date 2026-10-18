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

    /** No se pudo preguntar al backend (sin red o demasiados intentos seguidos): se puede reintentar. */
    data object SinConexion : ResultadoCodigo
}

/** Retos del día para el Modo Niño. Al ingresar el código, el reto queda descargado para jugar sin conexión. */
interface ServicioRetos {
    suspend fun buscarReto(codigo: String, hoy: LocalDate): ResultadoCodigo

    /** Niños del grupo que publicó el reto, para que cada uno elija su avatar (UI-09). */
    suspend fun ninosDelGrupo(reto: Reto): List<Nino>

    /**
     * El niño eligió su nombre: el backend le da el token con el que se envían sus resultados y su
     * límite diario de juego. Sin red no pasa nada: el niño juega igual y el sincronizador vuelve a
     * intentarlo mientras el código siga vigente.
     */
    suspend fun abrirSesion(reto: Reto, nino: Nino) {}
}
