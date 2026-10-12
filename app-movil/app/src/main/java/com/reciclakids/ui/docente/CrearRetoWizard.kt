package com.reciclakids.ui.docente

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.reciclakids.R
import com.reciclakids.model.CategoriaResiduo
import com.reciclakids.model.Dificultad
import com.reciclakids.model.EstadoReto
import com.reciclakids.model.RetoDocente
import com.reciclakids.ui.common.EstiloMarcador
import com.reciclakids.ui.common.FiguraDigito
import com.reciclakids.ui.common.MarcadorIlustracion
import com.reciclakids.ui.common.caneca
import com.reciclakids.ui.theme.ReciclaKidsColors
import com.reciclakids.ui.theme.ReciclaKidsTheme
import com.reciclakids.util.diaCorto
import com.reciclakids.util.diaLargo
import com.reciclakids.viewmodel.AsistenteReto
import com.reciclakids.viewmodel.EstadoPublicacion
import com.reciclakids.viewmodel.EstadoUi
import java.time.LocalDate

private val NombresPasos = listOf(
    R.string.crear_paso_canecas,
    R.string.crear_paso_dificultad,
    R.string.crear_paso_fecha,
    R.string.crear_paso_vista,
    R.string.crear_paso_publicar,
)

/**
 * UI-22 Crear reto, a pantalla completa: cinco pasos cortos con «Crear a partir de un reto
 * anterior» como primera opción, más los estados de publicación (enviando, éxito y fallo con
 * borrador guardado). No guarda estado propio: todo llega del [AsistenteReto].
 */
@Composable
fun CrearRetoWizard(
    asistente: AsistenteReto,
    publicacion: EstadoPublicacion,
    hojaReutilizar: Boolean,
    previos: EstadoUi<List<RetoDocente>>,
    onCambio: ((AsistenteReto) -> AsistenteReto) -> Unit,
    onAbrirReutilizar: () -> Unit,
    onCerrarReutilizar: () -> Unit,
    onReintentarPrevios: () -> Unit,
    onUsar: (RetoDocente) -> Unit,
    onPublicar: () -> Unit,
    onCerrar: () -> Unit,
    onIrTablero: () -> Unit,
    onMostrarCodigo: (RetoDocente) -> Unit,
    onVerBiblioteca: () -> Unit,
    modifier: Modifier = Modifier,
    editando: Boolean = false,
    origen: EstadoUi<Unit>? = null,
    onReintentarOrigen: () -> Unit = {},
    onCerrarResultado: () -> Unit = {},
) {
    BackHandler(enabled = asistente.paso > 1) { onCambio { it.atras() } }

    MarcoDocente(
        modifier = modifier,
        pantallaCompleta = true,
        barra = {
            BarraDocente(
                titulo = stringResource(if (editando) R.string.crear_titulo_editar else R.string.crear_titulo),
                onAtras = onCerrar,
                iconoAtras = R.drawable.ic_cerrar,
                descripcionAtras = stringResource(R.string.crear_cerrar),
                acciones = { ChipMenosDeUnMinuto(Modifier.padding(end = 12.dp)) },
            )
        },
        barraInferior = {
            PieAsistente(
                asistente = asistente,
                onAtras = { if (asistente.paso > 1) onCambio { it.atras() } else onCerrar() },
                onSiguiente = { if (asistente.esUltimoPaso) onPublicar() else onCambio { it.siguiente() } },
            )
        },
    ) { relleno ->
        Column(Modifier.fillMaxSize().padding(relleno)) {
            FilaPasos(asistente, onIrA = { paso -> onCambio { it.irA(paso) } })
            when (origen) {
                EstadoUi.Cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                EstadoUi.Error -> EstadoErrorRed(onReintentarOrigen)
                else -> CuerpoAsistente(asistente, onCambio, onAbrirReutilizar, onPublicar)
            }
        }
    }

    if (hojaReutilizar) {
        HojaReutilizar(previos, onUsar = onUsar, onCerrar = onCerrarReutilizar, onReintentar = onReintentarPrevios)
    }
    DialogosPublicacion(asistente, publicacion, onPublicar, onIrTablero, onMostrarCodigo, onVerBiblioteca, onCerrarResultado)
}

