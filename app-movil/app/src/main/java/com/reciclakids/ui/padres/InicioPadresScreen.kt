package com.reciclakids.ui.padres

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reciclakids.R
import com.reciclakids.model.HijoVinculado
import com.reciclakids.model.InicioPadres
import com.reciclakids.model.Insignia
import com.reciclakids.model.LogroHijo
import com.reciclakids.model.SemanaHijo
import com.reciclakids.ui.docente.AvatarNino
import com.reciclakids.ui.docente.BotonContorno
import com.reciclakids.ui.docente.BotonLleno
import com.reciclakids.ui.docente.EsqueletoCarga
import com.reciclakids.ui.docente.EstadoErrorRed
import com.reciclakids.ui.docente.EstadoVacioDocente
import com.reciclakids.ui.docente.MarcoDocente
import com.reciclakids.ui.docente.TarjetaDocente
import com.reciclakids.ui.docente.textoPorcentaje
import com.reciclakids.ui.theme.BalooDos
import com.reciclakids.ui.theme.Duracion
import com.reciclakids.ui.theme.ReciclaKidsTheme
import com.reciclakids.ui.theme.ReciclaKidsColors
import com.reciclakids.util.rangoFechas
import com.reciclakids.viewmodel.EstadoUi
import java.time.LocalDate

/**
 * UI-29 Inicio. La semana del hijo o hija se entiende en cinco segundos: retos completados,
 * aciertos, racha, último logro y una frase que dice en qué mejoró. El atajo principal lleva al
 * reporte completo (meta: que el 60 % de los acudientes lo consulte).
 */
@Composable
fun InicioPadresScreen(
    hijo: HijoVinculado,
    hijos: List<HijoVinculado>,
    estado: EstadoUi<InicioPadres>,
    onElegirHijo: (HijoVinculado) -> Unit,
    onReintentar: () -> Unit,
    onVerReporte: () -> Unit,
    onVerLogros: () -> Unit,
    onControlParental: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val varios = hijos.size > 1
    var selectorAbierto by rememberSaveable { mutableStateOf(false) }
    MarcoDocente(
        modifier = modifier,
        barra = { BarraHijo(hijo, varios, selectorAbierto, onAlternar = { selectorAbierto = !selectorAbierto }) },
    ) { relleno ->
        Column(Modifier.fillMaxSize().padding(relleno)) {
            AnimatedVisibility(visible = varios && selectorAbierto) {
                SelectorHijo(
                    hijos = hijos,
                    elegido = hijo.id,
                    onElegir = {
                        selectorAbierto = false
                        onElegirHijo(it)
                    },
                )
            }
            Box(Modifier.weight(1f)) {
                when (estado) {
                    EstadoUi.Cargando -> EsqueletoCarga()
                    EstadoUi.Error -> EstadoErrorRed(
                        onReintentar = onReintentar,
                        titulo = stringResource(R.string.padres_error_titulo),
                        texto = stringResource(R.string.padres_error_texto),
                    )
                    is EstadoUi.Vacio -> EstadoVacioDocente(
                        ilustracion = stringResource(R.string.inicio_vacio_ilustracion),
                        titulo = stringResource(R.string.inicio_vacio_titulo),
                        texto = stringResource(R.string.inicio_vacio_texto, hijo.nombre),
                        flotante = true,
                    ) {
                        BotonContorno(stringResource(R.string.inicio_vacio_preferencias), onClick = onControlParental)
                    }
                    is EstadoUi.Contenido -> ContenidoInicio(estado.datos, onVerReporte, onVerLogros, onControlParental)
                }
            }
        }
    }
}

