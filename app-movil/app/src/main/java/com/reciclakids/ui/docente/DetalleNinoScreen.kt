package com.reciclakids.ui.docente

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.reciclakids.R
import com.reciclakids.model.CategoriaResiduo
import com.reciclakids.model.Confusion
import com.reciclakids.model.DetalleNino
import com.reciclakids.model.InsigniaNino
import com.reciclakids.ui.common.EstiloMarcador
import com.reciclakids.ui.common.rayado
import com.reciclakids.ui.theme.ReciclaKidsTheme
import com.reciclakids.viewmodel.EstadoUi
import com.reciclakids.viewmodel.MensajeDetalleNino
import com.reciclakids.viewmodel.UmbralAciertosAlto
import com.reciclakids.viewmodel.cambioAciertos
import com.reciclakids.viewmodel.datosCargados
import com.reciclakids.viewmodel.mensaje

private val FondoAviso = Color(0xFFFFF6E8)
private val IconoAviso = Color(0xFF8A5A16)
private val TextoAviso = Color(0xFF4A3416)
private val FondoInsigniaGanada = Color(0xFFFFF8E9)
private val BordeInsigniaGanada = Color(0xFFE8C877)
private val FondoInsigniaPendiente = Color(0xFFF0F4F5)
private val FormaInsignia = RoundedCornerShape(14.dp)

/**
 * UI-27 Detalle de un niño, a pantalla completa: evolución, aciertos por caneca con una frase
 * que dice dónde se confunde, e insignias. Solo la docente y su acudiente ven esta información.
 */
@Composable
fun DetalleNinoScreen(
    estado: EstadoUi<DetalleNino?>,
    onVolver: () -> Unit,
    onReintentar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val detalle = estado.datosCargados
    MarcoDocente(
        modifier = modifier,
        pantallaCompleta = true,
        barra = {
            BarraDocente(
                titulo = detalle?.nombre.orEmpty(),
                onAtras = onVolver,
                inicioTitulo = { AvatarNino() },
            )
        },
    ) { relleno ->
        when (estado) {
            EstadoUi.Cargando -> EsqueletoCarga(Modifier.padding(relleno))
            EstadoUi.Error -> EstadoErrorRed(onReintentar, Modifier.padding(relleno))
            is EstadoUi.Vacio -> {
                val nino = estado.datos
                if (nino == null) {
                    EstadoErrorRed(onReintentar, Modifier.padding(relleno))
                } else {
                    EstadoVacioDocente(
                        ilustracion = null,
                        titulo = stringResource(R.string.detalle_vacio_titulo),
                        texto = stringResource(R.string.detalle_vacio_texto, nino.nombre),
                        modifier = Modifier.padding(relleno),
                    )
                }
            }
            is EstadoUi.Contenido -> estado.datos?.let { ContenidoDetalle(it, Modifier.padding(relleno)) }
        }
    }
}

@Composable
private fun ContenidoDetalle(detalle: DetalleNino, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                painterResource(R.drawable.ic_candado),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.detalle_privacidad),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TarjetaEvolucion(detalle)
        TarjetaPorCaneca(detalle)
        TarjetaInsignias(detalle.insignias)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TarjetaEvolucion(detalle: DetalleNino) {
    TarjetaDocente(Modifier.fillMaxWidth(), relleno = PaddingValues(20.dp), espacio = 16.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.detalle_evolucion), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.detalle_evolucion_sub),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
            }
            detalle.cambioAciertos?.let { cambio ->
                val semanas = detalle.aciertosPorSemana.size
                TextoCambio(
                    when {
                        cambio > 0 -> stringResource(R.string.detalle_cambio_sube, cambio, semanas)
                        cambio < 0 -> stringResource(R.string.detalle_cambio_baja, -cambio, semanas)
                        else -> stringResource(R.string.detalle_cambio_igual, semanas)
                    },
                    cambio,
                )
            }
        }
        GraficoBarras(
            valores = detalle.aciertosPorSemana.mapIndexed { i, valor -> stringResource(R.string.detalle_semana, i + 1) to valor },
            color = ColorEvolucion,
            altoMaximo = 150.dp,
            anchoMaximo = 60.dp,
        )
    }
}

