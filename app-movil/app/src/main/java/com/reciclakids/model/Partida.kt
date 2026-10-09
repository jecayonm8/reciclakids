package com.reciclakids.model

/** Aciertos seguidos desde los que cada acierto vale el doble (+20 en vez de +10). */
const val RachaConBonificacion = 3

const val PuntosPorAcierto = 10
const val PuntosPorAciertoEnRacha = 20

/** Tiempo del temporizador visual en dificultad difícil. Al acabarse el juego sigue igual. */
const val LimiteDificilMs = 90_000L

/**
 * Estado de una partida: el niño clasifica los residuos del reto en orden. Es el componente de
 * evaluación del lado de la app: decide al instante si cada intento es correcto, sin red.
 *
 * Un error nunca resta puntos ni vidas y deja la racha quieta: no la borra.
 */
data class EstadoPartida(
    val residuos: List<Residuo>,
    val indice: Int = 0,
    val aciertos: Int = 0,
    val errores: Int = 0,
    /** Intentos fallidos con el residuo que está en pantalla; desde 2 la caneca correcta brilla sola. */
    val erroresResiduoActual: Int = 0,
    val racha: Int = 0,
    val rachaMaxima: Int = 0,
    val puntaje: Int = 0,
) {
    val residuoActual: Residuo? get() = residuos.getOrNull(indice)
    val terminada: Boolean get() = indice >= residuos.size

    /** Evalúa el intento de echar el residuo actual en la caneca de [categoria]. */
    fun intentar(categoria: CategoriaResiduo): Intento {
        val residuo = checkNotNull(residuoActual) { "La partida ya terminó" }
        if (residuo.categoria != categoria) {
            return Intento(
                residuo = residuo,
                correcto = false,
                estado = copy(errores = errores + 1, erroresResiduoActual = erroresResiduoActual + 1),
            )
        }
        val nuevaRacha = racha + 1
        val puntos = if (nuevaRacha >= RachaConBonificacion) PuntosPorAciertoEnRacha else PuntosPorAcierto
        return Intento(
            residuo = residuo,
            correcto = true,
            estado = copy(
                indice = indice + 1,
                aciertos = aciertos + 1,
                erroresResiduoActual = 0,
                racha = nuevaRacha,
                rachaMaxima = maxOf(rachaMaxima, nuevaRacha),
                puntaje = puntaje + puntos,
            ),
        )
    }
}

data class Intento(val residuo: Residuo, val correcto: Boolean, val estado: EstadoPartida)

/** Lo que se le muestra al niño al terminar (UI-15). Todo reto termina en logro. */
data class ResultadoReto(
    val residuosSeparados: Int,
    val errores: Int,
    val rachaMaxima: Int,
    /** El reto del día cuenta para el acuario solo la primera vez que se completa. */
    val sumoAlAcuario: Boolean,
    val insigniasNuevas: List<Insignia>,
) {
    /** Siempre al menos una estrella: 3 sin errores, 2 con pocos, 1 con más. */
    val estrellas: Int
        get() = when {
            errores == 0 -> 3
            errores <= 2 -> 2
            else -> 1
        }
}
