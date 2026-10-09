package com.reciclakids.ui.nino

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reciclakids.R
import com.reciclakids.model.Faltante
import com.reciclakids.model.Insignia
import com.reciclakids.model.ProgresoNino
import com.reciclakids.model.faltante
import com.reciclakids.ui.common.FondoSubmarino
import com.reciclakids.ui.common.TarjetaInsignia
import com.reciclakids.ui.common.sombraTitular
import com.reciclakids.ui.theme.BalooDos
import com.reciclakids.ui.theme.EscalaFijaNino
import com.reciclakids.ui.theme.ReciclaKidsColors
import com.reciclakids.ui.theme.ReciclaKidsTheme

private val AguaColeccion = Brush.verticalGradient(listOf(Color(0xFF5FB9CE), Color(0xFF0C5C73)))

/**
 * UI-17 Colección de insignias. Muestra lo logrado y lo que falta sin sensación de castigo:
 * las pendientes son siluetas punteadas que dicen cuánto falta.
 */
@Composable
fun ColeccionInsigniasScreen(
    ganadas: Set<Insignia>,
    progreso: ProgresoNino,
    diasConReto: Int,
    onTocar: (Insignia) -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EscalaFijaNino {
        FondoSubmarino(agua = AguaColeccion, altoArena = 50.dp, modifier = modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    BotonVolverNino(onVolver)
                    BasicText(
                        stringResource(R.string.coleccion_titulo),
                        modifier = Modifier.weight(1f).semantics { heading() },
                        style = TextStyle(
                            fontFamily = BalooDos,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            shadow = sombraTitular(3.dp),
                        ),
                        maxLines = 1,
                        // En una línea aunque el teléfono sea angosto.
                        autoSize = TextAutoSize.StepBased(minFontSize = 20.sp, maxFontSize = 30.sp),
                    )
                    Text(
                        stringResource(R.string.coleccion_contador, ganadas.size, Insignia.entries.size),
                        modifier = Modifier
                            .background(ReciclaKidsColors.panelNino, CircleShape)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        fontFamily = BalooDos,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = ReciclaKidsColors.tintaNino,
                    )
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Las ganadas primero, como en una vitrina.
                    items(Insignia.entries.sortedByDescending { it in ganadas }, key = { it.name }) { insignia ->
                        val ganada = insignia in ganadas
                        TarjetaInsignia(
                            nombre = insignia.nombre,
                            nota = if (ganada) insignia.requisito else notaPendiente(insignia, progreso, diasConReto),
                            ganada = ganada,
                            onClick = { onTocar(insignia) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun notaPendiente(insignia: Insignia, progreso: ProgresoNino, diasConReto: Int): String =
    when (val falta: Faltante? = insignia.faltante(progreso, diasConReto)) {
        null -> insignia.requisito
        else -> pluralStringResource(
            if (falta.enDias) R.plurals.coleccion_faltan_dias else R.plurals.coleccion_faltan_retos,
            falta.cantidad,
            falta.cantidad,
        )
    }

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun ColeccionInsigniasScreenPreview() {
    ReciclaKidsTheme {
        ColeccionInsigniasScreen(
            ganadas = setOf(Insignia.AmigaTortuga, Insignia.RapidoComoPez, Insignia.RachaDeCinco, Insignia.CincoRetosDiarios),
            progreso = ProgresoNino("n", retosCompletados = 8),
            diasConReto = 5,
            onTocar = {},
            onVolver = {},
        )
    }
}
