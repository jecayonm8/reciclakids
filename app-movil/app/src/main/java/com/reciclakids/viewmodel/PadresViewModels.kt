package com.reciclakids.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reciclakids.model.ControlParental
import com.reciclakids.model.HijoVinculado
import com.reciclakids.model.ReporteHijo
import com.reciclakids.network.Respuesta
import com.reciclakids.network.ResultadoVinculacion
import com.reciclakids.network.ServicioPadres
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * UI-30. Cambiar de semana recarga sin volver al esqueleto: la tira de semanas sigue en pantalla
 * y, si falla la red, se queda la semana que ya se veía.
 */
class ReporteHijoViewModel(private val servicio: ServicioPadres, private val hijoId: String) : ViewModel() {

    var estado: EstadoUi<ReporteHijo> by mutableStateOf(EstadoUi.Cargando)
        private set

    /** Lunes de la semana pedida; `null` es la más reciente. */
    var semana: LocalDate? by mutableStateOf(null)
        private set

    /** Hay una semana nueva en camino mientras se sigue viendo la anterior. */
    var cambiandoSemana by mutableStateOf(false)
        private set

    private var trabajo: Job? = null

    fun cargar() {
        val silenciosa = estado.datosCargados != null
        trabajo?.cancel()
        trabajo = viewModelScope.launch {
            if (!silenciosa) estado = EstadoUi.Cargando
            cambiandoSemana = silenciosa
            estado = when (val respuesta = servicio.reporte(hijoId, semana)) {
                is Respuesta.Ok -> if (respuesta.dato.sinDatos) EstadoUi.Vacio(respuesta.dato) else EstadoUi.Contenido(respuesta.dato)
                Respuesta.SinConexion -> if (silenciosa) estado else EstadoUi.Error
            }
            cambiandoSemana = false
        }
    }

    fun elegirSemana(lunes: LocalDate) {
        if (lunes == estado.datosCargados?.semana?.inicio) return
        semana = lunes
        cargar()
    }
}

/** Lo que avisa UI-33 después de cada cambio. */
sealed interface AvisoControl {
    /** Se guardó; [anterior] es lo que restaura «Deshacer». */
    data class Guardado(val anterior: ControlParental) : AvisoControl
    data object NoGuardado : AvisoControl
}

/**
 * UI-33. Cada cambio se guarda solo y en el acto (cambio optimista): si no hay red, el control
 * vuelve a como estaba y se avisa. Después de guardar se ofrece «Deshacer».
 */
class ControlParentalViewModel(private val servicio: ServicioPadres, private val hijoId: String) : ViewModel() {

    var estado: EstadoUi<ControlParental> by mutableStateOf(EstadoUi.Cargando)
        private set

    var aviso: AvisoControl? by mutableStateOf(null)
        private set

    /** Aumenta con cada aviso, para mostrarlo aunque se repita el mismo. */
    var numeroAviso by mutableStateOf(0)
        private set

    fun cargar() {
        if (estado.datosCargados != null) return
        viewModelScope.launch {
            estado = EstadoUi.Cargando
            estado = when (val respuesta = servicio.controlParental(hijoId)) {
                is Respuesta.Ok -> EstadoUi.Contenido(respuesta.dato)
                Respuesta.SinConexion -> EstadoUi.Error
            }
        }
    }

    /** La pantalla ya mostró el aviso: al volver a entrar no se repite. */
    fun avisoAtendido() {
        aviso = null
    }

    fun cambiar(nuevo: ControlParental) {
        val anterior = estado.datosCargados ?: return
        if (nuevo == anterior) return
        guardar(nuevo, siFalla = anterior, alGuardar = AvisoControl.Guardado(anterior))
    }

    fun deshacer(anterior: ControlParental) {
        val actual = estado.datosCargados ?: return
        guardar(anterior, siFalla = actual, alGuardar = null)
    }

    private fun guardar(nuevo: ControlParental, siFalla: ControlParental, alGuardar: AvisoControl?) {
        estado = EstadoUi.Contenido(nuevo)
        viewModelScope.launch {
            if (servicio.guardarControlParental(hijoId, nuevo)) {
                if (alGuardar != null) avisar(alGuardar)
            } else {
                estado = EstadoUi.Contenido(siFalla)
                avisar(AvisoControl.NoGuardado)
            }
        }
    }

    private fun avisar(nuevo: AvisoControl) {
        aviso = nuevo
        numeroAviso++
    }
}

sealed interface EstadoVinculacion {
    data object Editando : EstadoVinculacion
    data object Enviando : EstadoVinculacion
    data object CodigoInvalido : EstadoVinculacion
    data object SinConexion : EstadoVinculacion

    /** [yaEstaba]: el código era de un hijo o hija que ya estaba vinculado. */
    data class Listo(val hijo: HijoVinculado, val yaEstaba: Boolean) : EstadoVinculacion
}

/** Formato del código de vinculación: dos letras del grupo, guion y cuatro caracteres («JB-2M91»). */
private val FormatoCodigoVinculacion = Regex("^[A-Z]{2}-[A-Z0-9]{4}$")

/** Escribe el código como lo entrega la docente: en mayúscula y con el guion en su lugar. */
fun normalizarCodigoVinculacion(texto: String): String {
    val limpio = texto.uppercase().filter { it.isLetterOrDigit() }.take(6)
    return if (limpio.length > 2) limpio.substring(0, 2) + "-" + limpio.substring(2) else limpio
}

fun codigoVinculacionCompleto(codigo: String): Boolean = FormatoCodigoVinculacion.matches(codigo)

/** Vincular otro hijo o hija desde UI-35 con el código que entrega su docente. */
class VincularHijoViewModel(private val servicio: ServicioPadres) : ViewModel() {

    var codigo by mutableStateOf("")
        private set

    var estado: EstadoVinculacion by mutableStateOf(EstadoVinculacion.Editando)
        private set

    fun escribir(texto: String) {
        codigo = normalizarCodigoVinculacion(texto)
        if (estado !is EstadoVinculacion.Enviando) estado = EstadoVinculacion.Editando
    }

    fun vincular() {
        if (!codigoVinculacionCompleto(codigo) || estado is EstadoVinculacion.Enviando) return
        estado = EstadoVinculacion.Enviando
        viewModelScope.launch {
            estado = when (val resultado = servicio.vincular(codigo)) {
                is ResultadoVinculacion.Vinculado -> EstadoVinculacion.Listo(resultado.hijo, yaEstaba = false)
                is ResultadoVinculacion.YaVinculado -> EstadoVinculacion.Listo(resultado.hijo, yaEstaba = true)
                ResultadoVinculacion.CodigoInvalido -> EstadoVinculacion.CodigoInvalido
                ResultadoVinculacion.SinConexion -> EstadoVinculacion.SinConexion
            }
        }
    }
}
