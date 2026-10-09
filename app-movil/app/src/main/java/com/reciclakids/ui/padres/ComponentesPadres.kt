package com.reciclakids.ui.padres

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reciclakids.R
import com.reciclakids.model.CategoriaResiduo
import com.reciclakids.model.HijoVinculado
import com.reciclakids.model.Insignia
import com.reciclakids.model.MensajeSemana
import com.reciclakids.model.Residuo
import com.reciclakids.model.inicioSemana
import com.reciclakids.ui.common.EstiloMarcador
import com.reciclakids.ui.common.rayado
import com.reciclakids.ui.docente.AvatarNino
import com.reciclakids.ui.docente.FormaTarjeta
import com.reciclakids.ui.docente.nombreEnFrase
import com.reciclakids.ui.docente.textoPorcentaje
import com.reciclakids.util.fechaCorta
import com.reciclakids.util.nombreDia
import java.time.LocalDate

// Modo Padres: Material 3 estándar como el Modo Docente (objetivos de 48 dp, texto desde 14 sp,
// contraste AA). Reusa los componentes de adulto de ui/docente; aquí va lo propio de la familia.

/** Fondo y borde de las tarjetas de insignia ganada (ámbar suave del prototipo). */
internal val FondoLogro = Color(0xFFFFF8E9)
internal val BordeLogro = Color(0xFFE8C877)
internal val TintaLogro = Color(0xFF7A6330)

/** Degradado oscuro de la tarjeta resumen: AA con texto blanco (corrección de la Fase 4). */
internal val ResumenInicio = Color(0xFF00546A)
internal val ResumenFin = Color(0xFF00687F)

/** «Jardín B · 5 años» */
@Composable
internal fun detalleHijo(hijo: HijoVinculado): String =
    stringResource(R.string.padres_hijo_detalle, hijo.grupo, pluralStringResource(R.plurals.padres_edad, hijo.edad, hijo.edad))

/**
 * Barra de UI-29 con el hijo o hija elegido. Si la cuenta tiene más de uno, toda la fila abre el
 * selector; con uno solo no hay nada que elegir y la flecha no aparece.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BarraHijo(hijo: HijoVinculado, varios: Boolean, abierto: Boolean, onAlternar: () -> Unit) {
    val etiqueta = stringResource(R.string.inicio_elegir_hijo)
    TopAppBar(
        title = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .then(if (varios) Modifier.clickable(onClickLabel = etiqueta, role = Role.Button, onClick = onAlternar) else Modifier)
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AvatarNino()
                Column(Modifier.weight(1f)) {
                    Text(hijo.nombre, style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        detalleHijo(hijo),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (varios) {
                    Icon(
                        painterResource(R.drawable.ic_flecha_abajo),
                        contentDescription = null,
                        modifier = Modifier.rotate(if (abierto) 180f else 0f),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    )
}

/** Medalla provisional: rayada si se ganó, gris lisa si está por descubrir. */
@Composable
internal fun MedallaInsignia(ganada: Boolean, modifier: Modifier = Modifier, tamano: Dp = 56.dp) {
    val estilo = EstiloMarcador.Insignia
    Box(
        modifier
            .size(tamano)
            .clip(CircleShape)
            .then(if (ganada) Modifier.rayado(estilo.claro, estilo.oscuro, paso = 8.dp) else Modifier.background(Color(0xFFE1E9EC)))
            .clearAndSetSemantics { }
    )
}

/** Tarjeta verde de la frase interpretativa: el avance en lenguaje claro. */
@Composable
internal fun TarjetaMensaje(texto: AnnotatedString, modifier: Modifier = Modifier, interna: Boolean = false) {
    val tinta = MaterialTheme.colorScheme.onTertiaryContainer
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.tertiaryContainer, if (interna) RoundedCornerShape(12.dp) else FormaTarjeta)
            .padding(14.dp)
            .semantics(mergeDescendants = true) { },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(painterResource(R.drawable.ic_tendencia_sube), contentDescription = null, tint = tinta, modifier = Modifier.padding(top = 2.dp).size(22.dp))
        Text(texto, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp), color = tinta)
    }
}

/** Botón-tarjeta con ícono arriba, para los atajos del inicio. */
@Composable
internal fun RowScope.AtajoPadres(texto: String, @DrawableRes icono: Int, tinte: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.weight(1f).heightIn(min = 76.dp),
        shape = FormaTarjeta,
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFBFC8CB)),
    ) {
        Column(
            Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        ) {
            Icon(painterResource(icono), contentDescription = null, tint = tinte, modifier = Modifier.size(26.dp))
            Text(texto, style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp))
        }
    }
}