@Composable
private fun ChipMenosDeUnMinuto(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painterResource(R.drawable.ic_reloj),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Text(
            stringResource(R.string.crear_menos_minuto),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            maxLines = 1,
        )
    }
}

/** Chips «1. Canecas … 5. Publicar»: tocables para saltar de paso. */
@Composable
private fun FilaPasos(asistente: AsistenteReto, onIrA: (Int) -> Unit) {
    val lista = rememberLazyListState()
    LaunchedEffect(asistente.paso) { lista.animateScrollToItem((asistente.paso - 2).coerceAtLeast(0)) }
    LazyRow(
        state = lista,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(NombresPasos.size) { i ->
            val paso = i + 1
            val activo = paso == asistente.paso
            val hecho = paso < asistente.paso
            val esquema = MaterialTheme.colorScheme
            Surface(
                selected = activo,
                onClick = { onIrA(paso) },
                enabled = paso == 1 || asistente.tieneCategorias,
                shape = CircleShape,
                color = when {
                    activo -> esquema.primary
                    hecho -> esquema.tertiaryContainer
                    else -> esquema.surfaceContainer
                },
                contentColor = when {
                    activo -> esquema.onPrimary
                    hecho -> esquema.onTertiaryContainer
                    else -> esquema.onSurfaceVariant
                },
            ) {
                Box(Modifier.heightIn(min = 36.dp).padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.crear_paso_chip, paso, stringResource(NombresPasos[i])),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun CuerpoAsistente(
    asistente: AsistenteReto,
    onCambio: ((AsistenteReto) -> AsistenteReto) -> Unit,
    onAbrirReutilizar: () -> Unit,
    onPublicar: () -> Unit,
) {
    // Transición de 200 ms entre pasos: aparece y sube 10 dp, como en el prototipo.
    val desplazamiento = with(LocalDensity.current) { 10.dp.roundToPx() }
    AnimatedContent(
        targetState = asistente.paso,
        transitionSpec = {
            (fadeIn(tween(200)) + slideInVertically(tween(200)) { desplazamiento }) togetherWith fadeOut(tween(90))
        },
        label = "pasoAsistente",
    ) { paso ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            when (paso) {
                1 -> PasoCanecas(asistente, onCambio, onAbrirReutilizar)
                2 -> PasoDificultad(asistente, onCambio)
                3 -> PasoFecha(asistente, onCambio)
                4 -> PasoVistaPrevia(asistente)
                else -> PasoPublicar(asistente, onPublicar)
            }
        }
    }
}

@Composable
private fun PieAsistente(asistente: AsistenteReto, onAtras: () -> Unit, onSiguiente: () -> Unit) {
    Surface(color = Color.White) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Row(
                modifier = Modifier
                    .navigationBarsPadding()
                    .fillMaxWidth()
                    .heightIn(min = 76.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BotonContorno(stringResource(R.string.crear_atras), onClick = onAtras)
                Text(
                    stringResource(R.string.paso_de, asistente.paso, AsistenteReto.TotalPasos),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    modifier = Modifier.weight(1f),
                )
                BotonLleno(
                    texto = stringResource(
                        when (asistente.paso) {
                            AsistenteReto.TotalPasos -> R.string.crear_publicar
                            AsistenteReto.PasoVistaPrevia -> R.string.crear_todo_listo
                            else -> R.string.crear_siguiente
                        }
                    ),
                    onClick = onSiguiente,
                    habilitado = asistente.tieneCategorias,
                )
            }
        }
    }
}

private val TituloPaso: TextStyle
    @Composable get() = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Normal)

// ---------- Paso 1 · Canecas ----------

@Composable
private fun PasoCanecas(
    asistente: AsistenteReto,
    onCambio: ((AsistenteReto) -> AsistenteReto) -> Unit,
    onAbrirReutilizar: () -> Unit,
) {
    Surface(
        onClick = onAbrirReutilizar,
        shape = FormaTarjeta,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(painterResource(R.drawable.ic_reutilizar), contentDescription = null, modifier = Modifier.size(30.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.crear_reutilizar_titulo), style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp))
                Text(
                    stringResource(R.string.crear_reutilizar_texto),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF28464F),
                )
            }
            Icon(painterResource(R.drawable.ic_siguiente), contentDescription = null)
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.surfaceVariant)
        Text(
            stringResource(R.string.crear_desde_cero),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.surfaceVariant)
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.crear_pregunta_canecas), style = TituloPaso)
        if (!asistente.tieneCategorias) {
            Text(
                stringResource(R.string.crear_paso_falta_caneca, stringResource(R.string.paso_de, 1, AsistenteReto.TotalPasos)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    CategoriaResiduo.entries.forEach { categoria ->
        TarjetaCategoria(
            categoria = categoria,
            elegida = categoria in asistente.categorias,
            onCambio = { onCambio { it.alternar(categoria) } },
        )
    }
}

@Composable
private fun TarjetaCategoria(categoria: CategoriaResiduo, elegida: Boolean, onCambio: () -> Unit) {
    val esquema = MaterialTheme.colorScheme
    Surface(
        checked = elegida,
        onCheckedChange = { onCambio() },
        modifier = Modifier.fillMaxWidth().semantics { role = Role.Checkbox },
        shape = FormaTarjeta,
        color = Color.White,
        border = if (elegida) BorderStroke(3.dp, esquema.primary) else BorderStroke(1.dp, BordeOpcion),
        shadowElevation = if (elegida) 2.dp else 0.dp,
    ) {
        Column(
            Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MuestraCaneca(categoria, tamano = 64.dp, radio = 16.dp, borde = 3.dp, conIcono = true)
            Text(categoria.nombre(), style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp))
            Text(
                categoria.ejemplos(),
                style = MaterialTheme.typography.bodyMedium,
                color = esquema.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Box(
                Modifier.size(24.dp).background(if (elegida) esquema.primary else esquema.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (elegida) {
                    Icon(painterResource(R.drawable.ic_check), contentDescription = null, modifier = Modifier.size(16.dp), tint = esquema.onPrimary)
                }
            }
        }
    }
}

// ---------- Paso 2 · Dificultad ----------

@Composable
private fun PasoDificultad(asistente: AsistenteReto, onCambio: ((AsistenteReto) -> AsistenteReto) -> Unit) {
    Text(stringResource(R.string.crear_pregunta_dificultad), style = TituloPaso)
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Dificultad.entries.forEach { dificultad ->
            val elegida = dificultad == asistente.dificultad
            OpcionElegible(elegida = elegida, onClick = { onCambio { it.conDificultad(dificultad) } }) {
                Row(
                    Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    RadioButton(selected = elegida, onClick = null)
                    Column(Modifier.weight(1f)) {
                        Text(dificultad.nombre(), style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp))
                        Text(
                            dificultad.detalle(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(dificultad.edad(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/** Tarjeta de opción única: borde de 3 dp en primario cuando está elegida. */
@Composable
private fun OpcionElegible(
    elegida: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contenido: @Composable () -> Unit,
) {
    Surface(
        selected = elegida,
        onClick = onClick,
        modifier = modifier.fillMaxWidth().semantics { role = Role.RadioButton },
        shape = FormaTarjeta,
        color = Color.White,
        border = if (elegida) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, BordeOpcion),
        content = contenido,
    )
}

// ---------- Paso 3 · Fecha ----------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PasoFecha(asistente: AsistenteReto, onCambio: ((AsistenteReto) -> AsistenteReto) -> Unit) {
    Text(stringResource(R.string.crear_pregunta_fecha), style = TituloPaso)
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OpcionFecha(
            titulo = stringResource(R.string.crear_fecha_hoy),
            texto = stringResource(R.string.crear_fecha_hoy_texto),
            elegida = !asistente.programado,
            onClick = { onCambio { it.paraHoy() } },
        )
        OpcionFecha(
            titulo = stringResource(R.string.crear_fecha_programar),
            texto = stringResource(R.string.crear_fecha_programar_texto),
            elegida = asistente.programado,
            onClick = { onCambio { it.programar() } },
        )
    }
    if (asistente.programado) {
        TarjetaDocente(Modifier.fillMaxWidth(), relleno = PaddingValues(18.dp), espacio = 12.dp) {
            Text(stringResource(R.string.crear_elige_dia), style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp))
            FlowRow(
                modifier = Modifier.selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                asistente.diasProgramables.forEach { dia ->
                    ChipDia(dia, elegido = dia == asistente.diaProgramado, onClick = { onCambio { it.programar(dia) } })
                }
            }
            Text(
                stringResource(R.string.crear_codigo_esa_manana),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun OpcionFecha(titulo: String, texto: String, elegida: Boolean, onClick: () -> Unit) {
    OpcionElegible(elegida = elegida, onClick = onClick) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp))
            Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ChipDia(dia: LocalDate, elegido: Boolean, onClick: () -> Unit) {
    val esquema = MaterialTheme.colorScheme
    Surface(
        selected = elegido,
        onClick = onClick,
        modifier = Modifier.semantics { role = Role.RadioButton },
        shape = CircleShape,
        color = if (elegido) esquema.primaryContainer else Color.White,
        contentColor = esquema.onPrimaryContainer,
        border = BorderStroke(1.dp, if (elegido) esquema.primary else esquema.outline),
    ) {
        Box(Modifier.heightIn(min = 40.dp).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            Text(diaCorto(dia), style = MaterialTheme.typography.labelLarge)
        }
    }
}

// ---------- Paso 4 · Vista previa ----------

@Composable
private fun PasoVistaPrevia(asistente: AsistenteReto) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.crear_vista_titulo), style = TituloPaso)
        // El juego pone las canecas según la dificultad: Fácil, blanca y verde; Medio y Difícil, las tres.
        VistaPreviaJuego(asistente.dificultad.categorias.toSet())
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.crear_resumen), style = TituloPaso)
        TarjetaDocente(Modifier.fillMaxWidth(), relleno = PaddingValues(18.dp), espacio = 0.dp) {
            FilaResumen(stringResource(R.string.crear_resumen_canecas), textoCategorias(asistente.categorias))
            FilaResumen(stringResource(R.string.crear_resumen_dificultad), asistente.dificultad.resumen())
            FilaResumen(stringResource(R.string.crear_resumen_residuos), textoResiduos(asistente.dificultad))
            FilaResumen(stringResource(R.string.crear_resumen_fecha), textoFechaAsistente(asistente), ultima = true)
        }
        Text(
            stringResource(R.string.crear_voz_sola),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FilaResumen(etiqueta: String, valor: String, ultima: Boolean = false) {
    Column {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 10.dp).semantics(mergeDescendants = true) { },
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(etiqueta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                valor,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
            )
        }
        if (!ultima) HorizontalDivider(color = Color(0xFFEDF3F5))
    }
}

/** «Hoy, martes 22» o «Programado para mié 23». */
@Composable
private fun textoFechaAsistente(asistente: AsistenteReto): String =
    if (asistente.programado) {
        stringResource(R.string.crear_fecha_resumen_programada, diaCorto(asistente.diaProgramado))
    } else {
        stringResource(R.string.crear_fecha_resumen_hoy, diaLargo(asistente.hoy))
    }

/**
 * Miniatura de la pantalla de juego dibujada a 560 × 350 dp y escalada al ancho disponible,
 * como en el prototipo. Muestra las canecas del juego, cada una con su color y su ícono.
 */
@Composable
private fun VistaPreviaJuego(categorias: Set<CategoriaResiduo>) {
    val forma = RoundedCornerShape(18.dp)
    val descripcion = stringResource(R.string.crear_vista_descripcion, textoCategorias(categorias))
    val mostradas = categorias.ifEmpty { CategoriaResiduo.entries.toSet() }.sorted()
    val panel = Color.White.copy(alpha = 0.9f)
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .aspectRatio(560f / 350f)
            .shadow(4.dp, forma)
            .clip(forma)
            .clearAndSetSemantics { contentDescription = descripcion }
    ) {
        val escala = maxWidth / 560.dp
        Box(
            Modifier
                .wrapContentSize(Alignment.TopStart, unbounded = true)
                .requiredSize(560.dp, 350.dp)
                .graphicsLayer {
                    scaleX = escala
                    scaleY = escala
                    transformOrigin = TransformOrigin(0f, 0f)
                }
                .background(ReciclaKidsColors.aguaJuego)
        ) {
            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(46.dp).background(ReciclaKidsColors.arena))
            Row(
                Modifier.padding(14.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(Modifier.size(34.dp).background(panel, CircleShape))
                Row(
                    Modifier.weight(1f).height(34.dp).background(panel, CircleShape).padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    repeat(4) { i ->
                        Box(
                            Modifier
                                .weight(1f)
                                .height(12.dp)
                                .background(if (i < 2) ReciclaKidsColors.algaSana else Color(0xFFDCE7EB), RoundedCornerShape(6.dp))
                        )
                    }
                }
                Box(Modifier.size(80.dp, 34.dp).background(panel, CircleShape))
            }
            MarcadorIlustracion(
                etiqueta = stringResource(R.string.crear_vista_residuo),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 90.dp)
                    .size(84.dp)
                    .border(3.dp, Color.White, RoundedCornerShape(20.dp)),
                forma = RoundedCornerShape(20.dp),
                estilo = EstiloMarcador.Residuo,
            )
            Row(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                val formaCaneca = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
                mostradas.forEach { categoria ->
                    val tipo = categoria.caneca
                    Box(
                        Modifier
                            .size(74.dp, 68.dp)
                            .background(tipo.fondo, formaCaneca)
                            .border(3.dp, tipo.borde, formaCaneca),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(painterResource(tipo.icono), contentDescription = null, modifier = Modifier.size(34.dp))
                    }
                }
            }
        }
    }
}

// ---------- Paso 5 · Publicar ----------

@Composable
private fun PasoPublicar(asistente: AsistenteReto, onPublicar: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Box(
            Modifier.size(120.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(R.drawable.ic_publicar),
                contentDescription = null,
                modifier = Modifier.size(54.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            stringResource(R.string.crear_listo_titulo),
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp, lineHeight = 32.sp),
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(
                if (asistente.programado) R.string.crear_listo_texto_programado else R.string.crear_listo_texto_hoy,
                textoCategorias(asistente.categorias),
                asistente.dificultad.resumen(),
                textoFechaAsistente(asistente),
            ),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        BotonLleno(
            texto = stringResource(R.string.crear_publicar_reto),
            onClick = onPublicar,
            modifier = Modifier.heightIn(min = 56.dp),
            habilitado = asistente.tieneCategorias,
        )
    }
}

// ---------- Hoja «Reutilizar un reto» ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HojaReutilizar(
    previos: EstadoUi<List<RetoDocente>>,
    onUsar: (RetoDocente) -> Unit,
    onCerrar: () -> Unit,
    onReintentar: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.reutilizar_titulo),
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp, fontWeight = FontWeight.Normal),
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onCerrar) {
                    Icon(painterResource(R.drawable.ic_cerrar), contentDescription = stringResource(R.string.comun_cerrar))
                }
            }
            when (previos) {
                EstadoUi.Cargando -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                EstadoUi.Error -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.docente_error_titulo), style = MaterialTheme.typography.bodyLarge)
                    BotonLleno(stringResource(R.string.docente_reintentar), onClick = onReintentar)
                }
                is EstadoUi.Vacio -> Text(
                    stringResource(R.string.reutilizar_vacio),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                is EstadoUi.Contenido -> LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(previos.datos, key = { it.id }) { reto -> FilaRetoPrevio(reto, onUsar = { onUsar(reto) }) }
                }
            }
        }
    }
}

