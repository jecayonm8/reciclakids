package com.reciclakids.ui.docente

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reciclakids.R
import com.reciclakids.model.CategoriaResiduo
import com.reciclakids.model.Dificultad
import com.reciclakids.model.EstadoReto
import com.reciclakids.model.RetoDocente
import com.reciclakids.model.residuosPorReto
import com.reciclakids.ui.common.EstiloMarcador
import com.reciclakids.ui.common.FiguraDigito
import com.reciclakids.ui.common.MarcadorIlustracion
import com.reciclakids.ui.common.caneca
import com.reciclakids.ui.common.flotar
import com.reciclakids.ui.theme.Duracion
import com.reciclakids.util.diaCorto
import java.time.LocalDate

// Modos adultos: Material 3 estándar, objetivos de 48 dp, texto desde 14 sp y contraste AA.
// El rojo solo aparece en errores de red y acciones destructivas.

internal val FormaTarjeta = RoundedCornerShape(16.dp)
private val FormaChip = RoundedCornerShape(8.dp)

/** Borde de una opción sin elegir en el asistente. */
internal val BordeOpcion = Color(0xFFBFC8CB)

/** Valor bajo en gráficos: ámbar, nunca rojo. */
internal val ColorValorBajo = Color(0xFFE8A33D)

/** Barras de la evolución semanal de un niño. */
internal val ColorEvolucion = Color(0xFF16A3B8)

internal val AltoMinimoBoton = 48.dp

// ---------- Estructura de pantalla ----------

/**
 * Scaffold de una pantalla docente. En las pestañas la NavigationBar va debajo y ya se encarga
 * del borde inferior; las pantallas completas (asistente, código, detalle) lo manejan aquí.
 */
@Composable
internal fun MarcoDocente(
    barra: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    pantallaCompleta: Boolean = false,
    snackbar: SnackbarHostState? = null,
    botonFlotante: @Composable () -> Unit = {},
    barraInferior: @Composable () -> Unit = {},
    contenido: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = barra,
        bottomBar = barraInferior,
        floatingActionButton = botonFlotante,
        snackbarHost = { if (snackbar != null) SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = if (pantallaCompleta) ScaffoldDefaults.contentWindowInsets else WindowInsets(0, 0, 0, 0),
        content = contenido,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BarraDocente(
    titulo: String,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    onAtras: (() -> Unit)? = null,
    @DrawableRes iconoAtras: Int = R.drawable.ic_atras,
    descripcionAtras: String = stringResource(R.string.comun_atras),
    inicioTitulo: (@Composable () -> Unit)? = null,
    acciones: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                inicioTitulo?.invoke()
                Column {
                    Text(
                        titulo,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = if (subtitulo != null) 20.sp else 18.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (subtitulo != null) {
                        Text(
                            subtitulo,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        },
        modifier = modifier,
        navigationIcon = {
            if (onAtras != null) {
                IconButton(onClick = onAtras) {
                    Icon(painterResource(iconoAtras), contentDescription = descripcionAtras)
                }
            }
        },
        actions = acciones,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    )
}

/** Columna centrada que se desplaza si no cabe (fuente al 200 %). */
@Composable
internal fun PantallaCentrada(
    modifier: Modifier = Modifier,
    relleno: Dp = 32.dp,
    espacio: Dp = 18.dp,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
                .heightIn(min = maxHeight)
                .padding(relleno),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(espacio, Alignment.CenterVertically),
            content = contenido,
        )
    }
}

// ---------- Estados obligatorios: carga, vacío y error de red ----------

@Composable
internal fun EsqueletoCarga(modifier: Modifier = Modifier) {
    val descripcion = stringResource(R.string.docente_cargando)
    val transicion = rememberInfiniteTransition(label = "esqueleto")
    val alfa = transicion.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(700, easing = EaseInOut), RepeatMode.Reverse),
        label = "esqueleto",
    )
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .clearAndSetSemantics { contentDescription = descripcion },
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BloqueEsqueleto(Modifier.fillMaxWidth().height(132.dp), alfa)
        BloqueEsqueleto(Modifier.fillMaxWidth().height(160.dp), alfa)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            BloqueEsqueleto(Modifier.weight(1f).height(96.dp), alfa)
            BloqueEsqueleto(Modifier.weight(1f).height(96.dp), alfa)
        }
        BloqueEsqueleto(Modifier.fillMaxWidth(0.6f).height(18.dp), alfa, radio = 6.dp)
        BloqueEsqueleto(Modifier.fillMaxWidth(0.4f).height(18.dp), alfa, radio = 6.dp)
    }
}

