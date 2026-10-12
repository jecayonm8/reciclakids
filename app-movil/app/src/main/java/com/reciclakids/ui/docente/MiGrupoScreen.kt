package com.reciclakids.ui.docente

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.reciclakids.R
import com.reciclakids.model.EstadoNinoHoy
import com.reciclakids.model.GrupoHoy
import com.reciclakids.model.NinoHoy
import com.reciclakids.ui.theme.ReciclaKidsTheme
import com.reciclakids.viewmodel.EstadoUi
import com.reciclakids.viewmodel.datosCargados

/** UI-25 Mi grupo: de un vistazo, quién ya jugó el reto de hoy. Cada fila abre el detalle del niño. */
@Composable
fun MiGrupoScreen(
    estado: EstadoUi<GrupoHoy>,
    onReintentar: () -> Unit,
    onNino: (ninoId: String) -> Unit,
    onIrACuenta: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val datos = estado.datosCargados
    MarcoDocente(
        modifier = modifier,
        barra = {
            BarraDocente(
                titulo = if (datos != null) {
                    stringResource(R.string.grupo_titulo, datos.nombreGrupo)
                } else {
                    stringResource(R.string.docente_tab_grupo)
                },
                acciones = {
                    if (datos != null) {
                        ChipEstado(
                            pluralStringResource(R.plurals.grupo_ninos, datos.ninos.size, datos.ninos.size),
                            fondo = MaterialTheme.colorScheme.secondaryContainer,
                            tinta = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(end = 16.dp),
                        )
                    }
                },
            )
        },
    ) { relleno ->
        when (estado) {
            EstadoUi.Cargando -> EsqueletoCarga(Modifier.padding(relleno))
            EstadoUi.Error -> EstadoErrorRed(onReintentar, Modifier.padding(relleno))
            is EstadoUi.Vacio -> EstadoVacioDocente(
                ilustracion = stringResource(R.string.grupo_vacio_ilustracion),
                titulo = stringResource(R.string.grupo_vacio_titulo),
                texto = stringResource(R.string.grupo_vacio_texto),
                modifier = Modifier.padding(relleno),
            ) {
                BotonLleno(stringResource(R.string.grupo_vacio_cuenta), onClick = onIrACuenta)
            }
            is EstadoUi.Contenido -> ListaGrupo(estado.datos, onNino, Modifier.padding(relleno))
        }
    }
}

@Composable
private fun ListaGrupo(grupo: GrupoHoy, onNino: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            grupo.retoHoy?.let { stringResource(R.string.grupo_reto_hoy, it) } ?: stringResource(R.string.grupo_sin_reto_hoy),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TarjetaDocente(Modifier.fillMaxWidth(), relleno = PaddingValues(0.dp), espacio = 0.dp) {
            grupo.ninos.forEachIndexed { i, nino ->
                FilaNino(nino, onClick = { onNino(nino.id) })
                if (i < grupo.ninos.lastIndex) HorizontalDivider(color = Color(0xFFEDF3F5))
            }
        }
    }
}

@Composable
private fun FilaNino(nino: NinoHoy, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AvatarNino()
        Column(Modifier.weight(1f)) {
            Text(nino.nombre, style = MaterialTheme.typography.bodyLarge)
            Text(
                nino.estado.detalle(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        ChipEstadoNino(nino.estado)
        Icon(
            painterResource(R.drawable.ic_siguiente),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ChipEstadoNino(estado: EstadoNinoHoy) {
    val esquema = MaterialTheme.colorScheme
    when (estado) {
        is EstadoNinoHoy.Completo -> ChipEstado(stringResource(R.string.nino_completo), esquema.tertiaryContainer, esquema.onTertiaryContainer)
        is EstadoNinoHoy.EnCurso -> ChipEstado(stringResource(R.string.nino_en_curso), esquema.secondaryContainer, esquema.onSecondaryContainer)
        EstadoNinoHoy.SinEmpezar -> ChipEstado(stringResource(R.string.nino_sin_empezar), esquema.surfaceContainer, esquema.onSurfaceVariant)
    }
}

@Composable
private fun EstadoNinoHoy.detalle(): String = when (this) {
    is EstadoNinoHoy.Completo -> stringResource(R.string.nino_completo_detalle, minutos, aciertos, total)
    is EstadoNinoHoy.EnCurso -> stringResource(R.string.nino_en_curso_detalle, residuoActual)
    EstadoNinoHoy.SinEmpezar -> stringResource(R.string.nino_sin_empezar_detalle)
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun MiGrupoPreview() {
    ReciclaKidsTheme {
        MiGrupoScreen(
            estado = EstadoUi.Contenido(
                GrupoHoy(
                    "Jardín B",
                    "Clasificar la lonchera",
                    listOf(
                        NinoHoy("1", "Salomé M.", EstadoNinoHoy.Completo(3, 8, 8)),
                        NinoHoy("2", "Ana L.", EstadoNinoHoy.EnCurso(4)),
                        NinoHoy("3", "Sara P.", EstadoNinoHoy.SinEmpezar),
                    ),
                )
            ),
            onReintentar = {}, onNino = {}, onIrACuenta = {},
        )
    }
}