/** Selector desplegable: solo aparece si la cuenta tiene más de un hijo o hija vinculado. */
@Composable
private fun SelectorHijo(hijos: List<HijoVinculado>, elegido: String, onElegir: (HijoVinculado) -> Unit) {
    Surface(color = Color.White) {
        Column {
            Column(Modifier.selectableGroup().padding(horizontal = 8.dp, vertical = 8.dp)) {
                hijos.forEach { opcion ->
                    val activo = opcion.id == elegido
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .background(if (activo) Color(0xFFE4F3F8) else Color.Transparent, RoundedCornerShape(12.dp))
                            .selectable(selected = activo, role = Role.RadioButton, onClick = { onElegir(opcion) })
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AvatarNino(Modifier.size(36.dp))
                        Column(Modifier.weight(1f)) {
                            Text(opcion.nombre, style = MaterialTheme.typography.bodyLarge)
                            Text(detalleHijo(opcion), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        RadioButton(selected = activo, onClick = null)
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }
    }
}

@Composable
private fun ContenidoInicio(
    datos: InicioPadres,
    onVerReporte: () -> Unit,
    onVerLogros: () -> Unit,
    onControlParental: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TarjetaResumenSemana(datos.semana)
        datos.ultimoLogro?.let { TarjetaUltimoLogro(it, datos.hoy) }
        val mensaje = datos.mensaje
        if (datos.semana.jugo && mensaje != null) {
            TarjetaMensaje(textoMensaje(mensaje))
        } else if (!datos.semana.jugo) {
            TarjetaDocente(Modifier.fillMaxWidth(), relleno = PaddingValues(14.dp)) {
                Text(
                    stringResource(R.string.inicio_sin_juego),
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        BotonLleno(
            stringResource(R.string.inicio_ver_reporte),
            onClick = onVerReporte,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AtajoPadres(
                texto = stringResource(R.string.inicio_logros, datos.insigniasGanadas, Insignia.entries.size),
                icono = R.drawable.ic_insignia,
                tinte = Color(0xFFB98A13),
                onClick = onVerLogros,
            )
            AtajoPadres(
                texto = stringResource(R.string.inicio_minutos_dia, datos.limiteMinutos),
                icono = R.drawable.ic_reloj,
                tinte = MaterialTheme.colorScheme.primary,
                onClick = onControlParental,
            )
        }
        Text(
            stringResource(R.string.inicio_solo_ves, datos.hijo.nombre),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Tarjeta resumen: degradado oscuro para que el blanco pase AA (corrección de la Fase 4). */
@Composable
private fun TarjetaResumenSemana(semana: SemanaHijo) {
    val forma = RoundedCornerShape(20.dp)
    val blanco = Color.White
    val descripcion = stringResource(R.string.inicio_resumen_descripcion, semana.retosCompletados, semana.retosPublicados)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(ResumenInicio, ResumenFin)), forma)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            stringResource(R.string.inicio_esta_semana, rangoFechas(semana.inicio, semana.fin)),
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp),
            color = blanco,
        )
        Row(
            modifier = Modifier.clearAndSetSemantics { contentDescription = descripcion },
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                semana.retosCompletados.toString(),
                style = TextStyle(fontFamily = BalooDos, fontWeight = FontWeight.ExtraBold, fontSize = 58.sp, lineHeight = 58.sp),
                color = blanco,
            )
            Text(
                pluralStringResource(R.plurals.inicio_de_retos, semana.retosPublicados, semana.retosPublicados),
                style = MaterialTheme.typography.bodyLarge,
                color = blanco,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }
        BarraSobreOscuro(if (semana.retosPublicados == 0) 0f else semana.retosCompletados / semana.retosPublicados.toFloat())
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CajaDato(semana.aciertos?.let { textoPorcentaje(it) } ?: stringResource(R.string.padres_sin_valor), stringResource(R.string.inicio_aciertos))
            CajaDato(semana.rachaMaxima.toString(), stringResource(R.string.inicio_racha))
        }
    }
}

/** La barra del resumen se llena en [Duracion.enfatica] al entrar, sobre el fondo oscuro. */
@Composable
private fun BarraSobreOscuro(fraccion: Float) {
    val avance = remember { Animatable(0f) }
    LaunchedEffect(fraccion) { avance.animateTo(fraccion.coerceIn(0f, 1f), tween(Duracion.enfatica)) }
    val relleno = ReciclaKidsColors.halo
    Box(
        Modifier
            .fillMaxWidth()
            .height(10.dp)
            .background(Color.White.copy(alpha = 0.28f), RoundedCornerShape(5.dp))
            .drawBehind { drawRoundRect(relleno, size = Size(size.width * avance.value, size.height), cornerRadius = CornerRadius(5.dp.toPx())) }
    )
}

@Composable
private fun RowScope.CajaDato(valor: String, etiqueta: String) {
    Column(
        modifier = Modifier
            .weight(1f)
            .background(Color(0x57001F28), RoundedCornerShape(14.dp))
            .padding(12.dp)
            .semantics(mergeDescendants = true) { },
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            valor,
            style = TextStyle(fontFamily = BalooDos, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 32.sp),
            color = Color.White,
        )
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium, color = Color.White)
    }
}

@Composable
private fun TarjetaUltimoLogro(logro: LogroHijo, hoy: LocalDate) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .fondoLogro(ganada = true, forma = RoundedCornerShape(16.dp))
            .padding(14.dp)
            .semantics(mergeDescendants = true) { },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        MedallaInsignia(ganada = true)
        Column(Modifier.weight(1f)) {
            val cuando = logro.ganadaEl?.let { textoCuando(it, hoy) }.orEmpty()
            Text(stringResource(R.string.inicio_ultimo_logro, cuando), style = MaterialTheme.typography.bodyMedium, color = TintaLogro)
            Text(logro.insignia.nombre, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp))
            Text(logro.insignia.paraPadres(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Preview(widthDp = 360, heightDp = 900)
@Composable
private fun InicioPadresPreview() {
    val hoy = LocalDate.of(2026, 9, 18)
    val salome = HijoVinculado("nino-1", "Salomé M.", "Jardín B", 5, "Pulpo azul", LocalDate.of(2026, 8, 21))
    ReciclaKidsTheme {
        InicioPadresScreen(
            hijo = salome,
            hijos = listOf(salome, salome.copy(id = "nino-a7", nombre = "Martín M.", grupo = "Jardín A", edad = 4)),
            estado = EstadoUi.Contenido(
                InicioPadres(
                    hijo = salome,
                    hoy = hoy,
                    semana = SemanaHijo(LocalDate.of(2026, 9, 14), 5, emptyList(), emptyList()),
                    anterior = null,
                    ultimoLogro = LogroHijo(Insignia.AmigaTortuga, hoy.minusDays(1)),
                    insigniasGanadas = 4,
                    limiteMinutos = 20,
                    sinDatos = false,
                )
            ),
            onElegirHijo = {}, onReintentar = {}, onVerReporte = {}, onVerLogros = {}, onControlParental = {},
        )
    }
}
