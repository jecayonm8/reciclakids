package com.reciclakids.ui.padres

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reciclakids.R
import com.reciclakids.model.HijoVinculado
import com.reciclakids.model.PrivacidadHijo
import com.reciclakids.ui.docente.AltoMinimoBoton
import com.reciclakids.ui.docente.BarraDocente
import com.reciclakids.ui.docente.BordeOpcion
import com.reciclakids.ui.docente.BotonContorno
import com.reciclakids.ui.docente.BotonTexto
import com.reciclakids.ui.docente.EsqueletoCarga
import com.reciclakids.ui.docente.EstadoErrorRed
import com.reciclakids.ui.docente.FilaDato
import com.reciclakids.ui.docente.FormaTarjeta
import com.reciclakids.ui.docente.MarcoDocente
import com.reciclakids.ui.docente.TarjetaDocente
import com.reciclakids.ui.theme.ReciclaKidsTheme
import com.reciclakids.util.diaYMes
import com.reciclakids.util.fechaLarga
import com.reciclakids.viewmodel.EstadoUi
import java.time.LocalDate

/** Ámbar de la solicitud pendiente: es un estado en espera, no un error. */
private val FondoPendiente = Color(0xFFFFF6E8)
private val BordePendiente = Color(0xFFE8C877)
private val TintaPendiente = Color(0xFF4A3416)
private val IconoPendiente = Color(0xFF8A5A16)

/**
 * UI-34 Privacidad y datos del menor: ejercer los derechos de la Ley 1581 de 2012 sin
 * ambigüedad. La eliminación pide una casilla explícita antes de enviar y después muestra el
 * estado de la solicitud. Es el único lugar del Modo Padres con rojo: una acción destructiva.
 */
@Composable
fun PrivacidadDatosScreen(
    estado: EstadoUi<PrivacidadHijo>,
    enviando: Boolean,
    onDescargar: (PrivacidadHijo) -> Unit,
    onVerPolitica: () -> Unit,
    onSolicitarEliminacion: () -> Unit,
    onCancelarSolicitud: () -> Unit,
    onReintentar: () -> Unit,
    onVolver: () -> Unit,
    snackbar: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    MarcoDocente(
        modifier = modifier,
        snackbar = snackbar,
        barra = { BarraDocente(titulo = stringResource(R.string.privacidad_titulo), onAtras = onVolver) },
    ) { relleno ->
        when (estado) {
            EstadoUi.Cargando -> EsqueletoCarga(Modifier.padding(relleno))
            EstadoUi.Error -> EstadoErrorRed(
                onReintentar = onReintentar,
                modifier = Modifier.padding(relleno),
                titulo = stringResource(R.string.padres_error_titulo),
                texto = stringResource(R.string.padres_error_texto),
            )
            is EstadoUi.Vacio -> ContenidoPrivacidad(estado.datos, enviando, onDescargar, onVerPolitica, onSolicitarEliminacion, onCancelarSolicitud, relleno)
            is EstadoUi.Contenido -> ContenidoPrivacidad(estado.datos, enviando, onDescargar, onVerPolitica, onSolicitarEliminacion, onCancelarSolicitud, relleno)
        }
    }
}

@Composable
private fun ContenidoPrivacidad(
    datos: PrivacidadHijo,
    enviando: Boolean,
    onDescargar: (PrivacidadHijo) -> Unit,
    onVerPolitica: () -> Unit,
    onSolicitarEliminacion: () -> Unit,
    onCancelarSolicitud: () -> Unit,
    relleno: PaddingValues,
) {
    var dialogo by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(relleno)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TarjetaAutorizacion(datos)
        TarjetaDatosGuardados(datos, onDescargar = { onDescargar(datos) }, onVerPolitica = onVerPolitica)
        val solicitud = datos.solicitudEliminacion
        if (solicitud != null) {
            TarjetaSolicitudPendiente(solicitud, enviando, onCancelarSolicitud)
        } else {
            TarjetaEliminar(datos.hijo, enviando, onAbrir = { dialogo = true })
        }
    }
    if (dialogo) {
        DialogoEliminacion(
            datos = datos,
            onEnviar = {
                dialogo = false
                onSolicitarEliminacion()
            },
            onCancelar = { dialogo = false },
        )
    }
}

@Composable
private fun TarjetaAutorizacion(datos: PrivacidadHijo) {
    val tinta = MaterialTheme.colorScheme.onTertiaryContainer
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.tertiaryContainer, FormaTarjeta)
            .padding(16.dp)
            .semantics(mergeDescendants = true) { },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = tinta, modifier = Modifier.padding(top = 2.dp).size(22.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.privacidad_autorizada), style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp), color = tinta)
            Text(
                stringResource(R.string.privacidad_autorizada_texto, fechaLarga(datos.autorizadaEl), datos.autorizadaPor),
                style = MaterialTheme.typography.bodyMedium,
                color = tinta,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TarjetaDatosGuardados(datos: PrivacidadHijo, onDescargar: () -> Unit, onVerPolitica: () -> Unit) {
    TarjetaDocente(Modifier.fillMaxWidth(), espacio = 12.dp) {
        Text(stringResource(R.string.privacidad_datos, datos.hijo.nombre), style = MaterialTheme.typography.titleMedium)
        FilaDato(stringResource(R.string.privacidad_nombre), datos.hijo.nombre)
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer)
        FilaDato(stringResource(R.string.privacidad_grupo), datos.hijo.grupo)
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer)
        FilaDato(stringResource(R.string.privacidad_avatar), datos.hijo.avatar)
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer)
        FilaDato(stringResource(R.string.privacidad_resultados), pluralStringResource(R.plurals.privacidad_registros, datos.registros, datos.registros))
        Text(
            stringResource(R.string.privacidad_no_guardamos),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            BotonContorno(stringResource(R.string.privacidad_descargar), onClick = onDescargar)
            BotonTexto(stringResource(R.string.cuenta_ver_politica), onClick = onVerPolitica)
        }
    }
}

