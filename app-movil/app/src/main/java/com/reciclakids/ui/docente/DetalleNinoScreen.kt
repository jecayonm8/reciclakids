package com.reciclakids.ui.docente

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.reciclakids.model.Insignia
import com.reciclakids.model.InsigniaNino
import com.reciclakids.ui.common.EstiloMarcador
import com.reciclakids.ui.common.IlustracionInsignia
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
private val AnchoMinimoInsignia = 100.dp
private val EspacioInsignias = 14.dp

/**
 * UI-27 Detalle de un niño, a pantalla completa: evolución, aciertos por caneca con una frase
 * que dice dónde se confunde, e insignias. Solo la docente y su acudiente ven esta información.
 * Al final, el código de vinculación para su acudiente y la opción de quitarlo del grupo.
 */
@Composable
fun DetalleNinoScreen(
    estado: EstadoUi<DetalleNino?>,
    onVolver: () -> Unit,
    onReintentar: () -> Unit,
    onCompartirCodigo: (DetalleNino) -> Unit,
    onRegenerarCodigo: () -> Unit,
    onQuitar: () -> Unit,
    modifier: Modifier = Modifier,
    quitando: Boolean = false,
    snackbar: SnackbarHostState? = null,
) {
    val detalle = estado.datosCargados
    val acciones = AccionesNino(onCompartirCodigo, onRegenerarCodigo, onQuitar, quitando)
    MarcoDocente(
        modifier = modifier,
        pantallaCompleta = true,
        snackbar = snackbar,
        barra = {
            BarraDocente(
                titulo = detalle?.nombre.orEmpty(),
                onAtras = onVolver,
                inicioTitulo = { AvatarNino(nombre = detalle?.nombre) },
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
                    ContenidoDetalle(nino, acciones, Modifier.padding(relleno))
                }
            }
            is EstadoUi.Contenido -> estado.datos?.let { ContenidoDetalle(it, acciones, Modifier.padding(relleno)) }
        }
    }
}

/** Lo que la docente puede hacer con un niño desde su detalle. */
private class AccionesNino(
    val onCompartirCodigo: (DetalleNino) -> Unit,
    val onRegenerarCodigo: () -> Unit,
    val onQuitar: () -> Unit,
    val quitando: Boolean,
)

@Composable
private fun ContenidoDetalle(detalle: DetalleNino, acciones: AccionesNino, modifier: Modifier = Modifier) {
    // Toda acción sensible pide confirmación antes de llegar al servicio.
    var confirmarRegenerar by rememberSaveable { mutableStateOf(false) }
    var confirmarQuitar by rememberSaveable { mutableStateOf(false) }
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
        if (detalle.sinResultados) {
            TarjetaSinResultados(detalle.nombre)
        } else {
            TarjetaEvolucion(detalle)
            TarjetaPorCaneca(detalle)
            TarjetaInsignias(detalle.insignias)
        }
        TarjetaVinculacionNino(
            detalle,
            onCompartir = { acciones.onCompartirCodigo(detalle) },
            onRegenerar = { confirmarRegenerar = true },
        )
        TarjetaQuitar(acciones.quitando, onQuitar = { confirmarQuitar = true })
    }
    if (confirmarRegenerar) {
        DialogoConfirmacion(
            titulo = stringResource(R.string.vinculacion_regenerar_titulo, detalle.nombre),
            texto = stringResource(R.string.vinculacion_regenerar_texto),
            confirmar = stringResource(R.string.vinculacion_regenerar_confirmar),
            destructivo = false,
            onConfirmar = {
                confirmarRegenerar = false
                acciones.onRegenerarCodigo()
            },
            onCancelar = { confirmarRegenerar = false },
        )
    }
    if (confirmarQuitar) {
        DialogoConfirmacion(
            titulo = stringResource(R.string.quitar_nino_titulo, detalle.nombre),
            texto = stringResource(R.string.quitar_nino_dialogo),
            confirmar = stringResource(R.string.quitar_nino_confirmar),
            destructivo = true,
            onConfirmar = {
                confirmarQuitar = false
                acciones.onQuitar()
            },
            onCancelar = { confirmarQuitar = false },
        )
    }
}

