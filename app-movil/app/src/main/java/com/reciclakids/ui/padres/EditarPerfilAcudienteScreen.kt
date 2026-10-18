package com.reciclakids.ui.padres

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import com.reciclakids.R
import com.reciclakids.network.DatosPerfilAcudiente
import com.reciclakids.ui.acceso.CampoCorreo
import com.reciclakids.ui.acceso.CampoTexto
import com.reciclakids.ui.docente.MarcoEditarPerfil
import com.reciclakids.ui.docente.errorCorreoPerfil
import com.reciclakids.ui.theme.ReciclaKidsTheme
import com.reciclakids.viewmodel.EstadoGuardado
import com.reciclakids.viewmodel.EstadoUi
import com.reciclakids.viewmodel.FormularioPerfilAcudiente

/** Editar el perfil del acudiente desde UI-35: nombre y correo. La barra marca la pestaña Cuenta. */
@Composable
fun EditarPerfilAcudienteScreen(
    estado: EstadoUi<FormularioPerfilAcudiente>,
    guardado: EstadoGuardado,
    onGuardar: () -> Unit,
    onReintentar: () -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MarcoEditarPerfil(
        estado, guardado, onGuardar, onReintentar, onVolver, modifier,
        tituloError = stringResource(R.string.padres_error_titulo),
        textoError = stringResource(R.string.padres_error_texto),
    ) { formulario, habilitado ->
        CampoTexto(
            formulario.nombre, { formulario.nombre = it }, stringResource(R.string.registro_tu_nombre),
            error = if (formulario.nombre.isBlank()) stringResource(R.string.error_campo_obligatorio) else null,
            teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            habilitado = habilitado,
        )
        CampoCorreo(
            formulario.correo, { formulario.correo = it },
            error = errorCorreoPerfil(formulario, guardado),
            ayuda = stringResource(R.string.editar_perfil_correo_ayuda_acudiente),
            imeAction = ImeAction.Done,
            habilitado = habilitado,
        )
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun EditarPerfilAcudientePreview() {
    ReciclaKidsTheme {
        EditarPerfilAcudienteScreen(
            estado = EstadoUi.Contenido(FormularioPerfilAcudiente(DatosPerfilAcudiente("Mariana Ríos", "mariana.r@correo.com"))),
            guardado = EstadoGuardado.Editando,
            onGuardar = {}, onReintentar = {}, onVolver = {},
        )
    }
}
