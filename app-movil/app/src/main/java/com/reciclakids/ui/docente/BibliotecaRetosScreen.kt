package com.reciclakids.ui.docente

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reciclakids.R
import com.reciclakids.model.BibliotecaDocente
import com.reciclakids.model.CategoriaResiduo
import com.reciclakids.model.Dificultad
import com.reciclakids.model.EstadoReto
import com.reciclakids.model.Reto
import com.reciclakids.ui.theme.ReciclaKidsTheme
import com.reciclakids.viewmodel.EstadoUi
import com.reciclakids.viewmodel.FiltroReto
import com.reciclakids.viewmodel.filtrados
import java.time.LocalDate

/** UI-23 Biblioteca de retos: encontrar, duplicar, ver resultados y editar los programados. */
@Composable
fun BibliotecaRetosScreen(
    estado: EstadoUi<BibliotecaDocente>,
    filtro: FiltroReto,
    onFiltro: (FiltroReto) -> Unit,
    onReintentar: () -> Unit,
    onCrear: () -> Unit,
    onDuplicar: (Reto) -> Unit,
    onVerResultados: (Reto) -> Unit,
    onEditar: (Reto) -> Unit,
    modifier: Modifier = Modifier,
) {
    MarcoDocente(
        modifier = modifier,
        barra = {
            BarraDocente(
                titulo = stringResource(R.string.biblioteca_titulo),
                acciones = { BotonLleno(stringResource(R.string.biblioteca_crear), onClick = onCrear, modifier = Modifier.padding(end = 12.dp)) },
            )
        },
    ) { relleno ->
        when (estado) {
            EstadoUi.Cargando -> EsqueletoCarga(Modifier.padding(relleno))
            EstadoUi.Error -> EstadoErrorRed(onReintentar, Modifier.padding(relleno))
            is EstadoUi.Vacio -> EstadoVacioDocente(
                ilustracion = stringResource(R.string.biblioteca_vacia_ilustracion),
                titulo = stringResource(R.string.biblioteca_vacia_titulo),
                texto = stringResource(R.string.biblioteca_vacia_texto),
                modifier = Modifier.padding(relleno),
            ) {
                BotonLleno(stringResource(R.string.biblioteca_vacia_crear), onClick = onCrear)
            }
            is EstadoUi.Contenido -> ListaRetos(
                biblioteca = estado.datos,
                filtro = filtro,
                onFiltro = onFiltro,
                onDuplicar = onDuplicar,
                onVerResultados = onVerResultados,
                onEditar = onEditar,
                modifier = Modifier.padding(relleno),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ListaRetos(
    biblioteca: BibliotecaDocente,
    filtro: FiltroReto,
    onFiltro: (FiltroReto) -> Unit,
    onDuplicar: (Reto) -> Unit,
    onVerResultados: (Reto) -> Unit,
    onEditar: (Reto) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visibles = biblioteca.retos.filtrados(filtro, biblioteca.hoy)
    Column(modifier.fillMaxSize()) {
        Column(
            Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FiltroReto.entries.forEach { opcion ->
                    ChipFiltro(opcion, seleccionado = opcion == filtro, onClick = { onFiltro(opcion) })
                }
            }
            Text(
                stringResource(R.string.biblioteca_conteo, visibles.size, biblioteca.retos.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(visibles, key = { it.id }) { reto ->
                TarjetaRetoBiblioteca(
                    reto = reto,
                    hoy = biblioteca.hoy,
                    onDuplicar = { onDuplicar(reto) },
                    onVerResultados = { onVerResultados(reto) },
                    onEditar = { onEditar(reto) },
                )
            }
            if (visibles.isEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            stringResource(R.string.biblioteca_sin_resultados),
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Normal),
                            textAlign = TextAlign.Center,
                        )
                        BotonContorno(stringResource(R.string.biblioteca_quitar_filtros), onClick = { onFiltro(FiltroReto.Todos) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ChipFiltro(filtro: FiltroReto, seleccionado: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = seleccionado,
        onClick = onClick,
        label = { Text(filtro.etiqueta(), style = MaterialTheme.typography.labelLarge) },
        modifier = Modifier.heightIn(min = 36.dp),
        shape = RoundedCornerShape(8.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
            labelColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = seleccionado,
            borderColor = MaterialTheme.colorScheme.outline,
            selectedBorderColor = Color.Transparent,
        ),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TarjetaRetoBiblioteca(
    reto: Reto,
    hoy: LocalDate,
    onDuplicar: () -> Unit,
    onVerResultados: () -> Unit,
    onEditar: () -> Unit,
) {
    TarjetaDocente(Modifier.fillMaxWidth(), relleno = PaddingValues(horizontal = 18.dp, vertical = 16.dp), espacio = 12.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    reto.nombre,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
                ChipEstadoReto(reto.estado, Modifier.align(Alignment.CenterVertically))
            }
            Text(detalleReto(reto, hoy), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BotonContorno(stringResource(R.string.biblioteca_duplicar), onClick = onDuplicar)
            BotonContorno(
                stringResource(R.string.biblioteca_resultados),
                onClick = onVerResultados,
                habilitado = reto.estado != EstadoReto.Borrador,
            )
            if (reto.editable) BotonTonal(stringResource(R.string.biblioteca_editar), onClick = onEditar)
        }
    }
}

@Composable
private fun FiltroReto.etiqueta(): String = stringResource(
    when (this) {
        FiltroReto.Todos -> R.string.filtro_todos
        FiltroReto.Aprovechables -> R.string.categoria_aprovechables
        FiltroReto.Organicos -> R.string.categoria_organicos
        FiltroReto.NoAprovechables -> R.string.categoria_no_aprovechables
        FiltroReto.Facil -> R.string.dificultad_facil
        FiltroReto.Medio -> R.string.dificultad_medio
        FiltroReto.EstaSemana -> R.string.filtro_esta_semana
    }
)

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun BibliotecaRetosPreview() {
    val hoy = LocalDate.of(2026, 9, 22)
    ReciclaKidsTheme {
        BibliotecaRetosScreen(
            estado = EstadoUi.Contenido(
                BibliotecaDocente(
                    hoy,
                    listOf(
                        Reto("r1", "Clasificar la lonchera", setOf(CategoriaResiduo.Aprovechables, CategoriaResiduo.Organicos), Dificultad.Medio, hoy, EstadoReto.Publicado, "4729"),
                        Reto("r2", "Después del refrigerio", setOf(CategoriaResiduo.Organicos), Dificultad.Facil, hoy.plusDays(1), EstadoReto.Programado),
                        Reto("r3", "Mezcla del día", CategoriaResiduo.entries.toSet(), Dificultad.Dificil, null, EstadoReto.Borrador),
                    ),
                )
            ),
            filtro = FiltroReto.Todos,
            onFiltro = {}, onReintentar = {}, onCrear = {}, onDuplicar = {}, onVerResultados = {}, onEditar = {},
        )
    }
}
