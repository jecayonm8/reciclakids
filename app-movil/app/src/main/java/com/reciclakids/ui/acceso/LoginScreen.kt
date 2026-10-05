package com.reciclakids.ui.acceso

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reciclakids.R
import com.reciclakids.ui.theme.BalooDos
import com.reciclakids.ui.theme.Nunito
import com.reciclakids.ui.theme.ReciclaKidsTheme
import com.reciclakids.util.correoValido

/** Por qué no se pudo iniciar sesión. */
enum class ErrorLogin { CredencialesInvalidas, SinConexion }

/**
 * UI-03 Inicio de sesión de adultos. Docentes y acudientes entran por la misma pantalla;
 * la app abre el modo según el rol de la cuenta.
 */
@Composable
fun LoginScreen(
    onEntrar: (correo: String, contrasena: String) -> Unit,
    onOlvideContrasena: () -> Unit,
    onCrearCuenta: () -> Unit,
    onVolverInicio: () -> Unit,
    modifier: Modifier = Modifier,
    cargando: Boolean = false,
    error: ErrorLogin? = null,
    cuentaRecienCreada: Boolean = false,
) {
    var correo by rememberSaveable { mutableStateOf("") }
    // La contraseña no pasa por el estado guardado de la actividad.
    var contrasena by remember { mutableStateOf("") }
    var intentoEnviar by rememberSaveable { mutableStateOf(false) }

    val correoOk = correoValido(correo)
    val enviar = {
        intentoEnviar = true
        if (correoOk && contrasena.isNotEmpty()) onEntrar(correo.trim(), contrasena)
    }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0xFF00687F), Color(0xFF17A2C4))))
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                stringResource(R.string.app_name),
                fontFamily = BalooDos,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 32.sp,
                color = Color.White,
            )
            Text(
                AnnotatedString.fromHtml(stringResource(R.string.login_encabezado)),
                fontFamily = Nunito,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = Color.White,
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(stringResource(R.string.login_titulo), style = MaterialTheme.typography.headlineSmall)
            if (cuentaRecienCreada) AvisoExito(stringResource(R.string.login_cuenta_creada))
            if (error == ErrorLogin.SinConexion) AvisoError(stringResource(R.string.error_sin_conexion))
            CampoCorreo(
                valor = correo,
                onCambio = { correo = it },
                error = if (intentoEnviar && !correoOk) stringResource(R.string.error_correo) else null,
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CampoContrasena(
                    valor = contrasena,
                    onCambio = { contrasena = it },
                    error = when {
                        intentoEnviar && contrasena.isEmpty() -> stringResource(R.string.error_campo_obligatorio)
                        error == ErrorLogin.CredencialesInvalidas -> stringResource(R.string.login_error_credenciales)
                        else -> null
                    },
                    onListo = enviar,
                )
                TextButton(onClick = onOlvideContrasena, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.login_olvidaste))
                }
            }
            BotonPrincipal(stringResource(R.string.login_entrar), onClick = enviar, cargando = cargando)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.surfaceVariant)
                Text(
                    stringResource(R.string.login_sin_cuenta),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
                HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.surfaceVariant)
            }
            OutlinedButton(
                onClick = onCrearCuenta,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Text(stringResource(R.string.login_crear_cuenta), style = MaterialTheme.typography.titleMedium)
            }
            TextButton(onClick = onVolverInicio, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(stringResource(R.string.comun_volver_inicio), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun LoginScreenPreview() {
    ReciclaKidsTheme {
        LoginScreen(onEntrar = { _, _ -> }, onOlvideContrasena = {}, onCrearCuenta = {}, onVolverInicio = {})
    }
}