@Composable
private fun TarjetaPorCaneca(detalle: DetalleNino) {
    TarjetaDocente(Modifier.fillMaxWidth(), relleno = PaddingValues(20.dp), espacio = 14.dp) {
        Column {
            Text(stringResource(R.string.detalle_por_caneca), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.detalle_por_caneca_sub),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        CategoriaResiduo.entries.forEach { categoria ->
            detalle.aciertosPorCategoria[categoria]?.let { valor ->
                FilaBarra(
                    etiqueta = categoria.nombre(),
                    porcentaje = valor,
                    color = if (valor >= UmbralAciertosAlto) MaterialTheme.colorScheme.tertiary else ColorValorBajo,
                    anchoEtiqueta = 120.dp,
                    inicio = { MuestraCaneca(categoria) },
                )
            }
        }
        detalle.mensaje()?.let { mensaje ->
            Row(
                Modifier.fillMaxWidth().background(FondoAviso, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    painterResource(R.drawable.ic_info),
                    contentDescription = null,
                    modifier = Modifier.padding(top = 2.dp).size(20.dp),
                    tint = IconoAviso,
                )
                Text(textoMensaje(mensaje), style = MaterialTheme.typography.bodyMedium, color = TextoAviso)
            }
        }
    }
}

@Composable
private fun textoMensaje(mensaje: MensajeDetalleNino): String = when (mensaje) {
    is MensajeDetalleNino.Confunde -> textoConfusion(mensaje.confusion)
    is MensajeDetalleNino.CanecaDificil -> stringResource(
        R.string.detalle_caneca_dificil,
        mensaje.categoria.colorCaneca(),
        textoPorcentaje(mensaje.aciertos),
    )
    MensajeDetalleNino.VaMuyBien -> stringResource(R.string.detalle_va_bien)
}

/** «Confunde el empaque metalizado con aprovechables. Un reto solo de caneca negra le ayudaría.» */
@Composable
private fun textoConfusion(confusion: Confusion): String = stringResource(
    R.string.detalle_confunde,
    confusion.residuo,
    confusion.elegida.nombreEnFrase(),
    confusion.correcta.colorCaneca(),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TarjetaInsignias(insignias: List<InsigniaNino>) {
    TarjetaDocente(Modifier.fillMaxWidth(), relleno = PaddingValues(20.dp), espacio = 14.dp) {
        Text(
            stringResource(R.string.detalle_insignias, insignias.count { it.ganada }, insignias.size),
            style = MaterialTheme.typography.titleMedium,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            insignias.forEach { InsigniaMini(it) }
        }
    }
}

@Composable
private fun InsigniaMini(insignia: InsigniaNino) {
    val descripcion = stringResource(
        if (insignia.ganada) R.string.detalle_insignia_ganada else R.string.detalle_insignia_pendiente,
        insignia.nombre,
    )
    val estiloMedalla = EstiloMarcador.Insignia
    Column(
        modifier = Modifier
            .width(120.dp)
            .then(
                if (insignia.ganada) {
                    Modifier.background(FondoInsigniaGanada, FormaInsignia).border(1.dp, BordeInsigniaGanada, FormaInsignia)
                } else {
                    Modifier.background(FondoInsigniaPendiente, FormaInsignia).bordePunteado(BordeOpcion)
                }
            )
            .padding(12.dp)
            .clearAndSetSemantics { contentDescription = descripcion },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(CircleShape)
                .then(
                    if (insignia.ganada) {
                        Modifier.rayado(estiloMedalla.claro, estiloMedalla.oscuro, paso = 8.dp)
                    } else {
                        Modifier.background(Color(0xFFE1E9EC))
                    }
                )
        )
        Text(
            insignia.nombre,
            style = MaterialTheme.typography.labelLarge,
            color = if (insignia.ganada) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Borde punteado de una insignia que todavía no se gana: nunca tachada ni «apagada». */
private fun Modifier.bordePunteado(color: Color): Modifier = drawBehind {
    val grosor = 1.dp.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(grosor / 2, grosor / 2),
        size = Size(size.width - grosor, size.height - grosor),
        cornerRadius = CornerRadius(14.dp.toPx() - grosor / 2),
        style = Stroke(grosor, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))),
    )
}

@Preview(widthDp = 360, heightDp = 1200)
@Composable
private fun DetalleNinoPreview() {
    ReciclaKidsTheme {
        DetalleNinoScreen(
            estado = EstadoUi.Contenido(
                DetalleNino(
                    id = "n02",
                    nombre = "Juan T.",
                    aciertosPorSemana = listOf(62, 71, 68, 74),
                    aciertosPorCategoria = mapOf(
                        CategoriaResiduo.Aprovechable to 94,
                        CategoriaResiduo.NoAprovechable to 61,
                        CategoriaResiduo.Organico to 88,
                    ),
                    confusion = Confusion("el empaque metalizado", CategoriaResiduo.NoAprovechable, CategoriaResiduo.Aprovechable),
                    insignias = listOf("Amiga tortuga", "Rápido como pez", "Racha de 5", "Pulpo ordenado").mapIndexed { i, n ->
                        InsigniaNino(n, ganada = i < 3)
                    },
                )
            ),
            onVolver = {},
            onReintentar = {},
        )
    }
}
