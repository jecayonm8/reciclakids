package com.reciclakids.ui.docente

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reciclakids.R
import com.reciclakids.model.CategoriaResiduo
import com.reciclakids.model.Dificultad
import com.reciclakids.model.EstadoReto
import com.reciclakids.model.RetoDocente
import com.reciclakids.ui.common.FiguraDigito
import com.reciclakids.ui.theme.BalooDos
import com.reciclakids.ui.theme.EscalaFijaNino
import com.reciclakids.ui.theme.ReciclaKidsColors
import com.reciclakids.ui.theme.ReciclaKidsTheme
import com.reciclakids.viewmodel.EstadoUi
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private val FondoInicio = Color(0xFF00687F)
private val FondoFin = Color(0xFF0C5C73)

/**
 * UI-24 Reto publicado · código, a pantalla completa para girar el teléfono hacia el grupo o
 * copiarlo en el tablero. Los dígitos gigantes (140 × 170 dp, rejilla 2 × 2) usan los mismos
 * colores y formas que el teclado del niño. Sin sonido: el aula ya es ruidosa.
 */
@Composable
fun CodigoPublicadoScreen(
    estado: EstadoUi<RetoDocente?>,
    hoy: LocalDate,
    ninosQueEntraron: Int?,
    onVolver: () -> Unit,
    onReintentar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reto = if (estado is EstadoUi.Contenido) estado.datos else null
    val codigo = reto?.codigoActivo(hoy)
    if (reto != null && codigo != null) {
        PantallaCodigo(reto, codigo, ninosQueEntraron, onVolver, modifier)
        return
    }
    MarcoDocente(
        modifier = modifier,
        pantallaCompleta = true,
        barra = { BarraDocente(titulo = stringResource(R.string.codigo_barra), onAtras = onVolver) },
    ) { relleno ->
        when (estado) {
            EstadoUi.Error -> EstadoErrorRed(onReintentar, Modifier.padding(relleno))
            EstadoUi.Cargando -> Box(Modifier.fillMaxSize().padding(relleno), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            else -> EstadoVacioDocente(
                ilustracion = stringResource(R.string.tablero_vacio_ilustracion),
                titulo = stringResource(R.string.codigo_no_disponible_titulo),
                texto = stringResource(R.string.codigo_no_disponible_texto),
                modifier = Modifier.padding(relleno),
            ) {
                BotonLleno(stringResource(R.string.crear_ir_tablero), onClick = onVolver)
            }
        }
    }
}

@Composable
private fun PantallaCodigo(reto: RetoDocente, codigo: String, ninosQueEntraron: Int?, onVolver: () -> Unit, modifier: Modifier) {
    // Mientras se muestra al grupo, la pantalla no se apaga.
    val vista = LocalView.current
    DisposableEffect(vista) {
        vista.keepScreenOn = true
        onDispose { vista.keepScreenOn = false }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .fondoDegradado()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconButton(
                onClick = onVolver,
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.White.copy(alpha = 0.14f),
                    contentColor = Color.White,
                ),
            ) {
                Icon(painterResource(R.drawable.ic_atras), contentDescription = stringResource(R.string.comun_atras))
            }
            Text(
                stringResource(R.string.reto_resumen_corto, reto.nombre, reto.dificultad.nombre()),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Normal),
                color = Color.White.copy(alpha = 0.9f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            stringResource(R.string.codigo_titulo),
            fontFamily = BalooDos,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 34.sp,
            lineHeight = 38.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        DigitosGigantes(codigo)
        Row(
            Modifier
                .background(Color.White.copy(alpha = 0.16f), CircleShape)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(painterResource(R.drawable.ic_reloj), contentDescription = null, tint = Color.White)
            Text(
                stringResource(R.string.codigo_vence_hoy),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                color = Color.White,
            )
        }
        if (ninosQueEntraron != null) {
            Row(
                Modifier
                    .background(Color.White.copy(alpha = 0.92f), CircleShape)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(Modifier.size(12.dp).background(MaterialTheme.colorScheme.tertiary, CircleShape))
                Text(
                    if (ninosQueEntraron == 0) {
                        stringResource(R.string.codigo_ningun_nino)
                    } else {
                        pluralStringResource(R.plurals.codigo_ninos_entraron, ninosQueEntraron, ninosQueEntraron)
                    },
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                    color = ReciclaKidsColors.tintaNino,
                )
            }
        }
        Text(
            stringResource(R.string.codigo_mismo_color),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
        )
    }
}

/** Rejilla 2 × 2. La escala de fuente se fija: los dígitos son figuras de tamaño exacto. */
@Composable
private fun DigitosGigantes(codigo: String) {
    val descripcion = stringResource(R.string.codigo_descripcion, codigo.toList().joinToString(" "))
    EscalaFijaNino {
        Column(
            Modifier.clearAndSetSemantics { contentDescription = descripcion },
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            codigo.chunked(2).forEach { fila ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    fila.forEach { digito ->
                        FiguraDigito(
                            digito = digito,
                            modifier = Modifier.size(140.dp, 170.dp),
                            tamanoTexto = 110.sp,
                            radioCuadrado = 32.dp,
                            puntaGota = 20.dp,
                        )
                    }
                }
            }
        }
    }
}

/** Degradado a 160° como en el prototipo (`linear-gradient(160deg, …)`). */
private fun Modifier.fondoDegradado(): Modifier = drawBehind {
    val angulo = Math.toRadians(160.0)
    val direccion = Offset(sin(angulo).toFloat(), -cos(angulo).toFloat())
    val largo = abs(size.width * direccion.x) + abs(size.height * direccion.y)
    drawRect(
        Brush.linearGradient(
            colors = listOf(FondoInicio, FondoFin),
            start = center - direccion * (largo / 2),
            end = center + direccion * (largo / 2),
        )
    )
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun CodigoPublicadoPreview() {
    val hoy = LocalDate.of(2026, 9, 22)
    ReciclaKidsTheme {
        CodigoPublicadoScreen(
            estado = EstadoUi.Contenido(
                RetoDocente("r1", "Clasificar la lonchera", setOf(CategoriaResiduo.Aprovechable), Dificultad.Medio, hoy, EstadoReto.Publicado, "4729")
            ),
            hoy = hoy,
            ninosQueEntraron = 14,
            onVolver = {},
            onReintentar = {},
        )
    }
}
