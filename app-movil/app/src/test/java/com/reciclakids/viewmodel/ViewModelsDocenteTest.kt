package com.reciclakids.viewmodel

import com.reciclakids.model.CuentaAdulto
import com.reciclakids.model.EstadoReto
import com.reciclakids.model.RolAdulto
import com.reciclakids.network.Respuesta
import com.reciclakids.network.ServicioDocente
import com.reciclakids.network.ServicioDocenteEnMemoria
import com.reciclakids.network.ServicioPadresEnMemoria
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
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
class ViewModelsDocenteTest {

    private val martes = LocalDate.of(2026, 9, 22)
    private val reloj = Clock.fixed(martes.atTime(9, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
    private val cuenta = CuentaAdulto("Laura Restrepo", "laura.r@jardin.edu.co", RolAdulto.Docente, "Gotitas", "Jardín B")
    private val servicio = ServicioDocenteEnMemoria(cuenta, latenciaMs = 0, reloj = reloj)

    private fun esperar() = ShadowLooper.idleMainLooper()

    // --- CargaViewModel ---

    @Test
    fun laCargaClasificaLaRespuestaEnContenidoVacioOError() {
        val conDatos = CargaViewModel<List<Int>>(fuente = { Respuesta.Ok(listOf(1)) }, esVacio = { it.isEmpty() })
        val vacio = CargaViewModel<List<Int>>(fuente = { Respuesta.Ok(emptyList()) }, esVacio = { it.isEmpty() })
        val sinRed = CargaViewModel<List<Int>>(fuente = { Respuesta.SinConexion })

        listOf(conDatos, vacio, sinRed).forEach { it.cargar() }
        esperar()

        assertEquals(EstadoUi.Contenido(listOf(1)), conDatos.estado)
        assertEquals(EstadoUi.Vacio(emptyList<Int>()), vacio.estado)
        assertEquals(EstadoUi.Error, sinRed.estado)
    }

    @Test
    fun unaRecargaSinConexionConservaLoQueYaSeMostraba() {
        var respuesta: Respuesta<Int> = Respuesta.Ok(7)
        val vm = CargaViewModel(fuente = { respuesta })
        vm.cargar()
        esperar()

        respuesta = Respuesta.SinConexion
        vm.cargar()
        esperar()

        assertEquals(EstadoUi.Contenido(7), vm.estado)
    }

    // --- CrearRetoViewModel ---

    @Test
    fun publicarUnRetoNuevoDejaElCodigoDelDia() {
        val vm = CrearRetoViewModel(servicio, OrigenAsistente.Nuevo, null)

        vm.publicar()
        esperar()

        val publicado = (vm.publicacion as EstadoPublicacion.Publicado).reto
        assertNotNull(publicado.codigoActivo(martes))
    }

    @Test
    fun siFallaLaPublicacionReintentarActualizaElMismoBorrador() {
        val vm = CrearRetoViewModel(servicio, OrigenAsistente.Nuevo, null)
        servicio.sinConexion = true
        vm.publicar()
        esperar()
        val borrador = (vm.publicacion as EstadoPublicacion.Fallida).borrador
        assertEquals(borrador.id, vm.asistente.idExistente)

        servicio.sinConexion = false
        vm.publicar()
        esperar()

        val publicado = (vm.publicacion as EstadoPublicacion.Publicado).reto
        assertEquals(borrador.id, publicado.id)
        assertEquals(EstadoReto.Publicado, publicado.estado)
    }

    @Test
    fun duplicarCopiaElRetoYAbreLaVistaPrevia() {
        val vm = CrearRetoViewModel(servicio, OrigenAsistente.Duplicar, "reto-envases")
        esperar()

        assertNull(vm.origenPendiente)
        assertEquals(AsistenteReto.PasoVistaPrevia, vm.asistente.paso)
        assertEquals("reto-envases", vm.asistente.reutilizadoDe)
        assertNull(vm.asistente.idExistente)
    }

    @Test
    fun editarConservaElIdDelProgramado() {
        val vm = CrearRetoViewModel(servicio, OrigenAsistente.Editar, "reto-refrigerio")
        esperar()

        assertTrue(vm.editando)
        assertEquals("reto-refrigerio", vm.asistente.idExistente)
        assertTrue(vm.asistente.programado)
    }

    @Test
    fun reutilizarAbreLaHojaConLosRetosPublicados() {
        val vm = CrearRetoViewModel(servicio, OrigenAsistente.Reutilizar, null)
        esperar()

        assertTrue(vm.hojaReutilizar)
        val previos = (vm.previos as EstadoUi.Contenido).datos
        assertTrue(previos.all { it.estado == EstadoReto.Publicado })
        assertEquals(3, previos.size)

        vm.usar(previos[1])
        assertEquals(false, vm.hojaReutilizar)
        assertEquals(previos[1].id, vm.asistente.reutilizadoDe)
    }

    // --- SesionViewModel ---

    @Test
    fun laSesionDeUnaDocenteConservaSuServicioAlVolverAEntrar() {
        val sesion = SesionViewModel()
        var creados = 0
        val crear: (CuentaAdulto) -> ServicioDocente = {
            creados++
            ServicioDocenteEnMemoria(it, latenciaMs = 0)
        }

        sesion.iniciar(cuenta, crear) { error("no se usa") }
        val primero = sesion.servicioDocente
        sesion.cerrar()
        assertNull(sesion.servicioDocente)
        sesion.iniciar(cuenta, crear) { error("no se usa") }

        assertSame(primero, sesion.servicioDocente)
        assertEquals(1, creados)
    }

    @Test
    fun unaSesionDeAcudienteTieneServicioDePadresYNoDeDocente() {
        val sesion = SesionViewModel()
        val acudiente = CuentaAdulto("Mariana Ríos", "mariana.r@correo.com", RolAdulto.Acudiente)
        var creados = 0

        sesion.iniciar(acudiente, { error("no se usa") }) {
            creados++
            ServicioPadresEnMemoria(it, latenciaMs = 0)
        }
        val primero = sesion.servicioPadres
        sesion.cerrar()
        assertNull(sesion.servicioPadres)
        sesion.iniciar(acudiente, { error("no se usa") }) { error("ya existe") }

        assertNull(sesion.servicioDocente)
        assertSame(primero, sesion.servicioPadres)
        assertEquals(1, creados)
        assertEquals(RolAdulto.Acudiente, sesion.cuenta?.rol)
    }
}
