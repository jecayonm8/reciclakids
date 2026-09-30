package com.reciclakids.ui.acceso

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reciclakids.R
import com.reciclakids.ui.theme.ReciclaKidsTheme

/**
 * UI-07 Autorización de datos del menor (paso 2 de 2 del registro de acudiente).
 * Ley 1581 de 2012: la casilla es explícita y nunca viene premarcada, y «Crear cuenta»
 * queda deshabilitado hasta marcarla.
 */
@Composable
fun AutorizacionDatosMenorScreen(
    onAtras: () -> Unit,
    onCrearCuenta: () -> Unit,
    onLeerPolitica: () -> Unit,
    modifier: Modifier = Modifier,
    cargando: Boolean = false,
    error: ErrorRegistro? = null,
) {
    var autoriza by rememberSaveable { mutableStateOf(false) }
    val cuerpo = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 23.sp)

    Scaffold(
        modifier = modifier,
        topBar = { BarraSuperiorAcceso(stringResource(R.string.autorizacion_titulo), onAtras) },
    ) { relleno ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PasoProgreso(paso = 2, total = 2)
            Text(stringResource(R.string.autorizacion_encabezado), style = MaterialTheme.typography.titleLarge)
            Text(
                AnnotatedString.fromHtml(stringResource(R.string.autorizacion_texto)),
                style = cuerpo,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(R.string.autorizacion_punto_1, R.string.autorizacion_punto_2, R.string.autorizacion_punto_3).forEach { punto ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            Modifier
                                .padding(top = 9.dp)
                                .size(5.dp)
                                .background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
                        )
                        Text(stringResource(punto), style = cuerpo, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            TextButton(onClick = onLeerPolitica, contentPadding = PaddingValues(horizontal = 4.dp)) {
                Text(stringResource(R.string.autorizacion_politica), style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp))
            }
            CasillaConsentimiento(autoriza, { autoriza = it }, relleno = PaddingValues(16.dp)) {
                Text(stringResource(R.string.autorizacion_casilla), style = cuerpo.copy(lineHeight = 22.sp))
            }
            when (error) {
                ErrorRegistro.SinConexion -> AvisoError(stringResource(R.string.error_sin_conexion))
                ErrorRegistro.CorreoEnUso -> AvisoError(stringResource(R.string.error_correo_en_uso))
                null -> Unit
            }
            BotonPrincipal(
                texto = stringResource(R.string.autorizacion_crear),
                onClick = onCrearCuenta,
                habilitado = autoriza,
                cargando = cargando,
            )
            Text(
                stringResource(R.string.autorizacion_nota),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun AutorizacionDatosMenorScreenPreview() {
    ReciclaKidsTheme { AutorizacionDatosMenorScreen(onAtras = {}, onCrearCuenta = {}, onLeerPolitica = {}) }
}
