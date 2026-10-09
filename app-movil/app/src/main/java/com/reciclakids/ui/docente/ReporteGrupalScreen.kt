package com.reciclakids.ui.docente

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reciclakids.R
import com.reciclakids.model.ReporteSemanal
import com.reciclakids.model.ValorDia
import com.reciclakids.model.ValorNino
import com.reciclakids.ui.theme.ReciclaKidsTheme
import com.reciclakids.util.nombreDiaCorto
import com.reciclakids.util.rangoFechas
import com.reciclakids.viewmodel.EstadoUi
import java.time.LocalDate

private val CifraKpi = TextStyle(fontSize = 36.sp, lineHeight = 40.sp)

/** UI-26 Reporte grupal semanal, comparado con la semana anterior, para decidir el foco de la siguiente. */
@Composable
fun ReporteGrupalScreen(
    estado: EstadoUi<ReporteSemanal>,
    onEnviar: () -> Unit,
    onReintentar: () -> Unit,
    onCrear: () -> Unit,
    modifier: Modifier = Modifier,
    enviando: Boolean = false,
    snackbar: SnackbarHostState? = null,
) {
    MarcoDocente(
        modifier = modifier,
        snackbar = snackbar,
        barra = {
            BarraDocente(
                titulo = stringResource(R.string.reporte_titulo),
                acciones = {
                    if (estado is EstadoUi.Contenido) {
                        val descripcion = stringResource(R.string.reporte_enviar_descripcion)
                        BotonContorno(
                            stringResource(R.string.reporte_enviar),
                            onClick = onEnviar,
                            habilitado = !enviando,
                            modifier = Modifier.padding(end = 12.dp).semantics { contentDescription = descripcion },
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
                ilustracion = null,
                titulo = stringResource(R.string.reporte_vacio_titulo),
                texto = stringResource(R.string.reporte_vacio_texto),
                modifier = Modifier.padding(relleno),
            ) {
                BotonLleno(stringResource(R.string.reporte_vacio_crear), onClick = onCrear)
            }
            is EstadoUi.Contenido -> ContenidoReporte(estado.datos, Modifier.padding(relleno))
        }
    }
}

@Composable
private fun ContenidoReporte(reporte: ReporteSemanal, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            stringResource(R.string.reporte_semana, rangoFechas(reporte.inicio, reporte.fin)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FilaKpi {
            TarjetaKpi(stringResource(R.string.reporte_completados), textoPorcentaje(reporte.completadosPct)) {
                TextoCambio(textoCambioPuntos(reporte.cambioCompletados), reporte.cambioCompletados)
            }
            TarjetaKpi(stringResource(R.string.reporte_aciertos), textoPorcentaje(reporte.aciertosPct)) {
                TextoCambio(textoCambioPuntos(reporte.cambioAciertos), reporte.cambioAciertos)
            }
        }
        FilaKpi {
            TarjetaKpi(stringResource(R.string.reporte_publicados), reporte.retosPublicados.toString()) {
                Text(
                    pluralStringResource(R.plurals.reporte_reutilizados, reporte.retosReutilizados, reporte.retosReutilizados),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TarjetaKpi(stringResource(R.string.reporte_insignias), reporte.insignias.toString()) {
                TextoCambio(textoCambioInsignias(reporte.cambioInsignias), reporte.cambioInsignias)
            }
        }
        TarjetaDocente(Modifier.fillMaxWidth(), relleno = PaddingValues(20.dp), espacio = 16.dp) {
            Text(stringResource(R.string.reporte_participacion), style = MaterialTheme.typography.titleMedium)
            GraficoBarras(
                valores = reporte.participacionPorDia.map { nombreDiaCorto(it.fecha) to it.porcentaje },
                color = MaterialTheme.colorScheme.primary,
            )
        }
        TarjetaDocente(Modifier.fillMaxWidth(), relleno = PaddingValues(20.dp), espacio = 12.dp) {
            Text(stringResource(R.string.reporte_por_nino), style = MaterialTheme.typography.titleMedium)
            reporte.completadosPorNino.forEach { nino ->
                FilaBarra(nino.nombre, nino.porcentaje, colorPorcentaje(nino.porcentaje))
            }
            Text(
                stringResource(R.string.reporte_privacidad),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Dos tarjetas de la misma altura. */
@Composable
private fun FilaKpi(contenido: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = contenido,
    )
}

@Composable
private fun RowScope.TarjetaKpi(titulo: String, valor: String, pie: @Composable () -> Unit) {
    TarjetaDocente(Modifier.weight(1f).fillMaxHeight(), relleno = PaddingValues(18.dp), espacio = 6.dp) {
        Text(titulo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = CifraKpi)
        pie()
    }
}

@Composable
private fun textoCambioPuntos(cambio: Int): String = when {
    cambio > 0 -> stringResource(R.string.reporte_cambio_sube, cambio)
    cambio < 0 -> stringResource(R.string.reporte_cambio_baja, -cambio)
    else -> stringResource(R.string.reporte_cambio_igual)
}

@Composable
private fun textoCambioInsignias(cambio: Int): String = when {
    cambio > 0 -> stringResource(R.string.reporte_cambio_insignias_sube, cambio)
    cambio < 0 -> stringResource(R.string.reporte_cambio_insignias_baja, -cambio)
    else -> stringResource(R.string.reporte_cambio_igual)
}

@Preview(widthDp = 360, heightDp = 1200)
@Composable
private fun ReporteGrupalPreview() {
    val inicio = LocalDate.of(2026, 9, 14)
    ReciclaKidsTheme {
        ReporteGrupalScreen(
            estado = EstadoUi.Contenido(
                ReporteSemanal(
                    inicio = inicio, fin = inicio.plusDays(4),
                    completadosPct = 86, cambioCompletados = 9, aciertosPct = 91, cambioAciertos = 4,
                    retosPublicados = 5, retosReutilizados = 2, insignias = 31, cambioInsignias = 0,
                    participacionPorDia = listOf(82, 91, 68, 95, 86).mapIndexed { i, v -> ValorDia(inicio.plusDays(i.toLong()), v) },
                    completadosPorNino = listOf(ValorNino("Salomé M.", 100), ValorNino("Juan T.", 80), ValorNino("Sara P.", 40)),
                )
            ),
            onEnviar = {}, onReintentar = {}, onCrear = {},
            snackbar = remember { SnackbarHostState() },
        )
    }
}
