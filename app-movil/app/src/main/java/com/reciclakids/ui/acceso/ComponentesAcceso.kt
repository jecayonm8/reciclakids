package com.reciclakids.ui.acceso

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reciclakids.R

private val FormaCampo = RoundedCornerShape(6.dp)
private val FormaPanel = RoundedCornerShape(14.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BarraSuperiorAcceso(titulo: String, onAtras: () -> Unit) {
    TopAppBar(
        title = { Text(titulo, style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp)) },
        navigationIcon = {
            IconButton(onClick = onAtras) {
                Icon(painterResource(R.drawable.ic_atras), contentDescription = stringResource(R.string.comun_atras))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    )
}

@Composable
internal fun CampoTexto(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    ayuda: String? = null,
    teclado: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
    estiloTexto: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        modifier = modifier.fillMaxWidth(),
        label = { Text(etiqueta) },
        textStyle = estiloTexto,
        isError = error != null,
        supportingText = (error ?: ayuda)?.let { texto -> { Text(texto) } },
        singleLine = true,
        keyboardOptions = teclado,
        shape = FormaCampo,
    )
}

@Composable
internal fun CampoCorreo(
    valor: String,
    onCambio: (String) -> Unit,
    modifier: Modifier = Modifier,
    etiqueta: String = stringResource(R.string.campo_correo),
    error: String? = null,
    imeAction: ImeAction = ImeAction.Next,
) {
    CampoTexto(
        valor = valor,
        onCambio = onCambio,
        etiqueta = etiqueta,
        modifier = modifier,
        error = error,
        teclado = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = imeAction),
    )
}

@Composable
internal fun CampoContrasena(
    valor: String,
    onCambio: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    imeAction: ImeAction = ImeAction.Done,
    onListo: () -> Unit = {},
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        modifier = modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.campo_contrasena)) },
        isError = error != null,
        supportingText = error?.let { texto -> { Text(texto) } },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onListo() }),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    painterResource(if (visible) R.drawable.ic_ojo_tachado else R.drawable.ic_ojo),
                    contentDescription = stringResource(
                        if (visible) R.string.campo_ocultar_contrasena else R.string.campo_mostrar_contrasena
                    ),
                )
            }
        },
        shape = FormaCampo,
    )
}

/** Botón principal de 56 dp de los formularios de acceso. Con [cargando] muestra el progreso y no responde. */
@Composable
internal fun BotonPrincipal(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    cargando: Boolean = false,
) {
    Button(
        onClick = { if (!cargando) onClick() },
        modifier = modifier.fillMaxWidth().height(56.dp),
        enabled = habilitado,
        colors = ButtonDefaults.buttonColors(
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = Color(0xFF9AA4A7),
        ),
    ) {
        if (cargando) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.5.dp,
            )
        } else {
            Text(texto, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
internal fun PasoProgreso(paso: Int, total: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.paso_de, paso, total), style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp))
        LinearProgressIndicator(
            progress = { paso / total.toFloat() },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
    }
}

/**
 * Casilla de aceptación explícita: toda la fila es el objetivo táctil. Quien la usa decide el
 * valor inicial de [marcada]; para datos de menores nunca puede venir premarcada.
 */
@Composable
internal fun CasillaConsentimiento(
    marcada: Boolean,
    onCambio: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    relleno: PaddingValues = PaddingValues(14.dp),
    texto: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerLow, FormaPanel)
            .toggleable(value = marcada, role = Role.Checkbox, onValueChange = onCambio)
            .padding(relleno),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Checkbox(checked = marcada, onCheckedChange = null, modifier = Modifier.padding(top = 2.dp))
        texto()
    }
}

/** Aviso positivo en línea: enlace enviado, cuenta creada. */
@Composable
internal fun AvisoExito(texto: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.tertiaryContainer, FormaPanel)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painterResource(R.drawable.ic_check),
            contentDescription = null,
            modifier = Modifier.padding(top = 2.dp).size(22.dp),
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
    }
}

/** Error de red o del servidor en un formulario de adultos. El rojo solo existe en estos modos. */
@Composable
internal fun AvisoError(texto: String, modifier: Modifier = Modifier) {
    Text(
        text = texto,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer, FormaPanel)
            .padding(14.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onErrorContainer,
    )
}
