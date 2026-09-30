package com.reciclakids.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.reciclakids.ui.theme.Elevacion
import com.reciclakids.ui.theme.ReciclaKidsColors
import com.reciclakids.ui.theme.ReciclaKidsTheme
import com.reciclakids.ui.theme.ResorteRebote

enum class EstadoResiduo {
    /** Flota en su sitio esperando a que lo tomen. */
    Reposo,

    /** Sigue al dedo: 1,12× y sombra más larga. */
    Arrastrando,

    /** Respuesta a una caneca equivocada: rebota suave y vuelve. Sin rojo ni cruces. */
    Rebote,
}

private val FormaResiduo = RoundedCornerShape(28.dp)

/**
 * Residuo que el niño arrastra a una caneca. El desplazamiento del arrastre lo aplica la
 * pantalla con [modifier]; aquí solo vive la apariencia de cada estado.
 * Con [enIman] el borde toma el color del halo: si lo suelta, entra en la caneca.
 */
@Composable
fun ObjetoResiduo(
    nombre: String,
    modifier: Modifier = Modifier,
    estado: EstadoResiduo = EstadoResiduo.Reposo,
    enIman: Boolean = false,
    ilustracion: Painter? = null,
    tamano: Dp = 132.dp,
) {
    val arrastrando = estado == EstadoResiduo.Arrastrando
    val escalaArrastre by animateFloatAsState(if (arrastrando) 1.12f else 1f, ResorteRebote, label = "escalaResiduo")
    val elevacion by animateDpAsState(if (arrastrando) Elevacion.nivel5 else Elevacion.nivel3, label = "elevacionResiduo")
    val rebote = remember { Animatable(1f) }
    LaunchedEffect(estado) { if (estado == EstadoResiduo.Rebote) rebote.rebotar(500) }

    Box(
        modifier = modifier
            .then(if (estado == EstadoResiduo.Reposo) Modifier.flotar(periodoMs = 3000) else Modifier)
            .graphicsLayer {
                val escala = escalaArrastre * rebote.value
                scaleX = escala
                scaleY = escala
            }
            .size(tamano)
            .shadow(elevacion, FormaResiduo)
            .background(Color.White, FormaResiduo)
            .border(4.dp, if (enIman) ReciclaKidsColors.halo else Color.White, FormaResiduo)
            .semantics { contentDescription = nombre },
        contentAlignment = Alignment.Center,
    ) {
        if (ilustracion != null) {
            Image(ilustracion, contentDescription = null, modifier = Modifier.padding(12.dp))
        } else {
            MarcadorIlustracion(nombre, Modifier.matchParentSize(), FormaResiduo, EstiloMarcador.Residuo)
        }
    }
}

@Preview
@Composable
private fun ObjetoResiduoPreview() {
    ReciclaKidsTheme {
        FondoSubmarino(agua = ReciclaKidsColors.aguaJuego) {
            ObjetoResiduo("botella\nplástica", Modifier.padding(32.dp), enIman = true)
        }
    }
}