@Composable
private fun BloqueEsqueleto(modifier: Modifier, alfa: State<Float>, radio: Dp = 16.dp) {
    Box(
        modifier
            .graphicsLayer { alpha = alfa.value }
            .background(Color(0xFFE1E9EC), RoundedCornerShape(radio))
    )
}

@Composable
internal fun EstadoErrorRed(onReintentar: () -> Unit, modifier: Modifier = Modifier) {
    PantallaCentrada(modifier) {
        Box(
            Modifier.size(120.dp).background(MaterialTheme.colorScheme.errorContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(R.drawable.ic_wifi_off),
                contentDescription = null,
                modifier = Modifier.size(52.dp),
                tint = MaterialTheme.colorScheme.error,
            )
        }
        Text(
            stringResource(R.string.docente_error_titulo),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.docente_error_texto),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        BotonLleno(stringResource(R.string.docente_reintentar), onClick = onReintentar)
    }
}

@Composable
internal fun EstadoVacioDocente(
    ilustracion: String?,
    titulo: String,
    texto: String,
    modifier: Modifier = Modifier,
    flotante: Boolean = false,
    acciones: @Composable ColumnScope.() -> Unit = {},
) {
    PantallaCentrada(modifier) {
        if (ilustracion != null) {
            MarcadorIlustracion(
                etiqueta = ilustracion,
                modifier = Modifier
                    .then(if (flotante) Modifier.flotar(amplitud = 8.dp) else Modifier)
                    .size(150.dp)
                    .clearAndSetSemantics { },
                estilo = EstiloMarcador.Adulto,
            )
        }
        Text(
            titulo,
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp, lineHeight = 32.sp),
            textAlign = TextAlign.Center,
        )
        Text(
            texto,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        acciones()
    }
}

// ---------- Botones ----------

private val EstiloBoton: TextStyle
    @Composable get() = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp)

@Composable
internal fun BotonLleno(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    cargando: Boolean = false,
    @DrawableRes icono: Int? = null,
) {
    Button(
        onClick = { if (!cargando) onClick() },
        modifier = modifier.heightIn(min = AltoMinimoBoton),
        enabled = habilitado,
        contentPadding = PaddingValues(horizontal = 24.dp),
        colors = ButtonDefaults.buttonColors(
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = Color(0xFF6F797B),
        ),
    ) {
        if (cargando) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.5.dp,
            )
        } else {
            if (icono != null) {
                Icon(painterResource(icono), contentDescription = null, modifier = Modifier.padding(end = 8.dp).size(20.dp))
            }
            Text(texto, style = EstiloBoton, textAlign = TextAlign.Center)
        }
    }
}

