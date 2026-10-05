package com.reciclakids.ui.acceso

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reciclakids.R
import com.reciclakids.model.RolAdulto
import com.reciclakids.ui.theme.ReciclaKidsTheme
import com.reciclakids.util.contrasenaValida
import com.reciclakids.util.correoValido
import com.reciclakids.viewmodel.FormularioAcudiente
import com.reciclakids.viewmodel.FormularioDocente

/** Por qué no se pudo crear la cuenta. */
enum class ErrorRegistro { CorreoEnUso, SinConexion }

/**
 * UI-04 Registro de docente y UI-05 Registro de acudiente (paso 1 de 2), con pestañas de rol
 * en la misma pantalla. El acudiente sigue a la autorización de datos del menor (UI-07).
 */
@Composable
fun RegistroScreen(
    rol: RolAdulto,
    onCambiarRol: (RolAdulto) -> Unit,
    docente: FormularioDocente,
    acudiente: FormularioAcudiente,
    onAtras: () -> Unit,
    onCrearDocente: () -> Unit,
    onContinuarAcudiente: () -> Unit,
    onLeerPolitica: () -> Unit,
    modifier: Modifier = Modifier,
    cargando: Boolean = false,
    error: ErrorRegistro? = null,
) {
    Scaffold(
        modifier = modifier,
        topBar = { BarraSuperiorAcceso(stringResource(R.string.registro_titulo), onAtras) },
    ) { relleno ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SelectorRol(rol, onCambiarRol)
            if (error == ErrorRegistro.SinConexion) AvisoError(stringResource(R.string.error_sin_conexion))
            val correoEnUso = if (error == ErrorRegistro.CorreoEnUso) stringResource(R.string.error_correo_en_uso) else null
            when (rol) {
                RolAdulto.Docente -> CamposDocente(docente, correoEnUso, cargando, onCrearDocente, onLeerPolitica)
                RolAdulto.Acudiente -> CamposAcudiente(acudiente, correoEnUso, onContinuarAcudiente)
            }
        }
    }
}

