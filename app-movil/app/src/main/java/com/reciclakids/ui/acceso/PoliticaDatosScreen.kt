package com.reciclakids.ui.acceso

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reciclakids.R
import com.reciclakids.ui.docente.FormaTarjeta
import com.reciclakids.ui.docente.TarjetaDocente
import com.reciclakids.ui.theme.ReciclaKidsTheme

/** Marca de viñeta dentro de los textos de la política. */
private const val Vineta = "• "

/**
 * Política de tratamiento de datos personales (Ley 1581 de 2012). Se abre desde el registro
 * (UI-04/05), la autorización de datos del menor (UI-07), la cuenta de la docente (UI-28) y la
 * privacidad de padres (UI-34).
 *
 * Todo el texto vive en strings.xml como un borrador que el equipo debe validar: el resumen y las
 * secciones son arreglos, así que el texto legal definitivo lo reemplaza, con más o menos
 * secciones, sin tocar esta pantalla.
 */
@Composable
fun PoliticaDatosScreen(onAtras: () -> Unit, modifier: Modifier = Modifier) {
    val titulos = stringArrayResource(R.array.politica_secciones_titulos)
    val textos = stringArrayResource(R.array.politica_secciones_textos)
    val cuerpo = MaterialTheme.typography.bodyLarge

    Scaffold(
        modifier = modifier,
        topBar = { BarraSuperiorAcceso(stringResource(R.string.politica_barra), onAtras) },
    ) { relleno ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.politica_titulo),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    stringResource(R.string.politica_version),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(stringResource(R.string.politica_intro), style = cuerpo, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TarjetaResumen(stringArrayResource(R.array.politica_resumen).toList())
            titulos.zip(textos).forEach { (titulo, texto) -> Seccion(titulo, texto, cuerpo) }
            TarjetaContacto()
        }
    }
}

@Composable
private fun TarjetaResumen(puntos: List<String>) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = FormaTarjeta, color = MaterialTheme.colorScheme.tertiaryContainer) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val tinta = MaterialTheme.colorScheme.onTertiaryContainer
            Text(
                stringResource(R.string.politica_resumen_titulo),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
                color = tinta,
            )
            puntos.forEach { punto ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(
                        painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        modifier = Modifier.padding(top = 2.dp).size(20.dp),
                        tint = tinta,
                    )
                    Text(punto, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 22.sp), color = tinta)
                }
            }
        }
    }
}

/** Una sección: los bloques van separados por una línea en blanco y las líneas que empiezan con «• » son viñetas. */
@Composable
private fun Seccion(titulo: String, texto: String, cuerpo: TextStyle) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            titulo,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp, lineHeight = 24.sp),
        )
        texto.split("\n\n").filter { it.isNotBlank() }.forEach { bloque ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                bloque.lines().filter { it.isNotBlank() }.forEach { linea ->
                    if (linea.startsWith(Vineta)) {
                        LineaVineta(linea.removePrefix(Vineta), cuerpo)
                    } else {
                        Text(linea, style = cuerpo, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/** La viñeta va en la línea base del texto, así que sigue en su sitio con la fuente al 200 %. */
@Composable
private fun LineaVineta(texto: String, cuerpo: TextStyle) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Row(Modifier.padding(start = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("•", modifier = Modifier.alignByBaseline(), style = cuerpo, color = color)
        Text(texto, modifier = Modifier.weight(1f).alignByBaseline(), style = cuerpo, color = color)
    }
}

/** Datos de contacto que exige el Decreto 1377 de 2013. Mientras no existan, son marcadores. */
@Composable
private fun TarjetaContacto() {
    TarjetaDocente(Modifier.fillMaxWidth(), espacio = 14.dp) {
        Text(
            stringResource(R.string.politica_contacto_titulo),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
        )
        DatoContacto(stringResource(R.string.politica_contacto_responsable_etiqueta), stringResource(R.string.politica_contacto_responsable))
        DatoContacto(stringResource(R.string.politica_contacto_domicilio_etiqueta), stringResource(R.string.politica_contacto_domicilio))
        DatoContacto(stringResource(R.string.politica_contacto_correo_etiqueta), stringResource(R.string.politica_contacto_correo))
        DatoContacto(stringResource(R.string.politica_contacto_telefono_etiqueta), stringResource(R.string.politica_contacto_telefono))
    }
}

/** Etiqueta encima del valor: un correo largo no cabe al lado de su etiqueta a 360 dp. */
@Composable
private fun DatoContacto(etiqueta: String, valor: String) {
    Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) { }) {
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.bodyLarge)
    }
}

@Preview(widthDp = 360, heightDp = 2600)
@Composable
private fun PoliticaDatosPreview() {
    ReciclaKidsTheme { PoliticaDatosScreen(onAtras = {}) }
}