@Composable
internal fun BotonContorno(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = AltoMinimoBoton),
        enabled = habilitado,
        contentPadding = PaddingValues(horizontal = 20.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
        border = BorderStroke(
            1.dp,
            if (habilitado) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Text(texto, style = EstiloBoton, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun BotonTexto(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    TextButton(onClick = onClick, modifier = modifier.heightIn(min = AltoMinimoBoton)) {
        Text(texto, style = EstiloBoton, color = color, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun BotonTonal(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = AltoMinimoBoton),
        contentPadding = PaddingValues(horizontal = 20.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Text(texto, style = EstiloBoton, textAlign = TextAlign.Center)
    }
}

// ---------- Tarjetas, chips y muestras ----------

@Composable
internal fun TarjetaDocente(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    relleno: PaddingValues = PaddingValues(16.dp),
    espacio: Dp = 10.dp,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    val cuerpo: @Composable () -> Unit = {
        Column(Modifier.padding(relleno), verticalArrangement = Arrangement.spacedBy(espacio), content = contenido)
    }
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = FormaTarjeta, color = Color.White, shadowElevation = 1.dp, content = cuerpo)
    } else {
        Surface(modifier = modifier, shape = FormaTarjeta, color = Color.White, shadowElevation = 1.dp, content = cuerpo)
    }
}

/** Estado con color + texto: nunca solo color. */
@Composable
internal fun ChipEstado(texto: String, fondo: Color, tinta: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .heightIn(min = 28.dp)
            .background(fondo, FormaChip)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(texto, style = MaterialTheme.typography.labelLarge, color = tinta, maxLines = 1)
    }
}

@Composable
internal fun ChipEstadoReto(estado: EstadoReto, modifier: Modifier = Modifier) {
    val esquema = MaterialTheme.colorScheme
    val (fondo, tinta) = when (estado) {
        EstadoReto.Publicado -> esquema.tertiaryContainer to esquema.onTertiaryContainer
        EstadoReto.Programado -> esquema.secondaryContainer to esquema.onSecondaryContainer
        EstadoReto.Borrador -> esquema.surfaceContainer to esquema.onSurfaceVariant
    }
    ChipEstado(estado.etiqueta(), fondo, tinta, modifier)
}

/** Muestra de la caneca: color + borde y, si cabe, su ícono. */
@Composable
internal fun MuestraCaneca(
    categoria: CategoriaResiduo,
    modifier: Modifier = Modifier,
    tamano: Dp = 22.dp,
    radio: Dp = 6.dp,
    borde: Dp = 2.dp,
    conIcono: Boolean = false,
) {
    val tipo = categoria.caneca
    val forma = RoundedCornerShape(radio)
    Box(
        modifier = modifier
            .size(tamano)
            .background(tipo.fondo, forma)
            .border(borde, tipo.borde, forma),
        contentAlignment = Alignment.Center,
    ) {
        if (conIcono) Image(painterResource(tipo.icono), contentDescription = null, modifier = Modifier.size(tamano * 0.55f))
    }
}

@Composable
internal fun AvatarNino(modifier: Modifier = Modifier) {
    MarcadorIlustracion(
        etiqueta = stringResource(R.string.grupo_avatar),
        modifier = modifier.size(44.dp).clearAndSetSemantics { },
        estilo = EstiloMarcador.Avatar,
    )
}

/** Los dígitos del código con su color y su forma, idénticos al teclado del niño. */
@Composable
internal fun FilaDigitos(
    codigo: String,
    ancho: Dp,
    alto: Dp,
    tamanoTexto: TextUnit,
    radioCuadrado: Dp,
    puntaGota: Dp,
    modifier: Modifier = Modifier,
    espacio: Dp = 8.dp,
) {
    val descripcion = stringResource(R.string.codigo_descripcion, codigo.toList().joinToString(" "))
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = descripcion },
        horizontalArrangement = Arrangement.spacedBy(espacio, Alignment.CenterHorizontally),
    ) {
        codigo.forEach { digito ->
            FiguraDigito(
                digito = digito,
                modifier = Modifier.size(ancho, alto),
                tamanoTexto = tamanoTexto,
                radioCuadrado = radioCuadrado,
                puntaGota = puntaGota,
            )
        }
    }
}

// ---------- Gráficos simples ----------