@Composable
private fun SelectorRol(rol: RolAdulto, onCambiarRol: (RolAdulto) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
            .padding(4.dp)
            .selectableGroup(),
    ) {
        listOf(
            RolAdulto.Docente to R.string.registro_rol_docente,
            RolAdulto.Acudiente to R.string.registro_rol_acudiente,
        ).forEach { (opcion, etiqueta) ->
            val activa = opcion == rol
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(CircleShape)
                    .background(if (activa) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer)
                    .selectable(selected = activa, role = Role.Tab, onClick = { onCambiarRol(opcion) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(etiqueta),
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                    color = if (activa) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CamposDocente(
    formulario: FormularioDocente,
    correoEnUso: String?,
    cargando: Boolean,
    onCrear: () -> Unit,
    onLeerPolitica: () -> Unit,
) {
    var intentoEnviar by remember { mutableStateOf(false) }
    val obligatorio = stringResource(R.string.error_campo_obligatorio)
    fun falta(valor: String) = if (intentoEnviar && valor.isBlank()) obligatorio else null

    CampoTexto(
        formulario.nombre, { formulario.nombre = it }, stringResource(R.string.registro_nombre_apellido),
        error = falta(formulario.nombre),
        teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
    )
    CampoCorreo(
        formulario.correo, { formulario.correo = it },
        etiqueta = stringResource(R.string.registro_correo_institucional),
        error = correoEnUso ?: errorCorreo(formulario.correo, intentoEnviar),
    )
    CampoTexto(formulario.jardin, { formulario.jardin = it }, stringResource(R.string.registro_jardin), error = falta(formulario.jardin))
    CampoTexto(formulario.grupo, { formulario.grupo = it }, stringResource(R.string.registro_grupo), error = falta(formulario.grupo))
    CampoContrasena(
        formulario.contrasena, { formulario.contrasena = it },
        error = errorContrasena(formulario.contrasena, intentoEnviar),
    )
    CasillaConsentimiento(formulario.aceptaTerminos, { formulario.aceptaTerminos = it }) {
        Text(
            textoConEnlace(
                stringResource(R.string.registro_terminos),
                stringResource(R.string.registro_terminos_enlace),
                onLeerPolitica,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    BotonPrincipal(
        texto = stringResource(R.string.registro_crear_docente),
        onClick = {
            intentoEnviar = true
            if (formulario.completo) onCrear()
        },
        habilitado = formulario.aceptaTerminos,
        cargando = cargando,
    )
}

@Composable
private fun CamposAcudiente(formulario: FormularioAcudiente, correoEnUso: String?, onContinuar: () -> Unit) {
    var intentoEnviar by remember { mutableStateOf(false) }
    val obligatorio = stringResource(R.string.error_campo_obligatorio)
    fun falta(valor: String) = if (intentoEnviar && valor.isBlank()) obligatorio else null

    PasoProgreso(paso = 1, total = 2)
    CampoTexto(
        formulario.nombre, { formulario.nombre = it }, stringResource(R.string.registro_tu_nombre),
        error = falta(formulario.nombre),
        teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
    )
    CampoCorreo(
        formulario.correo, { formulario.correo = it },
        error = correoEnUso ?: errorCorreo(formulario.correo, intentoEnviar),
    )
    CampoContrasena(
        formulario.contrasena, { formulario.contrasena = it },
        error = errorContrasena(formulario.contrasena, intentoEnviar),
        imeAction = ImeAction.Next,
    )
    CampoTexto(
        formulario.codigoVinculacion, { formulario.codigoVinculacion = it.uppercase() },
        stringResource(R.string.registro_codigo_vinculacion),
        error = falta(formulario.codigoVinculacion),
        ayuda = stringResource(R.string.registro_codigo_ayuda),
        teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false, imeAction = ImeAction.Done),
        estiloTexto = MaterialTheme.typography.bodyLarge.copy(letterSpacing = 2.2.sp),
    )
    BotonPrincipal(
        texto = stringResource(R.string.registro_continuar),
        onClick = {
            intentoEnviar = true
            if (formulario.completo) onContinuar()
        },
    )
}

@Composable
private fun errorCorreo(correo: String, intentoEnviar: Boolean): String? =
    if (intentoEnviar && !correoValido(correo)) stringResource(R.string.error_correo) else null

@Composable
private fun errorContrasena(contrasena: String, intentoEnviar: Boolean): String? =
    if (intentoEnviar && !contrasenaValida(contrasena)) stringResource(R.string.error_contrasena_corta) else null

/** Marca [enlace] dentro de [texto] como enlace que llama a [onClick]. */
@Composable
internal fun textoConEnlace(texto: String, enlace: String, onClick: () -> Unit): AnnotatedString {
    val inicio = texto.indexOf(enlace)
    if (inicio < 0) return AnnotatedString(texto)
    val estilo = TextLinkStyles(SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline))
    return buildAnnotatedString {
        append(texto)
        addLink(LinkAnnotation.Clickable("politica", estilo) { onClick() }, inicio, inicio + enlace.length)
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun RegistroDocentePreview() {
    ReciclaKidsTheme {
        RegistroScreen(
            rol = RolAdulto.Docente, onCambiarRol = {}, docente = FormularioDocente(), acudiente = FormularioAcudiente(),
            onAtras = {}, onCrearDocente = {}, onContinuarAcudiente = {}, onLeerPolitica = {},
        )
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun RegistroAcudientePreview() {
    ReciclaKidsTheme {
        RegistroScreen(
            rol = RolAdulto.Acudiente, onCambiarRol = {}, docente = FormularioDocente(), acudiente = FormularioAcudiente(),
            onAtras = {}, onCrearDocente = {}, onContinuarAcudiente = {}, onLeerPolitica = {},
        )
    }
}
