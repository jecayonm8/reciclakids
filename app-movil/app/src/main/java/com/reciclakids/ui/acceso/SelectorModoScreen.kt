package com.reciclakids.ui.acceso

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reciclakids.R
import com.reciclakids.ui.juego.Alga
import com.reciclakids.ui.juego.AnimacionPersonaje
import com.reciclakids.ui.juego.BotonNino
import com.reciclakids.ui.juego.EstadoConexion
import com.reciclakids.ui.juego.FondoSubmarino
import com.reciclakids.ui.juego.IndicadorSinConexion
import com.reciclakids.ui.juego.PersonajeMarino
import com.reciclakids.ui.juego.TipoPersonaje
import com.reciclakids.ui.theme.EscalaFijaNino
import com.reciclakids.ui.theme.ReciclaKidsTheme
import com.reciclakids.ui.theme.TipografiaNino

private val AguaSelector = Brush.verticalGradient(
    0f to Color(0xFF6FD8EA),
    0.6f to Color(0xFF1E9CC0),
    1f to Color(0xFF0C5C73),
)

/**
 * UI-02 Selector de modo. Un solo toque para jugar; el acceso de adultos existe pero no
 * llama la atención y queda detrás de la puerta para adultos.
 */
@Composable
fun SelectorModoScreen(
    onJugar: () -> Unit,
    onPuertaAbierta: () -> Unit,
    modifier: Modifier = Modifier,
    conexion: EstadoConexion = EstadoConexion.Conectado,
) {
    var puertaVisible by rememberSaveable { mutableStateOf(false) }

    EscalaFijaNino {
        FondoSubmarino(
            agua = AguaSelector,
            altoArena = 90.dp,
            modifier = modifier.fillMaxSize(),
            decoracion = {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    Alga(120.dp, Modifier.offset(maxWidth * 0.06f, maxHeight - 180.dp))
                    Alga(90.dp, Modifier.offset(maxWidth * 0.92f - 18.dp, maxHeight - 150.dp), ancho = 18.dp)
                }
            },
        ) {
            Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterVertically),
                ) {
                    PersonajeMarino(TipoPersonaje.Tortuga, animacion = AnimacionPersonaje.Animar, tamano = 170.dp)
                    BotonNino(
                        texto = stringResource(R.string.selector_jugar),
                        onClick = onJugar,
                        modifier = Modifier.fillMaxWidth(),
                        icono = painterResource(R.drawable.ic_play),
                        alto = 130.dp,
                        forma = RoundedCornerShape(44.dp),
                        profundidad = 10.dp,
                        estiloTexto = TipografiaNino.boton.copy(fontSize = 44.sp, lineHeight = 48.sp, fontWeight = FontWeight.ExtraBold),
                        tamanoIcono = 56.dp,
                    )
                }
                IndicadorSinConexion(conexion, Modifier.align(Alignment.TopEnd).padding(16.dp))
                BotonAdultos(onClick = { puertaVisible = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(18.dp))
            }
        }
    }

    // La hoja va fuera de EscalaFijaNino: es interfaz de adultos y respeta la escala de fuente.
    if (puertaVisible) {
        PuertaAdultosSheet(
            onAbierta = {
                puertaVisible = false
                onPuertaAbierta()
            },
            onCerrar = { puertaVisible = false },
        )
    }
}

@Composable
private fun BotonAdultos(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val forma = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(forma)
            .background(Color.White.copy(alpha = 0.72f))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painterResource(R.drawable.ic_candado),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            stringResource(R.string.selector_adultos),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun SelectorModoScreenPreview() {
    ReciclaKidsTheme { SelectorModoScreen(onJugar = {}, onPuertaAbierta = {}) }
}
