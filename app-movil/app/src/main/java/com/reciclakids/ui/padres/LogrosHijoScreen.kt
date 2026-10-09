package com.reciclakids.ui.padres

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reciclakids.R
import com.reciclakids.model.HijoVinculado
import com.reciclakids.model.Insignia
import com.reciclakids.model.LogroHijo
import com.reciclakids.model.LogrosHijo
import com.reciclakids.model.ProgresoNino
import com.reciclakids.model.faltante
import com.reciclakids.ui.docente.BarraDocente
import com.reciclakids.ui.docente.BordeOpcion
import com.reciclakids.ui.docente.ChipEstado
import com.reciclakids.ui.docente.EsqueletoCarga
import com.reciclakids.ui.docente.EstadoErrorRed
import com.reciclakids.ui.docente.MarcoDocente
import com.reciclakids.ui.theme.ReciclaKidsTheme
import com.reciclakids.viewmodel.EstadoUi
import com.reciclakids.viewmodel.datosCargados
import java.time.LocalDate

/**
 * UI-31 Logros e insignias: para compartir la alegría del logro en familia. Las pendientes van
 * con borde punteado y dicen cuánto falta cuando se puede contar; nunca se ven «perdidas».
 */
@Composable
fun LogrosHijoScreen(
    estado: EstadoUi<LogrosHijo>,
    onReintentar: () -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val datos = estado.datosCargados
    MarcoDocente(
        modifier = modifier,
        barra = {
            BarraDocente(
                titulo = datos?.let { stringResource(R.string.logros_titulo, it.hijo.nombre) } ?: stringResource(R.string.logros_titulo_corto),
                onAtras = onVolver,
                acciones = {
                    if (datos != null) {
                        ChipEstado(
                            stringResource(R.string.logros_conteo, datos.ganadas, datos.logros.size),
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
            EstadoUi.Error -> EstadoErrorRed(
                onReintentar = onReintentar,
                modifier = Modifier.padding(relleno),
                titulo = stringResource(R.string.padres_error_titulo),
                texto = stringResource(R.string.padres_error_texto),
            )
            is EstadoUi.Vacio -> ContenidoLogros(estado.datos, Modifier.padding(relleno))
            is EstadoUi.Contenido -> ContenidoLogros(estado.datos, Modifier.padding(relleno))
        }
    }
}

@Composable
private fun ContenidoLogros(datos: LogrosHijo, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Rejilla de dos columnas: ocho insignias caben sin desplazarse en 360 × 800.
        datos.logros.chunked(2).forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                fila.forEach { logro -> TarjetaLogro(logro, datos, Modifier.weight(1f)) }
                if (fila.size == 1) Column(Modifier.weight(1f)) { }
            }
        }
        Text(
            stringResource(R.string.logros_nota),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
    }
}

@Composable
private fun TarjetaLogro(logro: LogroHijo, datos: LogrosHijo, modifier: Modifier = Modifier) {
    val nota = notaLogro(logro, datos)
    val descripcion = stringResource(
        if (logro.ganada) R.string.logro_descripcion_ganada else R.string.logro_descripcion_pendiente,
        logro.insignia.nombre,
        nota,
    )
    Column(
        modifier = modifier
            .fondoLogro(ganada = logro.ganada, forma = FormaLogro)
            .then(if (logro.ganada) Modifier else Modifier.bordePunteado())
            .padding(horizontal = 10.dp, vertical = 14.dp)
            .clearAndSetSemantics { contentDescription = descripcion },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MedallaInsignia(ganada = logro.ganada, tamano = 64.dp)
        Text(
            logro.insignia.nombre,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp, lineHeight = 18.sp),
            color = if (logro.ganada) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            nota,
            style = MaterialTheme.typography.bodyMedium,
            color = if (logro.ganada) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** «Ganada el jueves», o lo que falta («Faltan 2 retos») o el requisito («Racha de 10»). */
@Composable
private fun notaLogro(logro: LogroHijo, datos: LogrosHijo): String {
    val fecha = logro.ganadaEl
    if (fecha != null) {
        return when (fecha) {
            datos.hoy -> stringResource(R.string.logro_ganada_hoy)
            datos.hoy.minusDays(1) -> stringResource(R.string.logro_ganada_ayer)
            else -> stringResource(R.string.logro_ganada_el, textoCuando(fecha, datos.hoy))
        }
    }
    val faltante = logro.insignia.faltante(ProgresoNino(datos.hijo.id, retosCompletados = datos.retosCompletados), datos.diasConReto)
    return when {
        faltante == null -> logro.insignia.requisito
        faltante.enDias -> pluralStringResource(R.plurals.coleccion_faltan_dias, faltante.cantidad, faltante.cantidad)
        else -> pluralStringResource(R.plurals.coleccion_faltan_retos, faltante.cantidad, faltante.cantidad)
    }
}

private val FormaLogro = RoundedCornerShape(16.dp)

/** Borde punteado de una insignia por descubrir: nunca tachada ni «apagada». */
private fun Modifier.bordePunteado(): Modifier = drawBehind {
    val grosor = 1.dp.toPx()
    drawRoundRect(
        color = BordeOpcion,
        topLeft = Offset(grosor / 2, grosor / 2),
        size = Size(size.width - grosor, size.height - grosor),
        cornerRadius = CornerRadius(16.dp.toPx() - grosor / 2),
        style = Stroke(grosor, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))),
    )
}

@Preview(widthDp = 360, heightDp = 900)
@Composable
private fun LogrosHijoPreview() {
    val hoy = LocalDate.of(2026, 9, 18)
    val salome = HijoVinculado("nino-1", "Salomé M.", "Jardín B", 5, "Pulpo azul", LocalDate.of(2026, 8, 21))
    val ganadas = mapOf(Insignia.AmigaTortuga to hoy.minusDays(1), Insignia.RachaDeCinco to hoy.minusDays(18))
    ReciclaKidsTheme {
        LogrosHijoScreen(
            estado = EstadoUi.Contenido(
                LogrosHijo(salome, hoy, Insignia.entries.map { LogroHijo(it, ganadas[it]) }, retosCompletados = 13, diasConReto = 13)
            ),
            onReintentar = {},
            onVolver = {},
        )
    }
}
