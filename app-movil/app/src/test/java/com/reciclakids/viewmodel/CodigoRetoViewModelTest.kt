package com.reciclakids.viewmodel

import com.reciclakids.model.Dificultad
import com.reciclakids.network.ServicioRetosEnMemoria
import com.reciclakids.ui.common.EstadoCodigo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class CodigoRetoViewModelTest {

    private val hoy = LocalDate.of(2026, 10, 8)
    private lateinit var vm: CodigoRetoViewModel

    @Before
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        vm = CodigoRetoViewModel(ServicioRetosEnMemoria(latenciaMs = 0), hoy = { hoy })
    }

    @After
    fun limpiar() = Dispatchers.resetMain()

    private fun escribir(codigo: String) = codigo.forEach(vm::escribir)

    @Test
    fun soloAdmiteCuatroDigitos() {
        escribir("472913")

        assertEquals("4729", vm.codigo)
    }

    @Test
    fun borrarQuitaElUltimoDigitoYVuelveAEditar() {
        escribir("1235")
        vm.confirmar()
        assertEquals(EstadoCodigo.Invalido, vm.estado)

        vm.borrar()

        assertEquals("123", vm.codigo)
        assertEquals(EstadoCodigo.Editando, vm.estado)
    }

    @Test
    fun codigoIncompletoNoSeConsultaYSeMuestraComoInvalido() {
        escribir("47")
        vm.confirmar()

        assertEquals(EstadoCodigo.Invalido, vm.estado)
        assertNull(vm.reto)
    }

    @Test
    fun codigoDelDiaAbreElReto() {
        escribir("4729")
        vm.confirmar()

        assertEquals(EstadoCodigo.Correcto, vm.estado)
        assertEquals(Dificultad.Medio, vm.reto?.dificultad)
        assertEquals(hoy, vm.reto?.fecha)
    }

    @Test
    fun codigoDeAyerEstaVencido() {
        escribir("1234")
        vm.confirmar()

        assertEquals(EstadoCodigo.Vencido, vm.estado)
        assertNull(vm.reto)
    }

    @Test
    fun codigoQueNoExisteEsInvalido() {
        escribir("0000")
        vm.confirmar()

        assertEquals(EstadoCodigo.Invalido, vm.estado)
    }

    @Test
    fun conElCodigoCorrectoElTecladoYaNoCambiaNada() {
        escribir("4729")
        vm.confirmar()

        vm.borrar()
        vm.escribir('1')

        assertEquals("4729", vm.codigo)
        assertEquals(EstadoCodigo.Correcto, vm.estado)
    }
}
