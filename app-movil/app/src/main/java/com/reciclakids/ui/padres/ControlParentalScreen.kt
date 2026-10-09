package com.reciclakids.ui.padres

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reciclakids.R
import com.reciclakids.model.ControlParental
import com.reciclakids.model.Dificultad
import com.reciclakids.model.OpcionesMinutosDiarios
import com.reciclakids.model.PasoMinutosDiarios
import com.reciclakids.ui.docente.BarraDocente
import com.reciclakids.ui.docente.BordeOpcion
import com.reciclakids.ui.docente.EsqueletoCarga
import com.reciclakids.ui.docente.EstadoErrorRed
import com.reciclakids.ui.docente.FilaInterruptor
import com.reciclakids.ui.docente.MarcoDocente
import com.reciclakids.ui.docente.TarjetaDocente
import com.reciclakids.ui.docente.nombre
import com.reciclakids.ui.theme.ReciclaKidsTheme
import com.reciclakids.viewmodel.EstadoUi
import kotlin.math.roundToInt

/**
 * UI-33 Control parental: poner límites sin negociar con el niño. Cada cambio se guarda solo y
 * se avisa con «Cambios guardados · Deshacer». El tiempo diario lo hace cumplir el Modo Niño con
 * UI-19, que no se puede saltar.
 */
@Composable
fun ControlParentalScreen(
    nombreHijo: String,
    estado: EstadoUi<ControlParental>,
    onCambio: (ControlParental) -> Unit,
    onReintentar: () -> Unit,
    onVolver: () -> Unit,
    snackbar: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    MarcoDocente(
        modifier = modifier,
        snackbar = snackbar,
        barra = { BarraDocente(titulo = stringResource(R.string.control_titulo), subtitulo = nombreHijo, onAtras = onVolver) },
    ) { relleno ->
        when (estado) {
            EstadoUi.Cargando -> EsqueletoCarga(Modifier.padding(relleno))
            EstadoUi.Error -> EstadoErrorRed(
                onReintentar = onReintentar,
                modifier = Modifier.padding(relleno),
                titulo = stringResource(R.string.padres_error_titulo),
                texto = stringResource(R.string.padres_error_texto),
            )
            is EstadoUi.Vacio -> ContenidoControl(estado.datos, onCambio, relleno)
            is EstadoUi.Contenido -> ContenidoControl(estado.datos, onCambio, relleno)
        }
    }
}

@Composable
private fun ContenidoControl(control: ControlParental, onCambio: (ControlParental) -> Unit, relleno: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(relleno)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TarjetaDificultad(control.dificultadSugerida) { onCambio(control.copy(dificultadSugerida = it)) }
        TarjetaTiempo(control.limiteMinutosDiarios) { onCambio(control.conMinutos(it)) }
        TarjetaDocente(Modifier.fillMaxWidth(), espacio = 16.dp) {
            Text(stringResource(R.string.control_correos), style = MaterialTheme.typography.titleMedium)
            FilaInterruptor(
                titulo = stringResource(R.string.control_correo_logros),
                detalle = stringResource(R.string.control_correo_logros_detalle),
                activo = control.correosLogro,
                onCambio = { onCambio(control.copy(correosLogro = it)) },
            )
            FilaInterruptor(
                titulo = stringResource(R.string.control_correo_semanal),
                detalle = stringResource(R.string.control_correo_semanal_detalle),
                activo = control.reporteSemanal,
                onCambio = { onCambio(control.copy(reporteSemanal = it)) },
            )
            FilaInterruptor(
                titulo = stringResource(R.string.control_correo_recordatorio),
                detalle = stringResource(R.string.control_correo_recordatorio_detalle),
                activo = control.recordatorioSinJugar,
                onCambio = { onCambio(control.copy(recordatorioSinJugar = it)) },
            )
        }
    }
}

@Composable
private fun TarjetaDificultad(elegida: Dificultad, onElegir: (Dificultad) -> Unit) {
    TarjetaDocente(Modifier.fillMaxWidth(), espacio = 12.dp) {
        Text(stringResource(R.string.control_dificultad), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.control_dificultad_texto),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Dificultad.entries.forEach { dificultad ->
                val activa = dificultad == elegida
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                        .selectable(selected = activa, role = Role.RadioButton, onClick = { onElegir(dificultad) }),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = if (activa) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, BordeOpcion),
                ) {
                    Row(
                        Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        RadioButton(selected = activa, onClick = null)
                        Column(Modifier.weight(1f)) {
                            Text(dificultad.nombre(), style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp))
                            Text(
                                dificultad.detallePadres(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Botones ± de 48 dp y un deslizador: el cambio se guarda al soltar, no en cada paso del arrastre. */
@Composable
private fun TarjetaTiempo(minutos: Int, onMinutos: (Int) -> Unit) {
    var arrastre by remember { mutableStateOf<Float?>(null) }
    val mostrado = arrastre?.let { redondearMinutos(it) } ?: minutos
    val minimo = OpcionesMinutosDiarios.first
    val maximo = OpcionesMinutosDiarios.last
    TarjetaDocente(Modifier.fillMaxWidth(), espacio = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.control_tiempo), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(
                stringResource(R.string.control_minutos, mostrado),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BotonPaso("–", stringResource(R.string.control_menos), habilitado = minutos > minimo) { onMinutos(minutos - PasoMinutosDiarios) }
            // TalkBack lee «Tiempo diario de juego, 20 min» en vez de un porcentaje.
            val etiqueta = stringResource(R.string.control_tiempo)
            val valor = stringResource(R.string.control_minutos, mostrado)
            Slider(
                value = (arrastre ?: minutos.toFloat()),
                onValueChange = { arrastre = it },
                onValueChangeFinished = {
                    arrastre?.let { onMinutos(redondearMinutos(it)) }
                    arrastre = null
                },
                valueRange = minimo.toFloat()..maximo.toFloat(),
                steps = (maximo - minimo) / PasoMinutosDiarios - 1,
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        contentDescription = etiqueta
                        stateDescription = valor
                    },
            )
            BotonPaso("+", stringResource(R.string.control_mas), habilitado = minutos < maximo) { onMinutos(minutos + PasoMinutosDiarios) }
        }
        Row {
            Text(
                stringResource(R.string.control_minutos, minimo),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(stringResource(R.string.control_minutos, maximo), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            stringResource(R.string.control_tiempo_nota),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun redondearMinutos(valor: Float): Int = (valor / PasoMinutosDiarios).roundToInt() * PasoMinutosDiarios

@Composable
private fun BotonPaso(simbolo: String, descripcion: String, habilitado: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = habilitado,
        modifier = Modifier.size(48.dp).semantics { contentDescription = descripcion },
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp),
        border = BorderStroke(1.dp, if (habilitado) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Text(simbolo, fontSize = 24.sp, color = if (habilitado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun Dificultad.detallePadres(): String = stringResource(
    when (this) {
        Dificultad.Facil -> R.string.control_facil
        Dificultad.Medio -> R.string.control_medio
        Dificultad.Dificil -> R.string.control_dificil
    }
)

@Preview(widthDp = 360, heightDp = 1000)
@Composable
private fun ControlParentalPreview() {
    ReciclaKidsTheme {
        ControlParentalScreen(
            nombreHijo = "Salomé M.",
            estado = EstadoUi.Contenido(ControlParental()),
            onCambio = {},
            onReintentar = {},
            onVolver = {},
            snackbar = remember { SnackbarHostState() },
        )
    }
}
