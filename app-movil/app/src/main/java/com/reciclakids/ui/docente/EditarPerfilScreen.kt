package com.reciclakids.ui.docente

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.reciclakids.R
import com.reciclakids.network.DatosPerfilDocente
import com.reciclakids.ui.acceso.AvisoError
import com.reciclakids.ui.acceso.CampoCorreo
import com.reciclakids.ui.acceso.CampoTexto
import com.reciclakids.ui.theme.ReciclaKidsTheme
import com.reciclakids.util.correoValido
import com.reciclakids.viewmodel.EstadoGuardado
import com.reciclakids.viewmodel.EstadoUi
import com.reciclakids.viewmodel.FormularioPerfil
import com.reciclakids.viewmodel.FormularioPerfilDocente
import com.reciclakids.viewmodel.correoEnUso
import com.reciclakids.viewmodel.datosCargados
import com.reciclakids.viewmodel.listoParaGuardar
import com.reciclakids.viewmodel.ocupado

/** Editar el perfil de la docente desde UI-28: nombre, correo, jardín y grupo. Va a pantalla completa. */
@Composable
fun EditarPerfilDocenteScreen(
    estado: EstadoUi<FormularioPerfilDocente>,
    guardado: EstadoGuardado,
    onGuardar: () -> Unit,
    onReintentar: () -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MarcoEditarPerfil(estado, guardado, onGuardar, onReintentar, onVolver, modifier, pantallaCompleta = true) { formulario, habilitado ->
        val obligatorio = stringResource(R.string.error_campo_obligatorio)
        fun falta(valor: String) = if (valor.isBlank()) obligatorio else null

        TituloCampos(stringResource(R.string.editar_perfil_tus_datos))
        CampoTexto(
            formulario.nombre, { formulario.nombre = it }, stringResource(R.string.registro_nombre_apellido),
            error = falta(formulario.nombre),
            teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            habilitado = habilitado,
        )
        CampoCorreo(
            formulario.correo, { formulario.correo = it },
            etiqueta = stringResource(R.string.registro_correo_institucional),
            error = errorCorreoPerfil(formulario, guardado),
            ayuda = stringResource(R.string.editar_perfil_correo_ayuda),
            habilitado = habilitado,
        )
        TituloCampos(stringResource(R.string.editar_perfil_jardin_grupo))
        CampoTexto(
            formulario.jardin, { formulario.jardin = it }, stringResource(R.string.registro_jardin),
            error = falta(formulario.jardin),
            habilitado = habilitado,
        )
        CampoTexto(
            formulario.grupo, { formulario.grupo = it }, stringResource(R.string.registro_grupo),
            error = falta(formulario.grupo),
            teclado = KeyboardOptions(imeAction = ImeAction.Done),
            habilitado = habilitado,
        )
    }
}

/**
 * Lo común a editar el perfil de la docente y del acudiente: carga, error de red, «Guardar
 * cambios» deshabilitado si no hay cambios o hay errores, aviso sin conexión y confirmación antes
 * de descartar lo escrito. Los errores se ven al escribir: si no, no se sabría por qué no se
 * puede guardar.
 */
@Composable
internal fun <F : FormularioPerfil<*>> MarcoEditarPerfil(
    estado: EstadoUi<F>,
    guardado: EstadoGuardado,
    onGuardar: () -> Unit,
    onReintentar: () -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    pantallaCompleta: Boolean = false,
    tituloError: String = stringResource(R.string.docente_error_titulo),
    textoError: String = stringResource(R.string.docente_error_texto),
    campos: @Composable ColumnScope.(formulario: F, habilitado: Boolean) -> Unit,
) {
    var confirmarDescartar by rememberSaveable { mutableStateOf(false) }
    val hayCambios = estado.datosCargados?.hayCambios == true && !guardado.ocupado
    val volver = { if (hayCambios) confirmarDescartar = true else onVolver() }
    BackHandler(enabled = hayCambios) { confirmarDescartar = true }

    MarcoDocente(
        modifier = modifier,
        pantallaCompleta = pantallaCompleta,
        barra = { BarraDocente(titulo = stringResource(R.string.cuenta_editar_perfil), onAtras = volver) },
    ) { relleno ->
        val formulario = estado.datosCargados
        when {
            formulario != null -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(relleno)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AvatarIniciales(formulario.nombre, Modifier.align(Alignment.CenterHorizontally))
                campos(formulario, !guardado.ocupado)
                if (guardado == EstadoGuardado.SinConexion) AvisoError(stringResource(R.string.error_sin_conexion))
                BotonLleno(
                    stringResource(R.string.editar_perfil_guardar),
                    onClick = onGuardar,
                    modifier = Modifier.fillMaxWidth(),
                    // Mientras guarda sigue habilitado para que el progreso se vea sobre el color principal.
                    habilitado = guardado.ocupado || formulario.listoParaGuardar(guardado),
                    cargando = guardado.ocupado,
                )
            }
            estado == EstadoUi.Error -> EstadoErrorRed(onReintentar, Modifier.padding(relleno), tituloError, textoError)
            else -> EsqueletoCarga(Modifier.padding(relleno))
        }
    }
    if (confirmarDescartar) {
        AlertDialog(
            onDismissRequest = { confirmarDescartar = false },
            title = { Text(stringResource(R.string.editar_perfil_descartar_titulo)) },
            text = {
                Text(
                    stringResource(R.string.editar_perfil_descartar_texto),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmarDescartar = false
                    onVolver()
                }) { Text(stringResource(R.string.editar_perfil_descartar)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmarDescartar = false }) { Text(stringResource(R.string.editar_perfil_seguir)) }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        )
    }
}

/** El correo en uso por otra cuenta pesa más que el formato: ese correo sí era válido. */
@Composable
internal fun errorCorreoPerfil(formulario: FormularioPerfil<*>, guardado: EstadoGuardado): String? = when {
    formulario.correoEnUso(guardado) -> stringResource(R.string.error_correo_en_uso)
    !correoValido(formulario.correo) -> stringResource(R.string.error_correo)
    else -> null
}

@Composable
internal fun TituloCampos(texto: String) {
    Text(texto, modifier = Modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium)
}

/** Las iniciales cambian mientras se escribe el nombre, como se verán en la cuenta. */
@Composable
private fun AvatarIniciales(nombre: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(72.dp)
            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Text(iniciales(nombre), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun EditarPerfilDocentePreview() {
    ReciclaKidsTheme {
        EditarPerfilDocenteScreen(
            estado = EstadoUi.Contenido(
                FormularioPerfilDocente(DatosPerfilDocente("Laura Restrepo", "laura.r@jardin.edu.co", "Gotitas", "Jardín B"))
            ),
            guardado = EstadoGuardado.Editando,
            onGuardar = {}, onReintentar = {}, onVolver = {},
        )
    }
}
