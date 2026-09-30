package com.reciclakids.ui.theme

import androidx.annotation.FontRes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.reciclakids.R

// Baloo 2 y Nunito van empaquetadas como fuentes variables (eje wght) para que el
// Modo Niño se vea igual sin conexión. Licencia OFL en app-movil/licencias/.
@OptIn(ExperimentalTextApi::class)
private fun fuenteVariable(@FontRes id: Int, peso: FontWeight) = Font(
    resId = id,
    weight = peso,
    variationSettings = FontVariation.Settings(FontVariation.weight(peso.weight)),
)

val BalooDos = FontFamily(
    fuenteVariable(R.font.baloo2, FontWeight.Medium),
    fuenteVariable(R.font.baloo2, FontWeight.SemiBold),
    fuenteVariable(R.font.baloo2, FontWeight.Bold),
    fuenteVariable(R.font.baloo2, FontWeight.ExtraBold),
)

val Nunito = FontFamily(
    fuenteVariable(R.font.nunito, FontWeight.Normal),
    fuenteVariable(R.font.nunito, FontWeight.SemiBold),
    fuenteVariable(R.font.nunito, FontWeight.Bold),
    fuenteVariable(R.font.nunito, FontWeight.ExtraBold),
)

// Adultos: la escala del diseño coincide con la de Material 3 sobre Roboto (fuente del
// sistema), así que se usa tal cual. Mínimo 14 sp; labelSmall solo para unidades en gráficos.
val Typography = Typography()

// 1.4 Tipografía Modo Niño. Nada por debajo de `etiqueta` salvo donde el prototipo lo indique.
object TipografiaNino {
    val display = TextStyle(fontFamily = BalooDos, fontSize = 56.sp, lineHeight = 60.sp, fontWeight = FontWeight.ExtraBold)
    val titulo = TextStyle(fontFamily = BalooDos, fontSize = 40.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold)
    val boton = TextStyle(fontFamily = BalooDos, fontSize = 32.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold)
    val numero = TextStyle(fontFamily = BalooDos, fontSize = 72.sp, lineHeight = 72.sp, fontWeight = FontWeight.ExtraBold)
    val etiqueta = TextStyle(fontFamily = Nunito, fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold)
}
