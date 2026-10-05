package com.reciclakids.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.reciclakids.R

/**
 * Destino provisional de una ruta cuya pantalla todavía no existe. Se borra cuando el
 * grafo correspondiente (nino, docente, padres) quede conectado.
 */
@Composable
fun PantallaPendiente(
    descripcion: String,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    etiquetaVolver: String = stringResource(R.string.comun_volver_inicio),
) {
    Scaffold(modifier = modifier) { relleno ->
        Column(
            modifier = Modifier.fillMaxSize().padding(relleno).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.pendiente_titulo), style = MaterialTheme.typography.headlineMedium)
            Text(
                descripcion,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            OutlinedButton(onClick = onVolver) { Text(etiquetaVolver) }
        }
    }
}
