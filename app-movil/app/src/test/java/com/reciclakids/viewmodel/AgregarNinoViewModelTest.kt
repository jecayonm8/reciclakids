package com.reciclakids.viewmodel

import com.reciclakids.model.NinoAgregado
import com.reciclakids.model.inicialDe
import com.reciclakids.model.nombreConInicial
import com.reciclakids.network.ResultadoAgregarNino
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

/** Robolectric da el hilo principal que usa viewModelScope. */
@RunWith(RobolectricTestRunner::class)
class AgregarNinoViewModelTest {

    private val enviados = mutableListOf<Pair<String, String>>()
    private var respuesta: ResultadoAgregarNino = ResultadoAgregarNino.Agregado(NinoAgregado("nino-11", "Pedro Q.", "JB-7KQ4"))
    private val vm = AgregarNinoViewModel { nombre, inicial ->
        enviados += nombre to inicial
        respuesta
    }

    private fun esperar() = ShadowLooper.idleMainLooper()

    @Test
    fun delApellidoSoloQuedaLaInicialEnMayuscula() {
        assertEquals("Salomé M.", nombreConInicial(" Salomé ", "m"))
        assertEquals("Q", inicialDe("quintero"))
        assertEquals("Ñ", inicialDe("ñ"))
        assertEquals("", inicialDe(" 3 "))
    }

    @Test
    fun soloSeEnvianElNombreYUnaLetra() {
        vm.guardar()
        assertTrue(enviados.isEmpty())
        // Tocar «Agregar al grupo» con todo vacío marca los dos campos.
        assertTrue(vm.formulario.intentoGuardar)
        assertEquals(ErrorCampoNino.Vacio, vm.formulario.errorNombre)
        assertEquals(ErrorCampoNino.Vacio, vm.formulario.errorInicial)

        vm.formulario.nombre = " Pedro "
        vm.formulario.escribirInicial("Quintero")
        assertTrue(vm.formulario.valido)
        assertEquals("Pedro Q.", vm.formulario.nombreGuardado)
        vm.guardar()
        esperar()

        assertEquals(listOf("Pedro" to "Q"), enviados)
        assertEquals(EstadoAgregarNino.Agregado(NinoAgregado("nino-11", "Pedro Q.", "JB-7KQ4")), vm.estado)
    }

    @Test
    fun sinConexionAvisaYAgregarOtroEmpiezaEnBlanco() {
        respuesta = ResultadoAgregarNino.SinConexion
        vm.formulario.nombre = "Pedro"
        vm.formulario.escribirInicial("Q")

        vm.guardar()
        esperar()
        assertEquals(EstadoAgregarNino.SinConexion, vm.estado)

        vm.otro()
        assertEquals(EstadoAgregarNino.Editando, vm.estado)
        assertEquals("", vm.formulario.nombre)
        assertFalse(vm.formulario.valido)
        assertFalse(vm.formulario.intentoGuardar)
    }

    @Test
    fun unNombreConSimbolosNoSeEnvia() {
        vm.formulario.nombre = "<b>Pedro</b>"
        vm.formulario.escribirInicial("Q")

        vm.guardar()

        assertEquals(ErrorCampoNino.Simbolos, vm.formulario.errorNombre)
        assertTrue(enviados.isEmpty())
        vm.formulario.nombre = "María-José D'Ávila"
        assertTrue(vm.formulario.valido)
    }

    @Test
    fun unNombreRepetidoQuedaMarcado() {
        respuesta = ResultadoAgregarNino.Repetido
        vm.formulario.nombre = "Salomé"
        vm.formulario.escribirInicial("M")

        vm.guardar()
        esperar()

        assertEquals(EstadoAgregarNino.Repetido("Salomé M."), vm.estado)
    }
}
