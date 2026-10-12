package com.reciclakids.ui.padres

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reciclakids.R
import com.reciclakids.model.CategoriaResiduo
import com.reciclakids.model.HijoVinculado
import com.reciclakids.model.LogroHijo
import com.reciclakids.model.ReporteHijo
import com.reciclakids.model.SemanaHijo
import com.reciclakids.model.UmbralAciertosPadres
import com.reciclakids.model.inicioSemana
import com.reciclakids.ui.docente.BarraDocente
import com.reciclakids.ui.docente.BotonTexto
import com.reciclakids.ui.docente.ColorEvolucion
import com.reciclakids.ui.docente.ColorValorBajo
import com.reciclakids.ui.docente.EsqueletoCarga
import com.reciclakids.ui.docente.EstadoErrorRed
import com.reciclakids.ui.docente.EstadoVacioDocente
import com.reciclakids.ui.docente.FilaBarra
import com.reciclakids.ui.docente.GraficoBarras
import com.reciclakids.ui.docente.MarcoDocente
import com.reciclakids.ui.docente.MuestraCaneca
import com.reciclakids.ui.docente.TarjetaDocente
import com.reciclakids.ui.docente.TextoCambio
import com.reciclakids.ui.docente.nombre
import com.reciclakids.ui.theme.Duracion
import com.reciclakids.ui.theme.ReciclaKidsTheme
import com.reciclakids.util.duracionMinutos
import com.reciclakids.util.nombreDiaCorto
import com.reciclakids.util.rangoCorto
import com.reciclakids.util.rangoFechas
import com.reciclakids.viewmodel.EstadoUi
import com.reciclakids.viewmodel.datosCargados
import java.time.LocalDate

/**
 * UI-30 Reporte semanal. Explica el avance en lenguaje claro, no en métricas: evolución, aciertos
 * por tipo de residuo con una frase interpretativa, tiempo de uso frente al límite e insignias.
 * La tira de semanas se desliza en horizontal.
 */
@Composable
fun ReporteSemanalScreen(
    estado: EstadoUi<ReporteHijo>,
    cambiandoSemana: Boolean,
    onSemana: (LocalDate) -> Unit,
    onReintentar: () -> Unit,
    onVolver: () -> Unit,
    onVerLogros: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MarcoDocente(
        modifier = modifier,
        barra = {
            BarraDocente(
                titulo = stringResource(R.string.reporte_padres_titulo),
                subtitulo = estado.datosCargados?.hijo?.nombre,
                onAtras = onVolver,
            )
        },
    ) { relleno ->
        when (estado) {
            EstadoUi.Cargando -> EsqueletoCarga(Modifier.padding(relleno))
            EstadoUi.Error -> EstadoErrorRed(
                onReintentar = onReintentar,
                modifier = Modifier.padding(relleno),
                titulo = stringResource(R.string.padres_error_titulo),
                texto = stringResource(R.string.padres_error_texto),
            )
            is EstadoUi.Vacio -> EstadoVacioDocente(
                ilustracion = null,
                titulo = stringResource(R.string.reporte_padres_vacio_titulo),
                texto = stringResource(R.string.reporte_padres_vacio_texto),
                modifier = Modifier.padding(relleno),
            )
            is EstadoUi.Contenido -> ContenidoReporte(estado.datos, cambiandoSemana, onSemana, onVerLogros, relleno)
        }
    }
}

