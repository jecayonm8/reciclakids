package com.reciclakids.ui.docente

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.reciclakids.R
import com.reciclakids.model.AvisosDocente
import com.reciclakids.model.PerfilDocente
import com.reciclakids.ui.theme.ReciclaKidsTheme
import com.reciclakids.viewmodel.EstadoUi

/** UI-28 Cuenta y ajustes: perfil, avisos, código de vinculación y privacidad (Ley 1581 de 2012). */
@Composable
fun CuentaDocenteScreen(
    estado: EstadoUi<PerfilDocente>,
    onReintentar: () -> Unit,
    onAvisos: (AvisosDocente) -> Unit,
    onCompartirCodigo: (String) -> Unit,
    onRegenerarCodigo: () -> Unit,
    onEditarPerfil: () -> Unit,
    onVerPolitica: () -> Unit,
    onCerrarSesion: () -> Unit,
    onEliminarDatos: () -> Unit,
    modifier: Modifier = Modifier,
    snackbar: SnackbarHostState? = null,
) {
    MarcoDocente(
        modifier = modifier,
        snackbar = snackbar,
        barra = { BarraDocente(titulo = stringResource(R.string.cuenta_titulo)) },
    ) { relleno ->
        val cuerpo = Modifier.padding(relleno)
        when (estado) {
            EstadoUi.Cargando -> EsqueletoCarga(cuerpo)
            EstadoUi.Error -> EstadoErrorRed(onReintentar, cuerpo)
            is EstadoUi.Vacio -> ContenidoCuenta(
                estado.datos, onAvisos, onCompartirCodigo, onRegenerarCodigo, onEditarPerfil,
                onVerPolitica, onCerrarSesion, onEliminarDatos, cuerpo,
            )
            is EstadoUi.Contenido -> ContenidoCuenta(
                estado.datos, onAvisos, onCompartirCodigo, onRegenerarCodigo, onEditarPerfil,
                onVerPolitica, onCerrarSesion, onEliminarDatos, cuerpo,
            )
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun ContenidoCuenta(
    perfil: PerfilDocente,
    onAvisos: (AvisosDocente) -> Unit,
    onCompartirCodigo: (String) -> Unit,
    onRegenerarCodigo: () -> Unit,
    onEditarPerfil: () -> Unit,
    onVerPolitica: () -> Unit,
    onCerrarSesion: () -> Unit,
    onEliminarDatos: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Toda acción sensible pide confirmación antes de llegar al servicio.
    var confirmarRegenerar by rememberSaveable { mutableStateOf(false) }
    var confirmarEliminar by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TarjetaPerfil(perfil, onEditarPerfil)
        TarjetaAvisos(perfil.avisos, onAvisos)
        TarjetaVinculacion(
            codigo = perfil.codigoVinculacion,
            onCompartir = { onCompartirCodigo(perfil.codigoVinculacion) },
            onRegenerar = { confirmarRegenerar = true },
        )
        TarjetaPrivacidad(onVerPolitica, onCerrarSesion, onEliminar = { confirmarEliminar = true })
    }
    if (confirmarRegenerar) {
        DialogoConfirmacion(
            titulo = stringResource(R.string.cuenta_regenerar_titulo),
            texto = stringResource(R.string.cuenta_regenerar_texto),
            confirmar = stringResource(R.string.cuenta_regenerar_confirmar),
            destructivo = false,
            onConfirmar = {
                confirmarRegenerar = false
                onRegenerarCodigo()
            },
            onCancelar = { confirmarRegenerar = false },
        )
    }
    if (confirmarEliminar) {
        DialogoConfirmacion(
            titulo = stringResource(R.string.cuenta_eliminar_titulo),
            texto = pluralStringResource(R.plurals.cuenta_eliminar_texto, perfil.totalNinos, perfil.totalNinos),
            confirmar = stringResource(R.string.cuenta_eliminar_confirmar),
            destructivo = true,
            onConfirmar = {
                confirmarEliminar = false
                onEliminarDatos()
            },
            onCancelar = { confirmarEliminar = false },
        )
    }
}

@Composable
private fun TarjetaPerfil(perfil: PerfilDocente, onEditarPerfil: () -> Unit) {
    TarjetaDocente(Modifier.fillMaxWidth(), relleno = PaddingValues(20.dp), espacio = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                Modifier.size(64.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    iniciales(perfil.nombre),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(perfil.nombre, style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp))
                Text(
                    perfil.correo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer)
        FilaDato(stringResource(R.string.cuenta_jardin), perfil.jardin)
        FilaDato(
            stringResource(R.string.cuenta_grupo),
            stringResource(
                R.string.cuenta_grupo_valor,
                perfil.grupo,
                pluralStringResource(R.plurals.grupo_ninos, perfil.totalNinos, perfil.totalNinos),
            ),
        )
        BotonContorno(stringResource(R.string.cuenta_editar_perfil), onClick = onEditarPerfil)
    }
}

@Composable
private fun FilaDato(etiqueta: String, valor: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(etiqueta, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            valor,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TarjetaAvisos(avisos: AvisosDocente, onAvisos: (AvisosDocente) -> Unit) {
    TarjetaDocente(Modifier.fillMaxWidth(), relleno = PaddingValues(20.dp), espacio = 16.dp) {
        Text(stringResource(R.string.cuenta_avisos), style = MaterialTheme.typography.titleMedium)
        FilaInterruptor(
            titulo = stringResource(R.string.aviso_publicacion),
            detalle = stringResource(R.string.aviso_publicacion_detalle),
            activo = avisos.confirmarPublicacion,
            onCambio = { onAvisos(avisos.copy(confirmarPublicacion = it)) },
        )
        FilaInterruptor(
            titulo = stringResource(R.string.aviso_resumen),
            detalle = stringResource(R.string.aviso_resumen_detalle),
            activo = avisos.resumenDiario,
            onCambio = { onAvisos(avisos.copy(resumenDiario = it)) },
        )
        FilaInterruptor(
            titulo = stringResource(R.string.aviso_sin_jugar),
            detalle = stringResource(R.string.aviso_sin_jugar_detalle),
            activo = avisos.ninosSinJugar,
            onCambio = { onAvisos(avisos.copy(ninosSinJugar = it)) },
        )
    }
}

/** Toda la fila es el objetivo táctil del interruptor. */
@Composable
private fun FilaInterruptor(titulo: String, detalle: String, activo: Boolean, onCambio: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = activo, role = Role.Switch, onValueChange = onCambio),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.bodyLarge)
            Text(detalle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = activo, onCheckedChange = null)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TarjetaVinculacion(codigo: String, onCompartir: () -> Unit, onRegenerar: () -> Unit) {
    TarjetaDocente(Modifier.fillMaxWidth(), relleno = PaddingValues(20.dp), espacio = 12.dp) {
        Text(stringResource(R.string.cuenta_vinculacion), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.cuenta_vinculacion_texto),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                codigo,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                fontSize = 26.sp,
                letterSpacing = 0.14.em,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            )
            Row(Modifier.align(Alignment.CenterVertically), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                BotonContorno(stringResource(R.string.cuenta_compartir), onClick = onCompartir)
                BotonTexto(stringResource(R.string.cuenta_regenerar), onClick = onRegenerar)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TarjetaPrivacidad(onVerPolitica: () -> Unit, onCerrarSesion: () -> Unit, onEliminar: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FormaTarjeta,
        color = Color.White,
        border = BorderStroke(1.dp, BordeOpcion),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.cuenta_privacidad), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.cuenta_privacidad_texto),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                BotonContorno(stringResource(R.string.cuenta_ver_politica), onClick = onVerPolitica)
                BotonTexto(stringResource(R.string.cuenta_cerrar_sesion), onClick = onCerrarSesion, color = MaterialTheme.colorScheme.onSurfaceVariant)
                // Acción destructiva: el único lugar del Modo Docente donde se usa el rojo.
                FilledTonalButton(
                    onClick = onEliminar,
                    modifier = Modifier.heightIn(min = AltoMinimoBoton),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.cuenta_eliminar), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun DialogoConfirmacion(
    titulo: String,
    texto: String,
    confirmar: String,
    destructivo: Boolean,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
) {
    val colorConfirmar = if (destructivo) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    AlertDialog(
        onDismissRequest = onCancelar,
        icon = if (destructivo) {
            { Icon(painterResource(R.drawable.ic_alerta), contentDescription = null, tint = MaterialTheme.colorScheme.error) }
        } else {
            null
        },
        title = { Text(titulo, textAlign = TextAlign.Center) },
        text = { Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        confirmButton = {
            TextButton(onClick = onConfirmar) { Text(confirmar, color = colorConfirmar) }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text(stringResource(R.string.comun_cancelar)) }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    )
}

/** «Laura Restrepo» → «LR». */
internal fun iniciales(nombre: String): String =
    nombre.trim().split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }

@Preview(widthDp = 360, heightDp = 1100)
@Composable
private fun CuentaDocentePreview() {
    ReciclaKidsTheme {
        CuentaDocenteScreen(
            estado = EstadoUi.Contenido(
                PerfilDocente("Laura Restrepo", "laura.r@jardin.edu.co", "Gotitas", "Jardín B", 22, "JB-2M91", AvisosDocente())
            ),
            onReintentar = {}, onAvisos = {}, onCompartirCodigo = {}, onRegenerarCodigo = {}, onEditarPerfil = {},
            onVerPolitica = {}, onCerrarSesion = {}, onEliminarDatos = {},
        )
    }
}
