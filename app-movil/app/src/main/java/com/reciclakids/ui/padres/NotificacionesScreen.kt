package com.reciclakids.ui.padres

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reciclakids.R
import com.reciclakids.model.CorreoEnviado
import com.reciclakids.model.HijoVinculado
import com.reciclakids.model.HistorialCorreos
import com.reciclakids.model.Insignia
import com.reciclakids.model.TipoCorreo
import com.reciclakids.model.inicioSemana
import com.reciclakids.ui.docente.BarraDocente
import com.reciclakids.ui.docente.BotonContorno
import com.reciclakids.ui.docente.EsqueletoCarga
import com.reciclakids.ui.docente.EstadoErrorRed
import com.reciclakids.ui.docente.EstadoVacioDocente
import com.reciclakids.ui.docente.MarcoDocente
import com.reciclakids.ui.docente.TarjetaDocente
import com.reciclakids.ui.docente.textoPorcentaje
import com.reciclakids.ui.theme.ReciclaKidsTheme
import com.reciclakids.util.horaCorta
import com.reciclakids.viewmodel.EstadoUi
import com.reciclakids.viewmodel.datosCargados
import java.time.LocalDate

/**
 * UI-32 Historial de notificaciones: el registro de cada correo enviado (logros y reporte
 * semanal), con un ícono distinto por tipo y un atajo a las preferencias de correo.
 */
@Composable
fun NotificacionesScreen(
    estado: EstadoUi<HistorialCorreos>,
    onReintentar: () -> Unit,
    onVolver: () -> Unit,
    onPreferencias: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MarcoDocente(
        modifier = modifier,
        barra = {
            BarraDocente(
                titulo = stringResource(R.string.avisos_titulo),
                subtitulo = estado.datosCargados?.hijo?.nombre,
                onAtras = onVolver,
            )
        },
    ) { relleno ->
        when (estado) {
            EstadoUi.Cargando -> EsqueletoCarga(Modifier.padding(relleno))
            EstadoUi.Error -> EstadoErrorRed(
                onReintentar = onReintentar,
                modifier = Modifier.padding(relleno),
                titulo = stringResource(R.string.padres_error_titulo),
                texto = stringResource(R.string.padres_error_texto),
            )
            is EstadoUi.Vacio -> EstadoVacioDocente(
                ilustracion = null,
                titulo = stringResource(R.string.avisos_vacio_titulo),
                texto = stringResource(R.string.avisos_vacio_texto),
                modifier = Modifier.padding(relleno),
            ) {
                BotonContorno(stringResource(R.string.avisos_preferencias), onClick = onPreferencias)
            }
            is EstadoUi.Contenido -> ListaCorreos(estado.datos, onPreferencias, relleno)
        }
    }
}

@Composable
private fun ListaCorreos(historial: HistorialCorreos, onPreferencias: () -> Unit, relleno: PaddingValues) {
    val semana = inicioSemana(historial.hoy)
    val (estaSemana, antes) = historial.correos.partition { !it.enviado.toLocalDate().isBefore(semana) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(relleno),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (estaSemana.isNotEmpty()) {
            item { Encabezado(stringResource(R.string.avisos_esta_semana)) }
            items(estaSemana) { FilaCorreo(it, historial.hoy) }
        }
        if (antes.isNotEmpty()) {
            item { Encabezado(stringResource(R.string.avisos_antes)) }
            items(antes) { FilaCorreo(it, historial.hoy) }
        }
        item {
            BotonContorno(
                stringResource(R.string.avisos_preferencias),
                onClick = onPreferencias,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun Encabezado(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 2.dp, vertical = 6.dp).semantics { heading() },
    )
}

@Composable
private fun FilaCorreo(correo: CorreoEnviado, hoy: LocalDate) {
    val esLogro = correo.tipo == TipoCorreo.Logro
    TarjetaDocente(Modifier.fillMaxWidth(), relleno = PaddingValues(14.dp)) {
        Row(
            modifier = Modifier.semantics(mergeDescendants = true) { },
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .background(if (esLogro) Color(0xFFFFF3D6) else MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(if (esLogro) R.drawable.ic_insignia else R.drawable.ic_reportes),
                    contentDescription = null,
                    tint = if (esLogro) Color(0xFF8A6410) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                val insignia = correo.insignia
                val (titulo, detalle) = if (esLogro && insignia != null) {
                    stringResource(R.string.correo_logro_titulo, insignia.nombre) to insignia.paraPadres()
                } else {
                    stringResource(R.string.correo_reporte_titulo) to detalleReporte(correo)
                }
                Text(titulo, style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp))
                Text(detalle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val cuando = stringResource(R.string.cuando_dia_hora, textoCuando(correo.enviado.toLocalDate(), hoy), horaCorta(correo.enviado.toLocalTime()))
                Text(
                    stringResource(R.string.correo_enviado, cuando, correo.destinatario),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun detalleReporte(correo: CorreoEnviado): String {
    val aciertos = correo.aciertos
    return if (aciertos != null) {
        stringResource(R.string.correo_reporte_detalle, correo.retosCompletados, correo.retosPublicados, textoPorcentaje(aciertos))
    } else {
        stringResource(R.string.correo_reporte_detalle_sin_aciertos, correo.retosCompletados, correo.retosPublicados)
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun NotificacionesPreview() {
    val hoy = LocalDate.of(2026, 9, 18)
    val salome = HijoVinculado("nino-1", "Salomé M.", "Jardín B", 5, "Pulpo azul", LocalDate.of(2026, 8, 21))
    val correo = "mariana.r@correo.com"
    ReciclaKidsTheme {
        NotificacionesScreen(
            estado = EstadoUi.Contenido(
                HistorialCorreos(
                    salome,
                    hoy,
                    listOf(
                        CorreoEnviado(TipoCorreo.ReporteSemanal, hoy.atTime(17, 0), correo, retosCompletados = 4, retosPublicados = 5, aciertos = 83),
                        CorreoEnviado(TipoCorreo.Logro, hoy.minusDays(1).atTime(18, 10), correo, insignia = Insignia.AmigaTortuga),
                        CorreoEnviado(TipoCorreo.ReporteSemanal, hoy.minusDays(7).atTime(17, 0), correo, retosCompletados = 3, retosPublicados = 5, aciertos = 72),
                    ),
                )
            ),
            onReintentar = {}, onVolver = {}, onPreferencias = {},
        )
    }
}
