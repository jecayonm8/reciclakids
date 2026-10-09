package com.reciclakids.viewmodel

import com.reciclakids.model.CategoriaResiduo
import com.reciclakids.model.Confusion
import com.reciclakids.model.DetalleNino

/** Umbral desde el que una caneca se considera dominada; coincide con el verde de las barras. */
const val UmbralAciertosAlto = 80

/** Frase interpretativa de UI-27: qué se le dificulta al niño y qué reto le ayudaría. */
sealed interface MensajeDetalleNino {
    data class Confunde(val confusion: Confusion) : MensajeDetalleNino
    data class CanecaDificil(val categoria: CategoriaResiduo, val aciertos: Int) : MensajeDetalleNino
    data object VaMuyBien : MensajeDetalleNino
}

fun DetalleNino.mensaje(): MensajeDetalleNino? {
    if (sinResultados) return null
    confusion?.let { return MensajeDetalleNino.Confunde(it) }
    val masDificil = aciertosPorCategoria.minByOrNull { it.value } ?: return null
    return if (masDificil.value < UmbralAciertosAlto) {
        MensajeDetalleNino.CanecaDificil(masDificil.key, masDificil.value)
    } else {
        MensajeDetalleNino.VaMuyBien
    }
}

/** Puntos que subieron (o bajaron) los aciertos entre la primera y la última semana. */
val DetalleNino.cambioAciertos: Int? get() = if (aciertosPorSemana.size < 2) null else aciertosPorSemana.last() - aciertosPorSemana.first()