@Composable
private fun FilaRetoPrevio(reto: RetoDocente, onUsar: () -> Unit) {
    val usar = stringResource(R.string.reutilizar_usar_descripcion, reto.nombre)
    val aciertos = reto.aciertosPromedio
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .background(Color.White, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(reto.nombre, style = MaterialTheme.typography.titleMedium)
            Text(
                if (aciertos != null) {
                    stringResource(
                        R.string.reto_detalle,
                        textoCategorias(reto.categorias),
                        reto.dificultad.nombre(),
                        stringResource(R.string.reutilizar_aciertos, textoPorcentaje(aciertos)),
                    )
                } else {
                    stringResource(R.string.reto_resumen_corto, textoCategorias(reto.categorias), reto.dificultad.nombre())
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        BotonTonal(
            stringResource(R.string.reutilizar_usar),
            onClick = onUsar,
            modifier = Modifier.semantics { contentDescription = usar },
        )
    }
}

// ---------- Publicando, publicado y fallido ----------

@Composable
private fun DialogosPublicacion(
    asistente: AsistenteReto,
    publicacion: EstadoPublicacion,
    onReintentar: () -> Unit,
    onIrTablero: () -> Unit,
    onMostrarCodigo: (RetoDocente) -> Unit,
    onVerBiblioteca: () -> Unit,
    onCerrarResultado: () -> Unit,
) {
    val haptico = LocalHapticFeedback.current
    LaunchedEffect(publicacion) {
        if (publicacion is EstadoPublicacion.Publicado) haptico.performHapticFeedback(HapticFeedbackType.Confirm)
    }
    when (publicacion) {
        EstadoPublicacion.Ninguna -> Unit
        EstadoPublicacion.Enviando -> Dialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        ) {
            Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(
                    Modifier.padding(horizontal = 40.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CircularProgressIndicator(Modifier.size(44.dp), strokeWidth = 4.dp)
                    Text(stringResource(R.string.crear_publicando), style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Normal))
                }
            }
        }
        is EstadoPublicacion.Publicado -> {
            val reto = publicacion.reto
            val codigo = reto.codigoActivo(asistente.hoy)
            val fecha = reto.fecha
            if (codigo != null) {
                DialogoResultado(
                    onCerrar = onIrTablero,
                    icono = R.drawable.ic_check,
                    fondoIcono = MaterialTheme.colorScheme.tertiaryContainer,
                    tintaIcono = MaterialTheme.colorScheme.onTertiaryContainer,
                    titulo = stringResource(R.string.crear_publicado_titulo),
                    texto = stringResource(R.string.crear_publicado_texto),
                    principal = stringResource(R.string.crear_mostrar_grupo) to { onMostrarCodigo(reto) },
                    secundaria = stringResource(R.string.crear_ir_tablero) to onIrTablero,
                ) {
                    DigitosMedianos(codigo)
                }
            } else {
                DialogoResultado(
                    onCerrar = onIrTablero,
                    icono = R.drawable.ic_check,
                    fondoIcono = MaterialTheme.colorScheme.tertiaryContainer,
                    tintaIcono = MaterialTheme.colorScheme.onTertiaryContainer,
                    titulo = stringResource(R.string.crear_programado_titulo),
                    texto = stringResource(R.string.crear_programado_texto, if (fecha != null) diaCorto(fecha) else ""),
                    principal = stringResource(R.string.crear_ir_tablero) to onIrTablero,
                    secundaria = stringResource(R.string.crear_ver_biblioteca) to onVerBiblioteca,
                )
            }
        }
        // Cerrar el aviso deja a la docente en el asistente, con el borrador listo para reintentar.
        is EstadoPublicacion.Fallida -> DialogoResultado(
            onCerrar = onCerrarResultado,
            icono = R.drawable.ic_alerta,
            fondoIcono = MaterialTheme.colorScheme.errorContainer,
            tintaIcono = MaterialTheme.colorScheme.error,
            titulo = stringResource(R.string.crear_fallo_titulo),
            texto = stringResource(R.string.crear_fallo_texto),
            principal = stringResource(R.string.docente_reintentar) to onReintentar,
            secundaria = stringResource(R.string.crear_ver_borrador) to onVerBiblioteca,
        )
    }
}

