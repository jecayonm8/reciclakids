package com.reciclakids.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reciclakids.model.Reto
import com.reciclakids.network.ResultadoCodigo
import com.reciclakids.network.ServicioRetos
import com.reciclakids.ui.common.EstadoCodigo
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Longitud del código del reto que da la docente. */
const val LongitudCodigoReto = 4

/**
 * UI-08 Código del reto. El niño escribe los cuatro dígitos y confirma; un código equivocado
 * o vencido nunca se muestra como error, solo cambia el mensaje y el borde de las casillas.
 */
class CodigoRetoViewModel(
    private val servicio: ServicioRetos,
    private val hoy: () -> LocalDate = LocalDate::now,
) : ViewModel() {

    var codigo by mutableStateOf("")
        private set
    var estado by mutableStateOf(EstadoCodigo.Editando)
        private set

    /** El reto encontrado; la pantalla avanza a «¿Quién eres?» cuando deja de ser null. */
    var reto by mutableStateOf<Reto?>(null)
        private set

    private var verificando = false

    fun escribir(digito: Char) {
        if (estado == EstadoCodigo.Correcto || codigo.length >= LongitudCodigoReto) return
        codigo += digito
        estado = EstadoCodigo.Editando
    }

    fun borrar() {
        if (estado == EstadoCodigo.Correcto) return
        codigo = codigo.dropLast(1)
        estado = EstadoCodigo.Editando
    }

    fun confirmar() {
        if (estado == EstadoCodigo.Correcto || verificando) return
        if (codigo.length < LongitudCodigoReto) {
            estado = EstadoCodigo.Invalido
            return
        }
        verificando = true
        viewModelScope.launch {
            when (val resultado = servicio.buscarReto(codigo, hoy())) {
                is ResultadoCodigo.Valido -> {
                    estado = EstadoCodigo.Correcto
                    reto = resultado.reto
                }
                ResultadoCodigo.Invalido -> estado = EstadoCodigo.Invalido
                ResultadoCodigo.Vencido -> estado = EstadoCodigo.Vencido
            }
            verificando = false
        }
    }
}
