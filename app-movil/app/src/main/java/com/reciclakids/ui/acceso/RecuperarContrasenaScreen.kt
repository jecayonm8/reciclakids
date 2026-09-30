package com.reciclakids.ui.acceso

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reciclakids.R
import com.reciclakids.ui.juego.EstiloMarcador
import com.reciclakids.ui.juego.MarcadorIlustracion
import com.reciclakids.ui.theme.ReciclaKidsTheme

/**
 * UI-06 Recuperar contraseña. El aviso de envío es el mismo exista o no la cuenta, para no
 * revelar qué correos están registrados.
 */
@Composable
fun RecuperarContrasenaScreen(
    onAtras: () -> Unit,
    onEnviar: (correo: String) -> Unit,
    modifier: Modifier = Modifier,
    enviando: Boolean = false,
    enviado: Boolean = false,
    sinConexion: Boolean = false,
) {
    var correo by rememberSaveable { mutableStateOf("") }
    var intentoEnviar by rememberSaveable { mutableStateOf(false) }
    val correoOk = correoValido(correo)

    Scaffold(
        modifier = modifier,
        topBar = { BarraSuperiorAcceso(stringResource(R.string.recuperar_titulo), onAtras) },
    ) { relleno ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MarcadorIlustracion("pez cartero\nilustración", Modifier.size(170.dp), estilo = EstiloMarcador.Adulto)
            Text(
                stringResource(R.string.recuperar_texto),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CampoCorreo(
                valor = correo,
                onCambio = { correo = it },
                error = if (intentoEnviar && !correoOk) stringResource(R.string.error_correo) else null,
                imeAction = ImeAction.Done,
            )
            BotonPrincipal(
                texto = stringResource(R.string.recuperar_enviar),
                onClick = {
                    intentoEnviar = true
                    if (correoOk) onEnviar(correo.trim())
                },
                cargando = enviando,
            )
            if (sinConexion) AvisoError(stringResource(R.string.error_sin_conexion))
            if (enviado) AvisoExito(stringResource(R.string.recuperar_enviado))
        }
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun RecuperarContrasenaScreenPreview() {
    ReciclaKidsTheme { RecuperarContrasenaScreen(onAtras = {}, onEnviar = {}, enviado = true) }
}
