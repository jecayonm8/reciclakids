package com.reciclakids.ui.nino

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reciclakids.R
import com.reciclakids.model.Nino
import com.reciclakids.ui.common.EstiloMarcador
import com.reciclakids.ui.common.FondoSubmarino
import com.reciclakids.ui.common.MarcadorIlustracion
import com.reciclakids.ui.theme.BalooDos
import com.reciclakids.ui.theme.EscalaFijaNino
import com.reciclakids.ui.theme.ReciclaKidsColors
import com.reciclakids.ui.theme.ReciclaKidsTheme

internal val AguaJuegoClara = Brush.verticalGradient(listOf(Color(0xFF7FE0EE), Color(0xFF1A93B6)))

private val FormaTarjetaAvatar = RoundedCornerShape(24.dp)

/**
 * UI-09 ¿Quién eres? El niño se identifica por su avatar, sin usuario ni contraseña.
 * [ninos] es null mientras se cargan.
 */
@Composable
fun SeleccionAvatarScreen(
    ninos: List<Nino>?,
    elegido: String?,
    onElegir: (Nino) -> Unit,
    onRepetirVoz: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EscalaFijaNino {
        FondoSubmarino(agua = AguaJuegoClara, altoArena = 50.dp, modifier = modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.quien_eres_titulo),
                        modifier = Modifier
                            .weight(1f)
                            .background(ReciclaKidsColors.panelNino, CircleShape)
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                            .semantics { heading() },
                        fontFamily = BalooDos,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 34.sp,
                        lineHeight = 38.sp,
                        color = ReciclaKidsColors.tintaNino,
                    )
                    BotonRepetirVoz(onRepetirVoz)
                }
                if (ninos == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(ninos, key = { it.id }) { nino ->
                            TarjetaAvatar(nino, elegido = nino.id == elegido, onClick = { onElegir(nino) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaAvatar(nino: Nino, elegido: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(FormaTarjetaAvatar)
            .background(ReciclaKidsColors.panelNino)
            .border(4.dp, if (elegido) ReciclaKidsColors.insigniaBorde else Color.White.copy(alpha = 0.9f), FormaTarjetaAvatar)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
    ) {
        MarcadorIlustracion("avatar", Modifier.size(86.dp), estilo = EstiloMarcador.Avatar)
        Text(
            text = nino.nombre,
            fontFamily = BalooDos,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 24.sp,
            color = ReciclaKidsColors.tintaNino,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun SeleccionAvatarScreenPreview() {
    ReciclaKidsTheme {
        SeleccionAvatarScreen(
            ninos = listOf("Salomé M.", "Juan T.", "Ana L.", "Emilio R.").mapIndexed { i, n -> Nino("$i", n) },
            elegido = "0",
            onElegir = {},
            onRepetirVoz = {},
        )
    }
}