/** Fondo ámbar de una insignia ganada; gris claro si está por descubrir. */
internal fun Modifier.fondoLogro(ganada: Boolean, forma: RoundedCornerShape = RoundedCornerShape(14.dp)): Modifier =
    if (ganada) background(FondoLogro, forma).border(1.dp, BordeLogro, forma) else background(Color(0xFFF0F4F5), forma)

// ---------- Textos ----------

/** La frase de la semana con la caneca en negrita, como en los prototipos. */
@Composable
internal fun textoMensaje(mensaje: MensajeSemana, confusion: Residuo? = null): AnnotatedString {
    val frase = when (mensaje) {
        is MensajeSemana.Mejoro -> conNegrita(
            stringResource(R.string.mensaje_mejoro, mensaje.categoria.nombreEnFrase(), textoPorcentaje(mensaje.antes), textoPorcentaje(mensaje.ahora)),
            mensaje.categoria.nombreEnFrase(),
        )
        is MensajeSemana.Practicar -> conNegrita(
            stringResource(R.string.mensaje_practicar, mensaje.categoria.nombreEnFrase(), textoPorcentaje(mensaje.aciertos)),
            mensaje.categoria.nombreEnFrase(),
        )
        MensajeSemana.VaMuyBien -> AnnotatedString(stringResource(R.string.mensaje_va_bien))
    }
    if (confusion == null) return frase
    val consejo = stringResource(R.string.mensaje_confunde, confusion.conArticulo(), confusion.categoria.consejo())
    return buildAnnotatedString {
        append(frase)
        append(" ")
        append(consejo)
    }
}

/** Pone en negrita la primera aparición de [resaltado] dentro de [texto]. */
internal fun conNegrita(texto: String, resaltado: String): AnnotatedString = buildAnnotatedString {
    val i = texto.indexOf(resaltado)
    if (i < 0) {
        append(texto)
        return@buildAnnotatedString
    }
    append(texto.substring(0, i))
    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(resaltado) }
    append(texto.substring(i + resaltado.length))
}

@Composable
private fun CategoriaResiduo.consejo(): String = stringResource(
    when (this) {
        CategoriaResiduo.Aprovechable -> R.string.consejo_aprovechables
        CategoriaResiduo.NoAprovechable -> R.string.consejo_no_aprovechables
        CategoriaResiduo.Organico -> R.string.consejo_organicos
    }
)

/** «el empaque metalizado» */
@Composable
internal fun Residuo.conArticulo(): String = when (id) {
    "botella" -> stringResource(R.string.residuo_botella)
    "lata" -> stringResource(R.string.residuo_lata)
    "papel" -> stringResource(R.string.residuo_papel)
    "carton" -> stringResource(R.string.residuo_carton)
    "cascara" -> stringResource(R.string.residuo_cascara)
    "restos" -> stringResource(R.string.residuo_restos)
    "servilleta" -> stringResource(R.string.residuo_servilleta)
    "empaque" -> stringResource(R.string.residuo_empaque)
    else -> nombre
}

/** Lo que logró el niño, contado al acudiente en tercera persona. */
@Composable
internal fun Insignia.paraPadres(): String = stringResource(
    when (this) {
        Insignia.AmigaTortuga -> R.string.logro_padres_amiga_tortuga
        Insignia.RapidoComoPez -> R.string.logro_padres_rapido_pez
        Insignia.RachaDeCinco -> R.string.logro_padres_racha_5
        Insignia.CincoRetosDiarios -> R.string.logro_padres_5_dias
        Insignia.PulpoOrdenado -> R.string.logro_padres_pulpo
        Insignia.AguaCristalina -> R.string.logro_padres_agua
        Insignia.CoralFeliz -> R.string.logro_padres_coral
        Insignia.GuardianDelMar -> R.string.logro_padres_guardian
    }
)

/** «hoy», «ayer», «jueves» si es de esta semana, o «12 sep». */
@Composable
internal fun textoCuando(fecha: LocalDate, hoy: LocalDate): String = when {
    fecha == hoy -> stringResource(R.string.cuando_hoy)
    fecha == hoy.minusDays(1) -> stringResource(R.string.cuando_ayer)
    inicioSemana(fecha) == inicioSemana(hoy) -> nombreDia(fecha)
    else -> fechaCorta(fecha)
}
