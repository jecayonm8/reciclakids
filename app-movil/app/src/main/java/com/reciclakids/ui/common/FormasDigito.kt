package com.reciclakids.ui.common

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.reciclakids.ui.theme.FormaDigito

/** Rombo inscrito con 2 % de margen para no invadir las celdas vecinas del teclado. */
val FormaRombo: Shape = GenericShape { size, _ ->
    moveTo(size.width * 0.5f, size.height * 0.02f)
    lineTo(size.width * 0.98f, size.height * 0.5f)
    lineTo(size.width * 0.5f, size.height * 0.98f)
    lineTo(size.width * 0.02f, size.height * 0.5f)
    close()
}

/** Gota: tres esquinas elípticas al 50 % y la inferior izquierda casi en punta. */
class FormaGota(private val punta: Dp = 10.dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val redonda = CornerRadius(size.width / 2, size.height / 2)
        val aguda = with(density) { CornerRadius(punta.toPx()) }
        return Outline.Rounded(
            RoundRect(size.toRect(), topLeft = redonda, topRight = redonda, bottomRight = redonda, bottomLeft = aguda)
        )
    }
}

/** Óvalo: esquinas elípticas de 44 × 26 dp, reducidas en proporción si la figura es más chica. */
object FormaOvalo : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val (rx, ry) = with(density) { 44.dp.toPx() to 26.dp.toPx() }
        val factor = minOf(1f, size.width / (2 * rx), size.height / (2 * ry))
        return Outline.Rounded(RoundRect(size.toRect(), CornerRadius(rx * factor, ry * factor)))
    }
}

/**
 * Figura de un dígito del código. [radioCuadrado] y [puntaGota] cambian con el tamaño:
 * tecla 20 / 10 dp, casilla 18 / 12 dp, dígito gigante del docente 32 / 20 dp.
 */
fun FormaDigito.comoShape(radioCuadrado: Dp = 20.dp, puntaGota: Dp = 10.dp): Shape = when (this) {
    FormaDigito.Circulo -> CircleShape
    FormaDigito.Cuadrado -> RoundedCornerShape(radioCuadrado)
    FormaDigito.Rombo -> FormaRombo
    FormaDigito.Gota -> FormaGota(puntaGota)
    FormaDigito.Ovalo -> FormaOvalo
}
