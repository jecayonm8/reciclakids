package com.reciclakids.ui.juego

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.reciclakids.ui.theme.ReciclaKidsColors
import com.reciclakids.ui.theme.ReciclaKidsTheme

/** Cada caneca tiene su personaje: pez (guía y blanca), tortuga (verde) y pulpo (negra). */
enum class TipoPersonaje(val etiqueta: String) {
    Pez("pez"),
    Tortuga("tortuga"),
    Pulpo("pulpo"),
}

enum class AnimacionPersonaje(val etiqueta: String, val periodoMs: Int) {
    Flotar("flota", 4000),
    Celebrar("celebra", 4000),
    Animar("saluda", 4000),
    Dormir("duerme", 5000),
}

/**
 * Personaje marino. Mientras llega la ilustración definitiva muestra un marcador
 * a tamaño y posición reales.
 */
@Composable
fun PersonajeMarino(
    tipo: TipoPersonaje,
    modifier: Modifier = Modifier,
    animacion: AnimacionPersonaje = AnimacionPersonaje.Flotar,
    tamano: Dp = 150.dp,
) {
    MarcadorIlustracion(
        etiqueta = "${tipo.etiqueta}\n${animacion.etiqueta}",
        modifier = modifier
            .flotar(periodoMs = animacion.periodoMs)
            .size(tamano),
    )
}

@Preview
@Composable
private fun PersonajeMarinoPreview() {
    ReciclaKidsTheme {
        FondoSubmarino(agua = ReciclaKidsColors.aguaJuego) {
            PersonajeMarino(TipoPersonaje.Tortuga, animacion = AnimacionPersonaje.Animar)
        }
    }
}