@Composable
private fun DialogoResultado(
    onCerrar: () -> Unit,
    icono: Int,
    fondoIcono: Color,
    tintaIcono: Color,
    titulo: String,
    texto: String,
    principal: Pair<String, () -> Unit>,
    secundaria: Pair<String, () -> Unit>,
    extra: @Composable () -> Unit = {},
) {
    Dialog(onDismissRequest = onCerrar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.padding(16.dp).widthIn(max = 520.dp).fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(
                Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(Modifier.size(88.dp).background(fondoIcono, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(painterResource(icono), contentDescription = null, modifier = Modifier.size(44.dp), tint = tintaIcono)
                }
                Text(titulo, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Text(
                    texto,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                extra()
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BotonLleno(principal.first, onClick = principal.second, modifier = Modifier.fillMaxWidth())
                    BotonContorno(secundaria.first, onClick = secundaria.second, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

/** Dígitos de 72 × 86 dp como máximo; se achican si el diálogo es angosto. */
@Composable
private fun DigitosMedianos(codigo: String) {
    val descripcion = stringResource(R.string.codigo_descripcion, codigo.toList().joinToString(" "))
    Row(
        Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = descripcion },
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
    ) {
        codigo.forEach { digito ->
            FiguraDigito(
                digito = digito,
                modifier = Modifier.weight(1f, fill = false).widthIn(max = 72.dp).aspectRatio(72f / 86f),
                tamanoTexto = 40.sp,
                radioCuadrado = 18.dp,
                puntaGota = 10.dp,
            )
        }
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun CrearRetoPaso1Preview() {
    ReciclaKidsTheme {
        CrearRetoWizard(
            asistente = AsistenteReto(LocalDate.of(2026, 9, 22)),
            publicacion = EstadoPublicacion.Ninguna,
            hojaReutilizar = false,
            previos = EstadoUi.Cargando,
            onCambio = {}, onAbrirReutilizar = {}, onCerrarReutilizar = {}, onReintentarPrevios = {}, onUsar = {},
            onPublicar = {}, onCerrar = {}, onIrTablero = {}, onMostrarCodigo = {}, onVerBiblioteca = {},
        )
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun CrearRetoVistaPreviaPreview() {
    ReciclaKidsTheme {
        CrearRetoWizard(
            asistente = AsistenteReto(LocalDate.of(2026, 9, 22), paso = 4),
            publicacion = EstadoPublicacion.Ninguna,
            hojaReutilizar = false,
            previos = EstadoUi.Cargando,
            onCambio = {}, onAbrirReutilizar = {}, onCerrarReutilizar = {}, onReintentarPrevios = {}, onUsar = {},
            onPublicar = {}, onCerrar = {}, onIrTablero = {}, onMostrarCodigo = {}, onVerBiblioteca = {},
        )
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun CrearRetoPublicadoPreview() {
    val hoy = LocalDate.of(2026, 9, 22)
    ReciclaKidsTheme {
        CrearRetoWizard(
            asistente = AsistenteReto(hoy, paso = 5),
            publicacion = EstadoPublicacion.Publicado(
                RetoDocente("r1", "Clasificar la lonchera", setOf(CategoriaResiduo.Organico), Dificultad.Medio, hoy, EstadoReto.Publicado, "4729")
            ),
            hojaReutilizar = false,
            previos = EstadoUi.Cargando,
            onCambio = {}, onAbrirReutilizar = {}, onCerrarReutilizar = {}, onReintentarPrevios = {}, onUsar = {},
            onPublicar = {}, onCerrar = {}, onIrTablero = {}, onMostrarCodigo = {}, onVerBiblioteca = {},
        )
    }
}