@Composable
private fun ContenidoReporte(
    reporte: ReporteHijo,
    cambiandoSemana: Boolean,
    onSemana: (LocalDate) -> Unit,
    onVerLogros: () -> Unit,
    relleno: PaddingValues,
) {
    Column(Modifier.fillMaxSize().padding(relleno)) {
        TiraSemanas(reporte, onSemana)
        Box(Modifier.fillMaxWidth().height(4.dp)) {
            if (cambiandoSemana) {
                val descripcion = stringResource(R.string.reporte_cargando_semana)
                LinearProgressIndicator(Modifier.fillMaxWidth().semantics { contentDescription = descripcion })
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            TarjetaEvolucion(reporte)
            TarjetaPorTipo(reporte.semana, reporte)
            TarjetaTiempo(reporte.semana, reporte.hoy, reporte.limiteMinutos)
            TarjetaInsigniasSemana(reporte.semana.insignias, onVerLogros)
        }
    }
}

@Composable
private fun TiraSemanas(reporte: ReporteHijo, onSemana: (LocalDate) -> Unit) {
    val actual = inicioSemana(reporte.hoy)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        reporte.semanas.forEach { lunes ->
            val etiqueta = if (lunes == actual) stringResource(R.string.reporte_esta_semana) else rangoCorto(lunes, lunes.plusDays(4))
            val descripcion = stringResource(R.string.reporte_semana, rangoFechas(lunes, lunes.plusDays(4)))
            FilterChip(
                selected = lunes == reporte.semana.inicio,
                onClick = { onSemana(lunes) },
                label = { Text(etiqueta, style = MaterialTheme.typography.labelLarge) },
                modifier = Modifier.semantics { contentDescription = descripcion },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
            )
        }
    }
}

@Composable
private fun TarjetaEvolucion(reporte: ReporteHijo) {
    TarjetaDocente(Modifier.fillMaxWidth(), espacio = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.reporte_evolucion), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            reporte.cambio?.let { cambio ->
                val texto = when {
                    cambio > 0 -> stringResource(R.string.reporte_pts_sube, cambio)
                    cambio < 0 -> stringResource(R.string.reporte_pts_baja, -cambio)
                    else -> stringResource(R.string.reporte_pts_igual)
                }
                TextoCambio(texto, cambio)
            }
        }
        val descripcion = stringResource(R.string.reporte_evolucion_descripcion)
        val tercero = MaterialTheme.colorScheme.tertiary
        GraficoBarras(
            valores = reporte.evolucion.mapIndexed { i, (_, aciertos) -> stringResource(R.string.detalle_semana, i + 1) to aciertos },
            color = ColorEvolucion,
            colorPorValor = { if (it >= UmbralAciertosPadres) tercero else ColorEvolucion },
            altoMaximo = 120.dp,
            anchoMaximo = 44.dp,
            modifier = Modifier.semantics { contentDescription = descripcion },
        )
    }
}

