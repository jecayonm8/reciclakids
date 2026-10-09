package com.reciclakids.services.notificaciones.correos

import java.time.LocalDate

/**
 * Canecas del código de colores colombiano (Resolución 2184 de 2019), en el orden en que las
 * lista el correo. Igual que en la app, nunca se distinguen solo por color: cada fila del correo
 * lleva muestra de la caneca, nombre de la categoría y porcentaje.
 */
enum class TipoCaneca(
    /** Rótulo de la fila: «Aprovechables». */
    val categoria: String,
    /** La categoría dentro de una frase: «mejoró separando orgánicos». */
    val categoriaEnFrase: String,
    /** «caneca blanca», para el texto plano, donde no hay muestra de color. */
    val nombre: String,
    val color: String,
    val borde: String,
) {
    Blanca("Aprovechables", "aprovechables", "caneca blanca", "#FFFFFF", "#B8C4C7"),
    Verde("Orgánicos", "orgánicos", "caneca verde", "#1E8E4E", "#1E8E4E"),
    Negra("No aprovechables", "no aprovechables", "caneca negra", "#23262A", "#23262A"),
}

/**
 * Frase interpretativa del reporte, con una sola parte resaltada en negrilla: «Esta semana mejoró
 * separando **orgánicos**: de 74 % a 92 % de aciertos.». La arma el componente de Reportes a
 * partir de los datos del propio niño; nunca lo compara con el grupo.
 */
data class MensajeInterpretativo(
    val antes: String,
    val resaltado: String,
    val despues: String,
) {
    init {
        require(resaltado.isNotBlank()) { "El mensaje interpretativo necesita una parte resaltada" }
    }

    val textoCompleto: String get() = antes + resaltado + despues

    companion object {
        /** La frase de mejora del diseño para una caneca: de [desde] % a [hasta] % de aciertos. */
        fun mejoraEn(caneca: TipoCaneca, desde: Int, hasta: Int): MensajeInterpretativo {
            require(desde in 0..100 && hasta in 0..100) { "Los porcentajes van de 0 a 100" }
            return MensajeInterpretativo(
                antes = "Esta semana mejoró separando ",
                resaltado = caneca.categoriaEnFrase,
                despues = ": de ${formatearPorcentaje(desde)} a ${formatearPorcentaje(hasta)} de aciertos.",
            )
        }
    }
}

/**
 * Tiempo de juego de la semana frente al límite diario del control parental (UI-33). La frase
 * compara el promedio por día jugado con [limiteDiarioMinutos]; sin límite definido, solo da el total.
 */
data class TiempoDeJuego(
    val minutosSemana: Int,
    val diasConJuego: Int,
    val limiteDiarioMinutos: Int?,
) {
    init {
        require(minutosSemana >= 0) { "Los minutos de juego no pueden ser negativos" }
        require(diasConJuego in 0..7) { "Los días con juego van de 0 a 7" }
        require(diasConJuego > 0 || minutosSemana == 0) { "Hay minutos de juego sin días con juego" }
        require(limiteDiarioMinutos == null || limiteDiarioMinutos > 0) { "El límite diario debe ser mayor que cero" }
    }
}

/**
 * Datos del correo de reporte semanal (UI-37), siempre de un solo niño o niña: el acudiente
 * solo ve los datos de su propio hijo (Ley 1581 de 2012).
 */
data class DatosCorreoReporteSemanal(
    /** Solo el primer nombre, como lo muestra el correo: «Salomé». */
    val nombreNino: String,
    val inicioSemana: LocalDate,
    val finSemana: LocalDate,
    val retosCompletados: Int,
    val retosPublicados: Int,
    val porcentajeAciertos: Int,
    val racha: Int,
    /** Sin mensaje, el correo omite el recuadro verde. */
    val mensajeInterpretativo: MensajeInterpretativo?,
    /** Porcentaje de aciertos por caneca. Una caneca sin datos esa semana no lleva fila. */
    val aciertosPorCaneca: Map<TipoCaneca, Int>,
    /** Nombres de las insignias ganadas en la semana, en orden. Vacía: se omite el recuadro. */
    val insigniasSemana: List<String>,
    val tiempoDeJuego: TiempoDeJuego,
    /** Abre el reporte semanal de ese hijo en la app: el botón «Abrir el reporte completo». */
    val enlaceReporte: String,
    val enlacesPie: EnlacesPieCorreo,
) {
    init {
        require(nombreNino.isNotBlank()) { "El reporte semanal necesita el nombre del niño" }
        require(!finSemana.isBefore(inicioSemana)) { "La semana termina antes de empezar" }
        require(retosPublicados >= 0) { "Los retos publicados no pueden ser negativos" }
        require(retosCompletados in 0..retosPublicados) { "Los retos completados deben estar entre 0 y los publicados" }
        require(porcentajeAciertos in 0..100) { "El porcentaje de aciertos va de 0 a 100" }
        require(racha >= 0) { "La racha no puede ser negativa" }
        require(aciertosPorCaneca.values.all { it in 0..100 }) { "Los aciertos por caneca van de 0 a 100" }
        require(insigniasSemana.none { it.isBlank() }) { "Las insignias de la semana necesitan nombre" }
    }
}
