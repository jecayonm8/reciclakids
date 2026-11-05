package com.reciclakids.ui.acceso

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reciclakids.R
import com.reciclakids.network.ResultadoCambioContrasena
import com.reciclakids.ui.theme.ReciclaKidsTheme
import com.reciclakids.util.contrasenaValida

/**
 * Cambiar la contraseña con la sesión iniciada, desde la cuenta de la docente o del acudiente.
 * Pide la actual y la nueva dos veces; si no recuerda la actual, «¿La olvidaste?» abre la
 * recuperación por correo (hallazgo 7 de la auditoría).
 */
@Composable
fun CambiarContrasenaScreen(
    onAtras: () -> Unit,
    onCambiar: (actual: String, nueva: String) -> Unit,
    onOlvide: () -> Unit,
    modifier: Modifier = Modifier,
    cambiando: Boolean = false,
    resultado: ResultadoCambioContrasena? = null,
) {
    var actual by rememberSaveable { mutableStateOf("") }
    var nueva by rememberSaveable { mutableStateOf("") }
    var confirmacion by rememberSaveable { mutableStateOf("") }
    var intentoEnviar by rememberSaveable { mutableStateOf(false) }
    // El error de la actual se va en cuanto se vuelve a escribir.
    var actualRechazada by rememberSaveable(resultado) { mutableStateOf(resultado == ResultadoCambioContrasena.ActualIncorrecta) }

    val errorActual = when {
        actualRechazada -> stringResource(R.string.cambiar_contrasena_actual_incorrecta)
        intentoEnviar && actual.isEmpty() -> stringResource(R.string.error_campo_obligatorio)
        else -> null
    }
    val errorNueva = when {
        !intentoEnviar -> null
        !contrasenaValida(nueva) -> stringResource(R.string.error_contrasena_corta)
        nueva == actual -> stringResource(R.string.cambiar_contrasena_igual)
        else -> null
    }
    val errorConfirmacion = if (intentoEnviar && confirmacion != nueva) stringResource(R.string.cambiar_contrasena_no_coincide) else null

    Scaffold(
        modifier = modifier,
        topBar = { BarraSuperiorAcceso(stringResource(R.string.cuenta_cambiar_contrasena), onAtras) },
    ) { relleno ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            if (resultado == ResultadoCambioContrasena.Cambiada) {
                AvisoExito(stringResource(R.string.cambiar_contrasena_listo))
                BotonPrincipal(texto = stringResource(R.string.cambiar_contrasena_volver), onClick = onAtras)
            } else {
                Text(
                    stringResource(R.string.cambiar_contrasena_texto),
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                CampoContrasena(
                    actual,
                    {
                        actual = it
                        actualRechazada = false
                    },
                    etiqueta = stringResource(R.string.cambiar_contrasena_actual),
                    error = errorActual,
                    imeAction = ImeAction.Next,
                )
                CampoContrasena(
                    nueva, { nueva = it },
                    etiqueta = stringResource(R.string.cambiar_contrasena_nueva),
                    error = errorNueva,
                    imeAction = ImeAction.Next,
                )
                CampoContrasena(
                    confirmacion, { confirmacion = it },
                    etiqueta = stringResource(R.string.cambiar_contrasena_confirmar),
                    error = errorConfirmacion,
                )
                BotonPrincipal(
                    texto = stringResource(R.string.cuenta_cambiar_contrasena),
                    onClick = {
                        intentoEnviar = true
                        if (actual.isNotEmpty() && contrasenaValida(nueva) && nueva != actual && confirmacion == nueva) onCambiar(actual, nueva)
                    },
                    cargando = cambiando,
                )
                if (resultado == ResultadoCambioContrasena.SinConexion) AvisoError(stringResource(R.string.error_sin_conexion))
                TextButton(onClick = onOlvide, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.cambiar_contrasena_olvide))
                }
            }
        }
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun CambiarContrasenaScreenPreview() {
    ReciclaKidsTheme { CambiarContrasenaScreen(onAtras = {}, onCambiar = { _, _ -> }, onOlvide = {}) }
}
