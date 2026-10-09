package com.reciclakids.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reciclakids.model.EstadoReto
import com.reciclakids.model.Reto
import com.reciclakids.network.Respuesta
import com.reciclakids.network.ResultadoPublicacion
import com.reciclakids.network.ServicioDocente
import kotlinx.coroutines.launch

/** Desde dónde se abre el asistente. */
enum class OrigenAsistente {
    Nuevo,

    /** Abre de una vez la hoja «Reutilizar un reto» (tablero y paso 1). */
    Reutilizar,

    /** «Duplicar» desde la biblioteca: copia el reto y salta a la vista previa. */
    Duplicar,

    /** «Editar» un reto programado o un borrador. */
    Editar,
}

sealed interface EstadoPublicacion {
    data object Ninguna : EstadoPublicacion
    data object Enviando : EstadoPublicacion
    data class Publicado(val reto: Reto) : EstadoPublicacion

    /** Sin conexión: quedó como borrador en el dispositivo y se puede reintentar. */
    data class Fallida(val borrador: Reto) : EstadoPublicacion
}

/** UI-22: lleva el [AsistenteReto] y la publicación. La meta es publicar en menos de un minuto. */
class CrearRetoViewModel(
    private val servicio: ServicioDocente,
    private val origen: OrigenAsistente,
    private val idReto: String?,
) : ViewModel() {

    var asistente by mutableStateOf(AsistenteReto(servicio.hoy()))
        private set

    var hojaReutilizar by mutableStateOf(false)
        private set

    /** Retos publicados que se pueden reutilizar. */
    var previos: EstadoUi<List<Reto>> by mutableStateOf(EstadoUi.Cargando)
        private set

    /** Mientras se trae el reto que se duplica o se edita. */
    var origenPendiente: EstadoUi<Unit>? by mutableStateOf(null)
        private set

    var publicacion: EstadoPublicacion by mutableStateOf(EstadoPublicacion.Ninguna)
        private set

    init {
        if (origen == OrigenAsistente.Reutilizar) abrirReutilizar()
        if (idReto != null && (origen == OrigenAsistente.Duplicar || origen == OrigenAsistente.Editar)) cargarOrigen()
    }

    val editando: Boolean get() = asistente.idExistente != null && origen == OrigenAsistente.Editar

    fun actualizar(cambio: (AsistenteReto) -> AsistenteReto) {
        asistente = cambio(asistente)
    }

    fun abrirReutilizar() {
        hojaReutilizar = true
        if (previos !is EstadoUi.Contenido) cargarPrevios()
    }

    fun cerrarReutilizar() {
        hojaReutilizar = false
    }

    fun usar(reto: Reto) {
        asistente = asistente.reutilizando(reto)
        hojaReutilizar = false
    }

    fun cargarPrevios() {
        viewModelScope.launch {
            previos = EstadoUi.Cargando
            previos = when (val respuesta = servicio.biblioteca()) {
                is Respuesta.Ok -> {
                    val publicados = respuesta.dato.retos.filter { it.estado == EstadoReto.Publicado }
                    if (publicados.isEmpty()) EstadoUi.Vacio(publicados) else EstadoUi.Contenido(publicados)
                }
                Respuesta.SinConexion -> EstadoUi.Error
            }
        }
    }

    fun cargarOrigen() {
        val id = idReto ?: return
        viewModelScope.launch {
            origenPendiente = EstadoUi.Cargando
            origenPendiente = when (val respuesta = servicio.reto(id)) {
                is Respuesta.Ok -> {
                    respuesta.dato?.let { reto ->
                        asistente = if (origen == OrigenAsistente.Editar) asistente.editando(reto) else asistente.reutilizando(reto)
                    }
                    null
                }
                Respuesta.SinConexion -> EstadoUi.Error
            }
        }
    }

    fun publicar() {
        if (publicacion == EstadoPublicacion.Enviando || !asistente.tieneCategorias) return
        publicacion = EstadoPublicacion.Enviando
        viewModelScope.launch {
            publicacion = when (val resultado = servicio.publicar(asistente.aBorrador())) {
                is ResultadoPublicacion.Publicado -> EstadoPublicacion.Publicado(resultado.reto)
                is ResultadoPublicacion.GuardadoComoBorrador -> {
                    // Reintentar actualiza ese mismo borrador en vez de crear otro.
                    asistente = asistente.copy(idExistente = resultado.reto.id)
                    EstadoPublicacion.Fallida(resultado.reto)
                }
            }
        }
    }

    fun cerrarResultado() {
        publicacion = EstadoPublicacion.Ninguna
    }
}