/** Barra que se llena al entrar ([Duracion.estandar] por defecto). */
@Composable
internal fun BarraProgreso(
    fraccion: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    alto: Dp = 8.dp,
    duracionMs: Int = Duracion.estandar,
) {
    val avance = remember { Animatable(0f) }
    LaunchedEffect(fraccion) { avance.animateTo(fraccion.coerceIn(0f, 1f), tween(duracionMs)) }
    val pista = MaterialTheme.colorScheme.surfaceVariant
    Box(
        modifier
            .fillMaxWidth()
            .height(alto)
            .clip(RoundedCornerShape(alto / 2))
            .drawBehind {
                drawRect(pista)
                drawRect(color, size = Size(size.width * avance.value, size.height))
            }
    )
}

/** Fila «nombre · barra · valor»; el valor siempre se escribe, el color solo acompaña. */
@Composable
internal fun FilaBarra(
    etiqueta: String,
    porcentaje: Int,
    color: Color,
    modifier: Modifier = Modifier,
    anchoEtiqueta: Dp = 96.dp,
    inicio: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) { },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        inicio?.invoke()
        Text(
            etiqueta,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(anchoEtiqueta),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        BarraProgreso(porcentaje / 100f, Modifier.weight(1f), color = color, alto = 14.dp, duracionMs = Duracion.enfatica)
        Text(
            textoPorcentaje(porcentaje),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.widthIn(min = 44.dp),
            textAlign = TextAlign.End,
        )
    }
}