@Composable
private fun TarjetaSolicitudPendiente(fecha: LocalDate, enviando: Boolean, onCancelar: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FormaTarjeta,
        color = FondoPendiente,
        border = BorderStroke(1.dp, BordePendiente),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.semantics(mergeDescendants = true) { }, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(painterResource(R.drawable.ic_reloj), contentDescription = null, tint = IconoPendiente, modifier = Modifier.padding(top = 2.dp).size(22.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.privacidad_solicitud_titulo), style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp), color = TintaPendiente)
                    Text(stringResource(R.string.privacidad_solicitud_texto, diaYMes(fecha)), style = MaterialTheme.typography.bodyMedium, color = TintaPendiente)
                }
            }
            OutlinedButton(
                onClick = onCancelar,
                enabled = !enviando,
                modifier = Modifier.heightIn(min = AltoMinimoBoton),
                border = BorderStroke(1.dp, IconoPendiente),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TintaPendiente),
            ) {
                Text(stringResource(R.string.privacidad_cancelar_solicitud), style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp))
            }
        }
    }
}

@Composable
private fun TarjetaEliminar(hijo: HijoVinculado, enviando: Boolean, onAbrir: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FormaTarjeta,
        color = Color.White,
        border = BorderStroke(1.dp, BordeOpcion),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.privacidad_eliminar_titulo, hijo.nombre), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.privacidad_eliminar_texto),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onAbrir,
                enabled = !enviando,
                modifier = Modifier.fillMaxWidth().heightIn(min = AltoMinimoBoton),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                if (enviando) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = MaterialTheme.colorScheme.error, strokeWidth = 2.5.dp)
                } else {
                    Text(stringResource(R.string.privacidad_eliminar_boton), style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp))
                }
            }
        }
    }
}

/** Advertencia → casilla explícita, nunca premarcada → enviar. Sin la casilla el botón no responde. */
@Composable
private fun DialogoEliminacion(datos: PrivacidadHijo, onEnviar: () -> Unit, onCancelar: () -> Unit) {
    var confirmado by rememberSaveable { mutableStateOf(false) }
    val error = MaterialTheme.colorScheme.error
    AlertDialog(
        onDismissRequest = onCancelar,
        icon = { Icon(painterResource(R.drawable.ic_alerta), contentDescription = null, tint = error) },
        title = { Text(stringResource(R.string.privacidad_dialogo_titulo, datos.hijo.nombre), textAlign = TextAlign.Center) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    stringResource(
                        R.string.privacidad_dialogo_texto,
                        pluralStringResource(R.plurals.privacidad_registros_retos, datos.registros, datos.registros),
                        pluralStringResource(R.plurals.privacidad_insignias, datos.insignias, datos.insignias),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(14.dp))
                        .toggleable(value = confirmado, role = Role.Checkbox, onValueChange = { confirmado = it })
                        .heightIn(min = 56.dp)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Checkbox(
                        checked = confirmado,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(checkedColor = error),
                    )
                    Text(stringResource(R.string.privacidad_dialogo_casilla, datos.hijo.nombre), style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onEnviar,
                enabled = confirmado,
                modifier = Modifier.heightIn(min = AltoMinimoBoton),
                colors = ButtonDefaults.buttonColors(containerColor = error, contentColor = MaterialTheme.colorScheme.onError),
            ) {
                Text(stringResource(R.string.privacidad_dialogo_enviar))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar, modifier = Modifier.heightIn(min = AltoMinimoBoton)) {
                Text(stringResource(R.string.comun_cancelar))
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    )
}

@Preview(widthDp = 360, heightDp = 900)
@Composable
private fun PrivacidadDatosPreview() {
    val salome = HijoVinculado("nino-1", "Salomé M.", "Jardín B", 5, "Pulpo azul", LocalDate.of(2026, 8, 21))
    ReciclaKidsTheme {
        PrivacidadDatosScreen(
            estado = EstadoUi.Contenido(PrivacidadHijo(salome, salome.vinculadoEl, "Mariana Ríos", 13, 4, solicitudEliminacion = null)),
            enviando = false,
            onDescargar = {}, onVerPolitica = {}, onSolicitarEliminacion = {}, onCancelarSolicitud = {},
            onReintentar = {}, onVolver = {},
            snackbar = remember { SnackbarHostState() },
        )
    }
}
