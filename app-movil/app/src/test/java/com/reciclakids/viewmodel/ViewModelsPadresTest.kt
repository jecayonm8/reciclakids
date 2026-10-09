package com.reciclakids.viewmodel

import com.reciclakids.model.ControlParental
import com.reciclakids.model.CuentaAdulto
import com.reciclakids.model.RolAdulto
import com.reciclakids.network.ControlesParentales
import com.reciclakids.network.DatosDemoPadres
import com.reciclakids.network.ServicioPadresEnMemoria
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

/** Robolectric da el hilo principal que usa viewModelScope. */
@RunWith(RobolectricTestRunner::class)
class ViewModelsPadresTest {

    private val viernes = LocalDate.of(2026, 9, 18)
    private val reloj = Clock.fixed(viernes.atTime(18, 30).toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
    private val cuenta = CuentaAdulto("Mariana Ríos", "mariana.r@correo.com", RolAdulto.Acudiente)
    private val controles = ControlesParentales()
    private val servicio = ServicioPadresEnMemoria(cuenta, controles, latenciaMs = 0, reloj = reloj)
    private val salome = DatosDemoPadres.IdSalome

    private fun esperar() = ShadowLooper.idleMainLooper()

    // --- ReporteHijoViewModel ---

    @Test
    fun cambiarDeSemanaNoVuelveAlEsqueletoYSiFallaLaRedSeQuedaLaQueSeVeia() {
        val vm = ReporteHijoViewModel(servicio, salome)
        vm.cargar()
        esperar()
        assertEquals(LocalDate.of(2026, 9, 14), vm.estado.datosCargados?.semana?.inicio)

        vm.elegirSemana(LocalDate.of(2026, 9, 7))
        assertTrue(vm.estado is EstadoUi.Contenido)
        esperar()
        assertEquals(LocalDate.of(2026, 9, 7), vm.estado.datosCargados?.semana?.inicio)
        assertFalse(vm.cambiandoSemana)

        servicio.sinConexion = true
        vm.elegirSemana(LocalDate.of(2026, 8, 31))
        esperar()
        assertEquals(LocalDate.of(2026, 9, 7), vm.estado.datosCargados?.semana?.inicio)
    }

    @Test
    fun unHijoSinJuegoMuestraElReporteVacio() {
        val vm = ReporteHijoViewModel(servicio, DatosDemoPadres.IdMartin)
        vm.cargar()
        esperar()

        assertTrue(vm.estado is EstadoUi.Vacio)
    }

    // --- ControlParentalViewModel ---

    @Test
    fun cadaCambioSeGuardaSoloYSePuedeDeshacer() {
        val vm = ControlParentalViewModel(servicio, salome)
        vm.cargar()
        esperar()

        vm.cambiar(ControlParental().conMinutos(25))
        esperar()
        assertEquals(25, controles.de(salome).limiteMinutosDiarios)
        val aviso = vm.aviso as AvisoControl.Guardado
        assertEquals(20, aviso.anterior.limiteMinutosDiarios)
        assertEquals(1, vm.numeroAviso)

        vm.avisoAtendido()
        vm.deshacer(aviso.anterior)
        esperar()
        assertEquals(20, controles.de(salome).limiteMinutosDiarios)
        assertEquals(20, vm.estado.datosCargados?.limiteMinutosDiarios)
        // Deshacer no ofrece deshacer otra vez.
        assertNull(vm.aviso)
    }

    @Test
    fun sinConexionElControlVuelveAComoEstabaYSeAvisa() {
        val vm = ControlParentalViewModel(servicio, salome)
        vm.cargar()
        esperar()

        servicio.sinConexion = true
        vm.cambiar(ControlParental(correosLogro = false))
        esperar()

        assertEquals(true, vm.estado.datosCargados?.correosLogro)
        assertEquals(AvisoControl.NoGuardado, vm.aviso)
        assertEquals(ControlParental(), controles.de(salome))
    }

    // --- VincularHijoViewModel ---

    @Test
    fun elCodigoSeEscribeEnMayusculaYConElGuionEnSuLugar() {
        assertEquals("JB-2M91", normalizarCodigoVinculacion("jb2m91"))
        assertEquals("JB-2M91", normalizarCodigoVinculacion("jb-2m91"))
        assertEquals("JB-2", normalizarCodigoVinculacion("j b 2"))
        assertEquals("JB", normalizarCodigoVinculacion("JB-"))
        assertEquals("JB-2M91", normalizarCodigoVinculacion("JB-2M91XYZ"))
        assertTrue(codigoVinculacionCompleto("JB-2M91"))
        assertFalse(codigoVinculacionCompleto("JB-2M9"))
    }

    @Test
    fun vincularConUnCodigoValidoTerminaListoYUnoInvalidoLoDice() {
        val vm = VincularHijoViewModel(servicio)

        vm.escribir("zz0000")
        vm.vincular()
        esperar()
        assertEquals(EstadoVinculacion.CodigoInvalido, vm.estado)

        vm.escribir("jb5h8w")
        assertEquals(EstadoVinculacion.Editando, vm.estado)
        vm.vincular()
        esperar()
        val listo = vm.estado as EstadoVinculacion.Listo
        assertEquals("Emilio R.", listo.hijo.nombre)
        assertFalse(listo.yaEstaba)
    }

    @Test
    fun unCodigoIncompletoNoSeEnvia() {
        val vm = VincularHijoViewModel(servicio)

        vm.escribir("JB-2M")
        vm.vincular()
        esperar()

        assertEquals(EstadoVinculacion.Editando, vm.estado)
    }
}