/** Barras verticales que crecen en [Duracion.enfatica] al entrar. */
@Composable
internal fun GraficoBarras(
    valores: List<Pair<String, Int>>,
    color: Color,
    modifier: Modifier = Modifier,
    altoMaximo: Dp = 120.dp,
    anchoMaximo: Dp = 56.dp,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        valores.forEach { (etiqueta, valor) ->
            val crecimiento = remember { Animatable(0f) }
            LaunchedEffect(valor) { crecimiento.animateTo(1f, tween(Duracion.enfatica)) }
            Column(
                modifier = Modifier.weight(1f).semantics(mergeDescendants = true) { },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    textoPorcentaje(valor),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Box(
                    Modifier
                        .widthIn(max = anchoMaximo)
                        .fillMaxWidth()
                        .height(altoMaximo * (valor.coerceIn(0, 100) / 100f))
                        .graphicsLayer {
                            scaleY = crecimiento.value
                            transformOrigin = TransformOrigin(0.5f, 1f)
                        }
                        .background(color, RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                )
                Text(
                    etiqueta,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Verde desde 80 %, azul desde 60 % y ámbar por debajo, como en los prototipos. */
@Composable
internal fun colorPorcentaje(valor: Int): Color = when {
    valor >= 80 -> MaterialTheme.colorScheme.tertiary
    valor >= 60 -> MaterialTheme.colorScheme.primary
    else -> ColorValorBajo
}

/** Cambio frente a la semana anterior con flecha: verde si sube, neutro si baja o se mantiene. */
@Composable
internal fun TextoCambio(texto: String, cambio: Int, modifier: Modifier = Modifier) {
    val color = if (cambio > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (cambio != 0) {
            Icon(
                painterResource(if (cambio > 0) R.drawable.ic_tendencia_sube else R.drawable.ic_tendencia_baja),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = color,
            )
        }
        Text(texto, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

// ---------- Textos de un reto ----------

@Composable
internal fun textoPorcentaje(valor: Int): String = stringResource(R.string.porcentaje, valor)

@Composable
internal fun CategoriaResiduo.nombre(): String = stringResource(
    when (this) {
        CategoriaResiduo.Aprovechable -> R.string.categoria_aprovechables
        CategoriaResiduo.NoAprovechable -> R.string.categoria_no_aprovechables
        CategoriaResiduo.Organico -> R.string.categoria_organicos
    }
)

/** En minúscula, para usar dentro de una frase. */
@Composable
internal fun CategoriaResiduo.nombreEnFrase(): String = stringResource(
    when (this) {
        CategoriaResiduo.Aprovechable -> R.string.categoria_aprovechables_frase
        CategoriaResiduo.NoAprovechable -> R.string.categoria_no_aprovechables_frase
        CategoriaResiduo.Organico -> R.string.categoria_organicos_frase
    }
)

@Composable
internal fun CategoriaResiduo.colorCaneca(): String = stringResource(
    when (this) {
        CategoriaResiduo.Aprovechable -> R.string.caneca_blanca_frase
        CategoriaResiduo.NoAprovechable -> R.string.caneca_negra_frase
        CategoriaResiduo.Organico -> R.string.caneca_verde_frase
    }
)

@Composable
internal fun CategoriaResiduo.ejemplos(): String = stringResource(
    when (this) {
        CategoriaResiduo.Aprovechable -> R.string.categoria_aprovechables_ejemplos
        CategoriaResiduo.NoAprovechable -> R.string.categoria_no_aprovechables_ejemplos
        CategoriaResiduo.Organico -> R.string.categoria_organicos_ejemplos
    }
)

/** «Aprovechables + Orgánicos», o «Las tres canecas». */
@Composable
internal fun textoCategorias(categorias: Set<CategoriaResiduo>): String = when {
    categorias.isEmpty() -> stringResource(R.string.categorias_ninguna)
    categorias.size == CategoriaResiduo.entries.size -> stringResource(R.string.categorias_todas)
    else -> categorias.sorted().map { it.nombre() }.joinToString(" + ")
}

@Composable
internal fun Dificultad.nombre(): String = stringResource(
    when (this) {
        Dificultad.Facil -> R.string.dificultad_facil
        Dificultad.Medio -> R.string.dificultad_medio
        Dificultad.Dificil -> R.string.dificultad_dificil
    }
)

/** «Medio · 3 canecas» */
@Composable
internal fun Dificultad.resumen(): String = stringResource(
    when (this) {
        Dificultad.Facil -> R.string.dificultad_facil_resumen
        Dificultad.Medio -> R.string.dificultad_medio_resumen
        Dificultad.Dificil -> R.string.dificultad_dificil_resumen
    }
)

@Composable
internal fun Dificultad.detalle(): String = stringResource(
    when (this) {
        Dificultad.Facil -> R.string.dificultad_facil_detalle
        Dificultad.Medio -> R.string.dificultad_medio_detalle
        Dificultad.Dificil -> R.string.dificultad_dificil_detalle
    }
)

@Composable
internal fun Dificultad.edad(): String = stringResource(
    when (this) {
        Dificultad.Facil -> R.string.dificultad_facil_edad
        Dificultad.Medio -> R.string.dificultad_medio_edad
        Dificultad.Dificil -> R.string.dificultad_dificil_edad
    }
)

@Composable
internal fun textoResiduos(dificultad: Dificultad): String =
    pluralStringResource(R.plurals.residuos, dificultad.residuosPorReto, dificultad.residuosPorReto)

@Composable
internal fun EstadoReto.etiqueta(): String = stringResource(
    when (this) {
        EstadoReto.Publicado -> R.string.estado_publicado
        EstadoReto.Programado -> R.string.estado_programado
        EstadoReto.Borrador -> R.string.estado_borrador
    }
)

/** «hoy», «mié 23» o «sin fecha». */
@Composable
internal fun textoFecha(fecha: LocalDate?, hoy: LocalDate): String = when {
    fecha == null -> stringResource(R.string.reto_sin_fecha)
    fecha == hoy -> stringResource(R.string.reto_fecha_hoy)
    else -> diaCorto(fecha)
}

/** «Aprovechables + Orgánicos · Medio · hoy» */
@Composable
internal fun detalleReto(reto: RetoDocente, hoy: LocalDate): String = stringResource(
    R.string.reto_detalle,
    textoCategorias(reto.categorias),
    reto.dificultad.nombre(),
    textoFecha(reto.fecha, hoy),
)
