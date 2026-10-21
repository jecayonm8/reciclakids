package com.reciclakids.ui.docente

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reciclakids.R
import com.reciclakids.model.NinoAgregado
import com.reciclakids.ui.acceso.AvisoError
import com.reciclakids.ui.acceso.CampoTexto
import com.reciclakids.ui.theme.ReciclaKidsTheme
import com.reciclakids.viewmodel.EstadoAgregarNino
import com.reciclakids.viewmodel.FormularioNino

/**
 * Agregar un niño al grupo desde UI-25, a pantalla completa. Solo se escriben su nombre y la
 * inicial del apellido (Ley 1581 de 2012); al agregarlo aparece su código de vinculación para
 * compartirlo con su acudiente.
 */
@Composable
fun AgregarNinoScreen(
    formulario: FormularioNino,
    estado: EstadoAgregarNino,
    onGuardar: () -> Unit,
    onCompartir: (NinoAgregado) -> Unit,
    onOtro: () -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MarcoDocente(
        modifier = modifier,
        pantallaCompleta = true,
        barra = { BarraDocente(titulo = stringResource(R.string.agregar_nino), onAtras = onVolver) },
    ) { relleno ->
        when (estado) {
            is EstadoAgregarNino.Agregado -> ContenidoAgregado(estado.nino, onCompartir, onOtro, onVolver, Modifier.padding(relleno))
            else -> FormularioAgregar(formulario, estado, onGuardar, Modifier.padding(relleno))
        }
    }
}

@Composable
private fun FormularioAgregar(formulario: FormularioNino, estado: EstadoAgregarNino, onGuardar: () -> Unit, modifier: Modifier) {
    val guardando = estado == EstadoAgregarNino.Guardando
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            stringResource(R.string.agregar_nino_texto),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        CampoTexto(
            formulario.nombre, { formulario.nombre = it }, stringResource(R.string.agregar_nino_nombre),
            teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            habilitado = !guardando,
        )
        CampoTexto(
            formulario.inicial, formulario::escribirInicial, stringResource(R.string.agregar_nino_inicial),
            ayuda = stringResource(R.string.agregar_nino_inicial_ayuda),
            teclado = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Done,
            ),
            habilitado = !guardando,
        )
        if (formulario.valido) {
            Row(
                modifier = Modifier.semantics(mergeDescendants = true) { },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AvatarNino(Modifier.size(40.dp), formulario.nombreGuardado)
                Text(
                    stringResource(R.string.agregar_nino_vista_previa, formulario.nombreGuardado),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        if (estado == EstadoAgregarNino.SinConexion) AvisoError(stringResource(R.string.error_sin_conexion))
        BotonLleno(
            stringResource(R.string.agregar_nino_guardar),
            onClick = onGuardar,
            modifier = Modifier.fillMaxWidth(),
            // Mientras guarda sigue habilitado para que el progreso se vea sobre el color principal.
            habilitado = guardando || formulario.valido,
            cargando = guardando,
        )
    }
}

@Composable
private fun ContenidoAgregado(
    nino: NinoAgregado,
    onCompartir: (NinoAgregado) -> Unit,
    onOtro: () -> Unit,
    onListo: () -> Unit,
    modifier: Modifier,
) {
    PantallaCentrada(modifier, relleno = 24.dp, espacio = 16.dp) {
        Box(
            Modifier.size(88.dp).background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(R.drawable.ic_check),
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
        Text(
            stringResource(R.string.agregar_nino_listo, nino.nombre),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.vinculacion_sin_vincular_texto, nino.nombre),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        TextoCodigoVinculacion(nino.codigoVinculacion, tamanoTexto = 30.sp)
        BotonLleno(stringResource(R.string.agregar_nino_compartir), onClick = { onCompartir(nino) }, modifier = Modifier.fillMaxWidth())
        BotonContorno(stringResource(R.string.agregar_nino_otro), onClick = onOtro, modifier = Modifier.fillMaxWidth())
        BotonTexto(stringResource(R.string.agregar_nino_terminar), onClick = onListo, modifier = Modifier.fillMaxWidth())
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun AgregarNinoPreview() {
    ReciclaKidsTheme {
        AgregarNinoScreen(
            formulario = FormularioNino().apply {
                nombre = "Salomé"
                escribirInicial("m")
            },
            estado = EstadoAgregarNino.Editando,
            onGuardar = {}, onCompartir = {}, onOtro = {}, onVolver = {},
        )
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun NinoAgregadoPreview() {
    ReciclaKidsTheme {
        AgregarNinoScreen(
            formulario = FormularioNino(),
            estado = EstadoAgregarNino.Agregado(NinoAgregado("nino-11", "Salomé M.", "JB-2M91")),
            onGuardar = {}, onCompartir = {}, onOtro = {}, onVolver = {},
        )
    }
}
