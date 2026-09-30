package com.reciclakids.ui.acceso

import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reciclakids.R
import com.reciclakids.ui.theme.BalooDos
import com.reciclakids.ui.theme.Nunito
import com.reciclakids.ui.theme.ReciclaKidsColors
import com.reciclakids.ui.theme.ReciclaKidsTheme
import kotlinx.coroutines.delay

/** Tiempo que hay que mantener presionado el círculo para abrir la puerta. */
const val DuracionPuertaMs = 3000

private val FormaTarjeta = RoundedCornerShape(18.dp)
private val FormaOpcion = RoundedCornerShape(14.dp)

private enum class EstadoSuma { Reposo, Mal, Bien }

/**
 * Puerta para adultos: oculta los modos Docente y Padres a un niño de 3 a 6 años sin frustrar
 * al adulto. Se abre manteniendo presionado el círculo 3 s o resolviendo una suma sencilla.
 * Sin voz: la puerta no se anuncia al niño. Se usa desde UI-02 y desde los ajustes del niño (UI-18).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PuertaAdultosSheet(onAbierta: () -> Unit, onCerrar: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        // Arrastrar la hoja cancelaría el gesto de mantener presionado.
        sheetGesturesEnabled = false,
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color(0x9E04323F),
        dragHandle = null,
    ) {
        PuertaAdultosContenido(suma = remember { generarSumaPuerta() }, onAbierta = onAbierta, onCerrar = onCerrar)
    }
}

@Composable
internal fun PuertaAdultosContenido(
    suma: SumaPuerta,
    onAbierta: () -> Unit,
    onCerrar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val abrir by rememberUpdatedState(onAbierta)
    // Milisegundos de espera antes de abrir; null mientras la puerta sigue cerrada.
    var apertura by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(apertura) {
        apertura?.let {
            delay(it)
            abrir()
        }
    }

    Column(
        modifier = modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(
                painterResource(R.drawable.ic_candado),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                stringResource(R.string.puerta_titulo),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
            )
            IconButton(onClick = onCerrar) {
                Icon(
                    painterResource(R.drawable.ic_cerrar),
                    contentDescription = stringResource(R.string.comun_cerrar),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            stringResource(R.string.puerta_descripcion),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TarjetaMantener(bloqueada = apertura != null, onCompleto = { apertura = 250 })
        TarjetaSuma(suma = suma, bloqueada = apertura != null, onCorrecta = { apertura = 400 })
    }
}

@Composable
private fun TarjetaMantener(bloqueada: Boolean, onCompleto: () -> Unit) {
    val completo by rememberUpdatedState(onCompleto)
    val progreso = remember { Animatable(0f) }
    var presionado by remember { mutableStateOf(false) }
    var listo by remember { mutableStateOf(false) }
    LaunchedEffect(presionado) {
        when {
            listo -> Unit
            presionado -> {
                progreso.animateTo(1f, tween(DuracionPuertaMs, easing = LinearEasing))
                listo = true
                completo()
            }
            // Al soltar antes de tiempo el anillo se reinicia.
            else -> progreso.snapTo(0f)
        }
    }
    val descripcion = stringResource(R.string.puerta_mantener_boton)
    val anillo = ReciclaKidsColors.botonJugar

    Row(
        modifier = Modifier.tarjetaPuerta(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .drawBehind { drawArc(anillo, startAngle = -90f, sweepAngle = 360f * progreso.value, useCenter = true) }
                .padding(7.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .semantics { contentDescription = descripcion }
                .pointerInput(bloqueada) {
                    if (bloqueada) return@pointerInput
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        presionado = true
                        waitForUpOrCancellation()
                        presionado = false
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(22.dp).background(Color.White, CircleShape))
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.puerta_mantener), style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp))
            TextoEstado(
                when {
                    listo -> R.string.puerta_mantener_listo
                    presionado -> R.string.puerta_mantener_progreso
                    else -> R.string.puerta_mantener_reposo
                }
            )
        }
    }
}

@Composable
private fun TarjetaSuma(suma: SumaPuerta, bloqueada: Boolean, onCorrecta: () -> Unit) {
    var estado by remember { mutableStateOf(EstadoSuma.Reposo) }
    Column(
        modifier = Modifier.tarjetaPuerta(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(R.string.puerta_suma_titulo),
            modifier = Modifier.align(Alignment.Start),
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
        )
        Text(
            stringResource(R.string.puerta_suma, suma.a, suma.b),
            fontFamily = BalooDos,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 34.sp,
            color = ReciclaKidsColors.tintaNino,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            suma.opciones.forEach { opcion ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .background(Color.White, FormaOpcion)
                        .border(BorderStroke(2.dp, MaterialTheme.colorScheme.outline), FormaOpcion)
                        .clickable(enabled = !bloqueada, role = Role.Button) {
                            if (opcion == suma.respuesta) {
                                estado = EstadoSuma.Bien
                                onCorrecta()
                            } else {
                                estado = EstadoSuma.Mal
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(opcion.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
                }
            }
        }
        TextoEstado(
            when (estado) {
                EstadoSuma.Reposo -> R.string.puerta_suma_reposo
                EstadoSuma.Mal -> R.string.puerta_suma_mal
                EstadoSuma.Bien -> R.string.puerta_suma_bien
            }
        )
    }
}

@Composable
private fun TextoEstado(@StringRes texto: Int) {
    Text(
        stringResource(texto),
        fontFamily = Nunito,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun Modifier.tarjetaPuerta(): Modifier = this
    .fillMaxWidth()
    .background(Color.White, FormaTarjeta)
    .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, FormaTarjeta)
    .padding(16.dp)

@Preview(widthDp = 360)
@Composable
private fun PuertaAdultosPreview() {
    ReciclaKidsTheme {
        PuertaAdultosContenido(
            suma = SumaPuerta(4, 3, listOf(5, 7, 9)),
            onAbierta = {},
            onCerrar = {},
            modifier = Modifier.background(MaterialTheme.colorScheme.surface),
        )
    }
}
