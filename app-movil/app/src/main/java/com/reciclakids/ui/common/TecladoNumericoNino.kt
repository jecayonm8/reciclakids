package com.reciclakids.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reciclakids.R
import com.reciclakids.ui.theme.BalooDos
import com.reciclakids.ui.theme.Duracion
import com.reciclakids.ui.theme.EstilosDigito
import com.reciclakids.ui.theme.ReciclaKidsColors
import com.reciclakids.ui.theme.ReciclaKidsTheme
import com.reciclakids.ui.theme.Tactil

/** Estado de las casillas del código del reto; cada uno cambia el borde, nunca a rojo. */
enum class EstadoCodigo(val borde: Color) {
    Editando(Color.White),
    Correcto(ReciclaKidsColors.botonConfirmar),
    Invalido(Color(0xFFE8A33D)),
    Vencido(Color(0xFF7A57C9)),
}

/**
 * Un dígito con su color y su forma. Es la misma figura en la tecla, en la casilla y en el
 * código gigante que muestra la docente (UI-24): el niño empareja figuras, no lee números.
 */
@Composable
fun FiguraDigito(
    digito: Char,
    modifier: Modifier = Modifier,
    tamanoTexto: TextUnit = 40.sp,
    radioCuadrado: Dp = 20.dp,
    puntaGota: Dp = 10.dp,
    borde: BorderStroke? = null,
) {
    val estilo = EstilosDigito.getValue(digito)
    val forma = estilo.forma.comoShape(radioCuadrado, puntaGota)
    Box(
        modifier = modifier
            .background(estilo.fondo, forma)
            .then(if (borde != null) Modifier.border(borde, forma) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = digito.toString(),
            fontFamily = BalooDos,
            fontWeight = FontWeight.ExtraBold,
            fontSize = tamanoTexto,
            color = estilo.texto,
        )
    }
}

/**
 * Casillas de 70 × 88 dp que adoptan la forma y el color del dígito escrito.
 * Solo se pintan los primeros [longitud] caracteres de [codigo].
 */
@Composable
fun CasillasCodigo(
    codigo: String,
    modifier: Modifier = Modifier,
    longitud: Int = 4,
    estado: EstadoCodigo = EstadoCodigo.Editando,
) {
    val visible = codigo.take(longitud)
    val descripcion = if (visible.isEmpty()) {
        stringResource(R.string.codigo_vacio)
    } else {
        stringResource(R.string.codigo_descripcion, visible.toList().joinToString(" "))
    }
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = descripcion },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        repeat(longitud) { i ->
            val digito = visible.getOrNull(i)
            val escala = remember { Animatable(1f) }
            LaunchedEffect(digito) { if (digito != null) escala.rebotar() }
            val tamano = Modifier
                .size(70.dp, 88.dp)
                .graphicsLayer {
                    scaleX = escala.value
                    scaleY = escala.value
                }
            if (digito != null) {
                FiguraDigito(
                    digito = digito,
                    modifier = tamano,
                    tamanoTexto = 46.sp,
                    radioCuadrado = 18.dp,
                    puntaGota = 12.dp,
                    borde = BorderStroke(4.dp, estado.borde),
                )
            } else {
                Box(tamano.casillaVacia())
            }
        }
    }
}

private fun Modifier.casillaVacia(): Modifier = this
    .background(Color.White.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
    .drawBehind {
        val grosor = 4.dp.toPx()
        val radio = 20.dp.toPx() - grosor / 2
        drawRoundRect(
            color = Color.White.copy(alpha = 0.9f),
            topLeft = Offset(grosor / 2, grosor / 2),
            size = Size(size.width - grosor, size.height - grosor),
            cornerRadius = CornerRadius(radio),
            style = Stroke(grosor, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 8.dp.toPx()))),
        )
    }

/**
 * Teclado 3 × 4 del Modo Niño: diez dígitos con color y forma propios, más borrar y confirmar
 * en la misma rejilla. No guarda el código; la pantalla lo hace y decide el [EstadoCodigo].
 */
@Composable
fun TecladoNumericoNino(
    onDigito: (Char) -> Unit,
    onBorrar: () -> Unit,
    onConfirmar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf("123", "456", "789").forEach { fila ->
            FilaTeclas {
                fila.forEach { digito -> TeclaDigito(digito, onClick = { onDigito(digito) }) }
            }
        }
        FilaTeclas {
            TeclaAccion(
                icono = painterResource(R.drawable.ic_borrar),
                descripcion = stringResource(R.string.teclado_borrar),
                fondo = Color.White,
                tinta = MaterialTheme.colorScheme.onSurfaceVariant,
                tamanoIcono = 34.dp,
                borde = BorderStroke(3.dp, MaterialTheme.colorScheme.outline),
                onClick = onBorrar,
            )
            TeclaDigito('0', onClick = { onDigito('0') })
            TeclaAccion(
                icono = painterResource(R.drawable.ic_check),
                descripcion = stringResource(R.string.teclado_entrar),
                fondo = ReciclaKidsColors.botonConfirmar,
                tinta = Color.White,
                tamanoIcono = 38.dp,
                onClick = onConfirmar,
            )
        }
    }
}

@Composable
private fun FilaTeclas(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

@Composable
private fun Modifier.tecla(onClick: () -> Unit): Modifier {
    val fuente = remember { MutableInteractionSource() }
    val presionada by fuente.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (presionada) 0.94f else 1f,
        animationSpec = tween(Duracion.tacto, easing = LinearEasing),
        label = "escalaTecla",
    )
    return this
        .height(Tactil.teclaNino)
        .clickable(fuente, indication = null, role = Role.Button, onClick = onClick)
        .graphicsLayer {
            scaleX = escala
            scaleY = escala
        }
}

@Composable
private fun RowScope.TeclaDigito(digito: Char, onClick: () -> Unit) {
    FiguraDigito(digito, Modifier.weight(1f).tecla(onClick))
}

@Composable
private fun RowScope.TeclaAccion(
    icono: Painter,
    descripcion: String,
    fondo: Color,
    tinta: Color,
    tamanoIcono: Dp,
    onClick: () -> Unit,
    borde: BorderStroke? = null,
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .tecla(onClick)
            .background(fondo, CircleShape)
            .then(if (borde != null) Modifier.border(borde, CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icono, contentDescription = descripcion, modifier = Modifier.size(tamanoIcono), tint = tinta)
    }
}

@Preview(widthDp = 360)
@Composable
private fun TecladoNumericoNinoPreview() {
    ReciclaKidsTheme {
        FondoSubmarino(agua = ReciclaKidsColors.aguaJuego, altoArena = 60.dp) {
            Column(
                Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CasillasCodigo("47")
                TecladoNumericoNino(onDigito = {}, onBorrar = {}, onConfirmar = {})
            }
        }
    }
}