@Composable
private fun TarjetaPorTipo(semana: SemanaHijo, reporte: ReporteHijo) {
    TarjetaDocente(Modifier.fillMaxWidth(), espacio = 14.dp) {
        Text(stringResource(R.string.reporte_por_tipo), style = MaterialTheme.typography.titleMedium)
        val porCategoria = semana.porCategoria
        if (porCategoria.isEmpty()) {
            Text(stringResource(R.string.reporte_no_jugo), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            val tercero = MaterialTheme.colorScheme.tertiary
            OrdenCanecas.forEach { categoria ->
                porCategoria[categoria]?.porcentaje?.let { valor ->
                    FilaBarra(
                        etiqueta = categoria.nombre(),
                        porcentaje = valor,
                        color = if (valor >= UmbralAciertosPadres) tercero else ColorValorBajo,
                        anchoEtiqueta = 124.dp,
                        inicio = { MuestraCaneca(categoria) },
                    )
                }
            }
            reporte.mensaje?.let { TarjetaMensaje(textoMensaje(it, semana.confusionFrecuente), interna = true) }
        }
    }
}

/** El orden de las canecas en los gráficos, como en los prototipos: blanca, verde, negra. */
private val OrdenCanecas = listOf(CategoriaResiduo.Aprovechable, CategoriaResiduo.Organico, CategoriaResiduo.NoAprovechable)

@Composable
private fun TarjetaTiempo(semana: SemanaHijo, hoy: LocalDate, limite: Int) {
    TarjetaDocente(Modifier.fillMaxWidth(), espacio = 12.dp) {
        Text(stringResource(R.string.reporte_tiempo), style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(duracionMinutos(semana.minutos), style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp), modifier = Modifier.alignByBaseline())
            Text(
                stringResource(R.string.reporte_tiempo_semana),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alignByBaseline(),
            )
        }
        GraficoMinutos(semana.minutosPorDia(hoy), limite)
        val promedio = semana.minutosPromedio
        Text(
            stringResource(
                when {
                    promedio < limite -> R.string.reporte_tiempo_debajo
                    promedio > limite -> R.string.reporte_tiempo_encima
                    else -> R.string.reporte_tiempo_igual
                },
                promedio,
                limite,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Minutos por día; la escala es el límite diario, así una barra llena es un día en el límite. */
@Composable
private fun GraficoMinutos(dias: List<Pair<LocalDate, Int>>, limite: Int, alto: Dp = 72.dp) {
    val escala = maxOf(limite, dias.maxOfOrNull { it.second } ?: 0, 1)
    val primario = MaterialTheme.colorScheme.primary
    val vacio = MaterialTheme.colorScheme.surfaceVariant
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        dias.forEach { (fecha, minutos) ->
            val crecimiento = remember { Animatable(0f) }
            LaunchedEffect(minutos) { crecimiento.animateTo(1f, tween(Duracion.enfatica)) }
            val descripcion = stringResource(R.string.reporte_tiempo_dia_descripcion, nombreDiaCorto(fecha), minutos)
            Column(
                modifier = Modifier.weight(1f).clearAndSetSemantics { contentDescription = descripcion },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    Modifier
                        .widthIn(max = 26.dp)
                        .fillMaxWidth()
                        .height(if (minutos == 0) 4.dp else alto * (minutos / escala.toFloat()))
                        .graphicsLayer {
                            scaleY = crecimiento.value
                            transformOrigin = TransformOrigin(0.5f, 1f)
                        }
                        .background(if (minutos == 0) vacio else primario, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
                )
                Text(nombreDiaCorto(fecha), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TarjetaInsigniasSemana(insignias: List<LogroHijo>, onVerLogros: () -> Unit) {
    TarjetaDocente(Modifier.fillMaxWidth(), espacio = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.reporte_insignias_semana), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            BotonTexto(stringResource(R.string.reporte_ver_todas), onClick = onVerLogros)
        }
        if (insignias.isEmpty()) {
            Text(stringResource(R.string.reporte_sin_insignias), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                insignias.forEach { logro ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fondoLogro(ganada = true)
                            .padding(12.dp)
                            .semantics(mergeDescendants = true) { },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        MedallaInsignia(ganada = true, tamano = 52.dp)
                        Text(logro.insignia.nombre, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Preview(widthDp = 360, heightDp = 1300)
@Composable
private fun ReporteSemanalPreview() {
    val hoy = LocalDate.of(2026, 9, 18)
    val lunes = inicioSemana(hoy)
    val salome = HijoVinculado("nino-1", "Salomé M.", "Jardín B", 5, "Pulpo azul", LocalDate.of(2026, 8, 21))
    ReciclaKidsTheme {
        ReporteSemanalScreen(
            estado = EstadoUi.Contenido(
                ReporteHijo(
                    hijo = salome,
                    hoy = hoy,
                    semanas = (0L..3L).map { lunes.minusWeeks(it) },
                    semana = SemanaHijo(lunes, 5, emptyList(), emptyList()),
                    anterior = null,
                    evolucion = listOf(lunes.minusWeeks(3) to 61, lunes.minusWeeks(2) to 78, lunes.minusWeeks(1) to 72, lunes to 83),
                    limiteMinutos = 20,
                )
            ),
            cambiandoSemana = false,
            onSemana = {}, onReintentar = {}, onVolver = {}, onVerLogros = {},
        )
    }
}