/** Un niño recién agregado todavía no ha jugado: lo primero es compartir su código. */
@Composable
private fun TarjetaSinResultados(nombre: String) {
    TarjetaDocente(Modifier.fillMaxWidth(), relleno = PaddingValues(20.dp), espacio = 8.dp) {
        Text(stringResource(R.string.detalle_vacio_titulo), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.detalle_vacio_texto, nombre),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TarjetaVinculacionNino(detalle: DetalleNino, onCompartir: () -> Unit, onRegenerar: () -> Unit) {
    val esquema = MaterialTheme.colorScheme
    TarjetaDocente(Modifier.fillMaxWidth(), relleno = PaddingValues(20.dp), espacio = 12.dp) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.vinculacion_titulo),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
            // Pendiente es ámbar, como todo lo que está en espera: nunca un error.
            if (detalle.acudienteVinculado) {
                ChipEstado(stringResource(R.string.vinculacion_vinculado), esquema.tertiaryContainer, esquema.onTertiaryContainer)
            } else {
                ChipEstado(stringResource(R.string.vinculacion_sin_vincular), FondoAviso, TextoAviso)
            }
        }
        Text(
            if (detalle.acudienteVinculado) {
                stringResource(R.string.vinculacion_vinculado_texto)
            } else {
                stringResource(R.string.vinculacion_sin_vincular_texto, detalle.nombre)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = esquema.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TextoCodigoVinculacion(detalle.codigoVinculacion, Modifier.align(Alignment.CenterVertically))
            Row(Modifier.align(Alignment.CenterVertically), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                BotonContorno(stringResource(R.string.vinculacion_compartir), onClick = onCompartir)
                BotonTexto(stringResource(R.string.vinculacion_regenerar), onClick = onRegenerar)
            }
        }
    }
}

/** Acción destructiva: el único rojo de esta pantalla. */
@Composable
private fun TarjetaQuitar(quitando: Boolean, onQuitar: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FormaTarjeta,
        color = Color.White,
        border = BorderStroke(1.dp, BordeOpcion),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(R.string.quitar_nino_texto),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FilledTonalButton(
                onClick = onQuitar,
                enabled = !quitando,
                modifier = Modifier.heightIn(min = AltoMinimoBoton),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(stringResource(R.string.quitar_nino), style = MaterialTheme.typography.labelLarge)
            }
        }
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

@Composable
private fun TarjetaInsignias(insignias: List<InsigniaNino>) {
    TarjetaDocente(Modifier.fillMaxWidth(), relleno = PaddingValues(20.dp), espacio = 14.dp) {
        Text(
            stringResource(R.string.detalle_insignias, insignias.count { it.ganada }, insignias.size),
            style = MaterialTheme.typography.titleMedium,
        )
        // Columnas de igual ancho que llenan la tarjeta; en cada fila todas las insignias miden
        // lo que la más alta, aunque un nombre ocupe dos líneas.
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val columnas = ((maxWidth + EspacioInsignias) / (AnchoMinimoInsignia + EspacioInsignias)).toInt().coerceAtLeast(2)
            Column(verticalArrangement = Arrangement.spacedBy(EspacioInsignias)) {
                insignias.chunked(columnas).forEach { fila ->
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(EspacioInsignias)) {
                        fila.forEach { InsigniaMini(it, Modifier.weight(1f).fillMaxHeight()) }
                        repeat(columnas - fila.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun InsigniaMini(insignia: InsigniaNino, modifier: Modifier = Modifier) {
    val descripcion = stringResource(
        if (insignia.ganada) R.string.detalle_insignia_ganada else R.string.detalle_insignia_pendiente,
        insignia.nombre,
    )
    val estiloMedalla = EstiloMarcador.Insignia
    val arte = Insignia.entries.firstOrNull { it.nombre == insignia.nombre }
    Column(
        modifier = modifier
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
        if (insignia.ganada && arte != null) {
            IlustracionInsignia(arte, Modifier.size(56.dp))
        } else {
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
        }
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
                    codigoVinculacion = "JB-4QX7",
                    acudienteVinculado = true,
                )
            ),
            onVolver = {},
            onReintentar = {},
            onCompartirCodigo = {},
            onRegenerarCodigo = {},
            onQuitar = {},
        )
    }
}
