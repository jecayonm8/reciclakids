package com.reciclakids.ui.docente

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reciclakids.R
import com.reciclakids.model.CategoriaResiduo
import com.reciclakids.model.Dificultad
import com.reciclakids.model.EstadoReto
import com.reciclakids.model.Reto
import com.reciclakids.model.TableroDocente
import com.reciclakids.ui.theme.ReciclaKidsTheme
import com.reciclakids.viewmodel.EstadoUi
import com.reciclakids.viewmodel.datosCargados
import java.time.LocalDate

/**
 * UI-21 Tablero de inicio. En cinco segundos se sabe si hay reto hoy, cuál es el código y
 * cómo va el grupo; reutilizar un reto anterior está siempre a la vista.
 */
@Composable
fun TableroDocenteScreen(
    estado: EstadoUi<TableroDocente>,
    onReintentar: () -> Unit,
    onCrear: () -> Unit,
    onReutilizar: () -> Unit,
    onMostrarCodigo: (retoId: String) -> Unit,
    onVerGrupo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val datos = estado.datosCargados
    MarcoDocente(
        modifier = modifier,
        barra = {
            if (datos != null) {
                BarraDocente(
                    titulo = stringResource(R.string.tablero_hola, primerNombre(datos.nombreDocente)),
                    subtitulo = stringResource(R.string.tablero_grupo_hoy, datos.grupo),
                )
            } else {
                BarraDocente(titulo = stringResource(R.string.docente_tab_inicio))
            }
        },
        botonFlotante = {
            if (estado is EstadoUi.Contenido) {
                ExtendedFloatingActionButton(
                    onClick = onCrear,
                    icon = { Icon(painterResource(R.drawable.ic_mas), contentDescription = null) },
                    text = { Text(stringResource(R.string.tablero_crear), style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp)) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        },
    ) { relleno ->
        when (estado) {
            EstadoUi.Cargando -> EsqueletoCarga(Modifier.padding(relleno))
            EstadoUi.Error -> EstadoErrorRed(onReintentar, Modifier.padding(relleno))
            is EstadoUi.Vacio -> EstadoVacioDocente(
                ilustracion = stringResource(R.string.tablero_vacio_ilustracion),
                titulo = stringResource(R.string.tablero_vacio_titulo),
                texto = stringResource(R.string.tablero_vacio_texto),
                modifier = Modifier.padding(relleno),
                flotante = true,
            ) {
                BotonLleno(stringResource(R.string.tablero_vacio_crear), onClick = onCrear, modifier = Modifier.fillMaxWidth())
                BotonContorno(stringResource(R.string.tablero_vacio_reutilizar), onClick = onReutilizar, modifier = Modifier.fillMaxWidth())
            }
            is EstadoUi.Contenido -> ContenidoTablero(estado.datos, relleno, onReutilizar, onMostrarCodigo, onVerGrupo)
        }
    }
}

@Composable
private fun ContenidoTablero(
    datos: TableroDocente,
    relleno: PaddingValues,
    onReutilizar: () -> Unit,
    onMostrarCodigo: (String) -> Unit,
    onVerGrupo: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(relleno)
            .verticalScroll(rememberScrollState())
            // Abajo queda espacio para que el botón «Crear» no tape la última tarjeta.
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        datos.retoHoy?.let { reto ->
            reto.codigoActivo(datos.hoy)?.let { codigo ->
                TarjetaCodigoActivo(codigo, onClick = { onMostrarCodigo(reto.id) })
            }
            TarjetaRetoHoy(reto, onMostrarCodigo = { onMostrarCodigo(reto.id) })
        }
        TarjetaResumenGrupo(datos, onClick = onVerGrupo)
        BotonContorno(stringResource(R.string.tablero_reutilizar), onClick = onReutilizar, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun TarjetaCodigoActivo(codigo: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = FormaTarjeta,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.tablero_codigo_activo), style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp))
            FilaDigitos(
                codigo = codigo,
                ancho = 52.dp,
                alto = 60.dp,
                tamanoTexto = 30.sp,
                radioCuadrado = 14.dp,
                puntaGota = 8.dp,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                stringResource(R.string.codigo_vence_hoy),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun TarjetaRetoHoy(reto: Reto, onMostrarCodigo: () -> Unit) {
    TarjetaDocente(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(reto.nombre, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            ChipEstadoReto(reto.estado)
        }
        Text(
            stringResource(R.string.reto_resumen_corto, reto.dificultad.resumen(), textoResiduos(reto.dificultad)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        BotonLleno(stringResource(R.string.tablero_mostrar_codigo), onClick = onMostrarCodigo, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun TarjetaResumenGrupo(datos: TableroDocente, onClick: () -> Unit) {
    TarjetaDocente(Modifier.fillMaxWidth(), onClick = onClick) {
        Text(stringResource(R.string.tablero_resumen), style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(datos.completaron.toString(), style = CifraGrande, modifier = Modifier.alignByBaseline())
            Text(
                stringResource(R.string.tablero_completaron, datos.totalNinos),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alignByBaseline(),
            )
        }
        BarraProgreso(if (datos.totalNinos == 0) 0f else datos.completaron / datos.totalNinos.toFloat())
        val aciertos = datos.aciertosPromedio
        if (aciertos != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    textoPorcentaje(aciertos),
                    style = CifraGrande,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.alignByBaseline(),
                )
                Text(
                    stringResource(R.string.tablero_aciertos),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.alignByBaseline(),
                )
            }
        } else {
            Text(
                stringResource(R.string.tablero_aciertos_pendientes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val CifraGrande = TextStyle(fontSize = 30.sp, lineHeight = 36.sp)

/** «Laura Restrepo» → «Laura». */
internal fun primerNombre(nombre: String): String = nombre.trim().substringBefore(' ')

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun TableroDocentePreview() {
    val hoy = LocalDate.of(2026, 9, 22)
    ReciclaKidsTheme {
        TableroDocenteScreen(
            estado = EstadoUi.Contenido(
                TableroDocente(
                    hoy = hoy,
                    nombreDocente = "Laura Restrepo",
                    grupo = "Jardín B",
                    retoHoy = Reto(
                        "r1", "Clasificar la lonchera", setOf(CategoriaResiduo.Aprovechables, CategoriaResiduo.Organicos),
                        Dificultad.Medio, hoy, EstadoReto.Publicado, codigo = "4729",
                    ),
                    completaron = 18,
                    totalNinos = 22,
                    aciertosPromedio = 91,
                )
            ),
            onReintentar = {}, onCrear = {}, onReutilizar = {}, onMostrarCodigo = {}, onVerGrupo = {},
        )
    }
}
