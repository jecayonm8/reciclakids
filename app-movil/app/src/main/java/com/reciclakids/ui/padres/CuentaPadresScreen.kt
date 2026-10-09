package com.reciclakids.ui.padres

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reciclakids.R
import com.reciclakids.model.HijoVinculado
import com.reciclakids.model.PerfilAcudiente
import com.reciclakids.ui.docente.AvatarNino
import com.reciclakids.ui.docente.BarraDocente
import com.reciclakids.ui.docente.BotonContorno
import com.reciclakids.ui.docente.BotonLleno
import com.reciclakids.ui.docente.BotonTexto
import com.reciclakids.ui.docente.EsqueletoCarga
import com.reciclakids.ui.docente.EstadoErrorRed
import com.reciclakids.ui.docente.FormaTarjeta
import com.reciclakids.ui.docente.MarcoDocente
import com.reciclakids.ui.docente.TarjetaDocente
import com.reciclakids.ui.docente.iniciales
import com.reciclakids.ui.theme.ReciclaKidsTheme
import com.reciclakids.util.diaYMes
import com.reciclakids.viewmodel.EstadoUi
import com.reciclakids.viewmodel.EstadoVinculacion
import com.reciclakids.viewmodel.codigoVinculacionCompleto
import java.time.LocalDate

/** UI-35 Cuenta: perfil, contraseña, control parental, privacidad, hijos vinculados y sesión. */
@Composable
fun CuentaPadresScreen(
    estado: EstadoUi<PerfilAcudiente>,
    onReintentar: () -> Unit,
    onEditarPerfil: () -> Unit,
    onCambiarContrasena: () -> Unit,
    onControlParental: () -> Unit,
    onPrivacidad: () -> Unit,
    onVincular: () -> Unit,
    onCerrarSesion: () -> Unit,
    snackbar: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    MarcoDocente(
        modifier = modifier,
        snackbar = snackbar,
        barra = { BarraDocente(titulo = stringResource(R.string.cuenta_padres_titulo)) },
    ) { relleno ->
        when (estado) {
            EstadoUi.Cargando -> EsqueletoCarga(Modifier.padding(relleno))
            EstadoUi.Error -> EstadoErrorRed(
                onReintentar = onReintentar,
                modifier = Modifier.padding(relleno),
                titulo = stringResource(R.string.padres_error_titulo),
                texto = stringResource(R.string.padres_error_texto),
            )
            is EstadoUi.Vacio -> Unit
            is EstadoUi.Contenido -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(relleno)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                TarjetaPerfil(estado.datos)
                TarjetaOpciones(
                    listOf(
                        stringResource(R.string.cuenta_editar_perfil) to onEditarPerfil,
                        stringResource(R.string.cuenta_cambiar_contrasena) to onCambiarContrasena,
                        stringResource(R.string.control_titulo) to onControlParental,
                        stringResource(R.string.cuenta_privacidad_menor) to onPrivacidad,
                    )
                )
                TarjetaHijos(estado.datos.hijos, onVincular)
                BotonTexto(
                    stringResource(R.string.cuenta_cerrar_sesion),
                    onClick = onCerrarSesion,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TarjetaPerfil(perfil: PerfilAcudiente) {
    TarjetaDocente(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.semantics(mergeDescendants = true) { },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                    .clearAndSetSemantics { },
                contentAlignment = Alignment.Center,
            ) {
                Text(iniciales(perfil.nombre), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Column(Modifier.weight(1f)) {
                Text(perfil.nombre, style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp))
                Text(perfil.correo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Lista de opciones de 60 dp de alto, cada fila con su chevron. */
@Composable
private fun TarjetaOpciones(opciones: List<Pair<String, () -> Unit>>) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = FormaTarjeta, color = Color.White, shadowElevation = 1.dp) {
        Column {
            opciones.forEachIndexed { i, (etiqueta, onClick) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp)
                        .clickable(role = Role.Button, onClick = onClick)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(etiqueta, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Icon(painterResource(R.drawable.ic_siguiente), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (i < opciones.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer)
            }
        }
    }
}

@Composable
private fun TarjetaHijos(hijos: List<HijoVinculado>, onVincular: () -> Unit) {
    TarjetaDocente(Modifier.fillMaxWidth(), espacio = 12.dp) {
        Text(stringResource(R.string.cuenta_hijos), style = MaterialTheme.typography.titleMedium)
        hijos.forEach { hijo ->
            Row(
                modifier = Modifier.heightIn(min = 56.dp).semantics(mergeDescendants = true) { },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AvatarNino(Modifier.size(40.dp))
                Column(Modifier.weight(1f)) {
                    Text(hijo.nombre, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.cuenta_hijo_detalle, hijo.grupo, diaYMes(hijo.vinculadoEl)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        BotonContorno(stringResource(R.string.cuenta_vincular), onClick = onVincular, modifier = Modifier.fillMaxWidth())
        Text(
            stringResource(R.string.cuenta_vincular_nota),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Vincular otro hijo o hija con el código que entrega su docente («JB-2M91»). El código se
 * escribe en mayúscula y con el guion solo; el botón se activa cuando está completo.
 */
@Composable
fun VincularHijoScreen(
    codigo: String,
    estado: EstadoVinculacion,
    onCodigo: (String) -> Unit,
    onVincular: () -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MarcoDocente(
        modifier = modifier,
        barra = { BarraDocente(titulo = stringResource(R.string.vincular_titulo), onAtras = onVolver) },
    ) { relleno ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno)
                .verticalScroll(rememberScrollState())
                .padding(PaddingValues(horizontal = 24.dp, vertical = 24.dp)),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.vincular_texto), style = MaterialTheme.typography.bodyLarge)
            val error = when (estado) {
                EstadoVinculacion.CodigoInvalido -> stringResource(R.string.vincular_invalido)
                EstadoVinculacion.SinConexion -> stringResource(R.string.docente_error_accion)
                else -> null
            }
            val completo = codigoVinculacionCompleto(codigo)
            OutlinedTextField(
                value = codigo,
                onValueChange = onCodigo,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.vincular_campo)) },
                supportingText = { Text(error ?: stringResource(R.string.vincular_ejemplo)) },
                isError = error != null,
                singleLine = true,
                textStyle = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace, letterSpacing = 2.sp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { if (completo) onVincular() }),
            )
            BotonLleno(
                stringResource(R.string.vincular_boton),
                onClick = onVincular,
                habilitado = completo,
                cargando = estado == EstadoVinculacion.Enviando,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                stringResource(R.string.vincular_privacidad),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(widthDp = 360, heightDp = 900)
@Composable
private fun CuentaPadresPreview() {
    val desde = LocalDate.of(2026, 9, 12)
    ReciclaKidsTheme {
        CuentaPadresScreen(
            estado = EstadoUi.Contenido(
                PerfilAcudiente(
                    "Mariana Ríos",
                    "mariana.r@correo.com",
                    listOf(
                        HijoVinculado("nino-1", "Salomé M.", "Jardín B", 5, "Pulpo azul", desde),
                        HijoVinculado("nino-a7", "Martín M.", "Jardín A", 4, "Tortuga verde", desde),
                    ),
                )
            ),
            onReintentar = {}, onEditarPerfil = {}, onCambiarContrasena = {}, onControlParental = {},
            onPrivacidad = {}, onVincular = {}, onCerrarSesion = {},
            snackbar = remember { SnackbarHostState() },
        )
    }
}
