package com.reciclakids.viewmodel

import com.reciclakids.model.CuentaAdulto
import com.reciclakids.model.RolAdulto
import com.reciclakids.network.DatosPerfilAcudiente
import com.reciclakids.network.DatosPerfilDocente
import com.reciclakids.network.Respuesta
import com.reciclakids.network.ResultadoEdicionPerfil
import com.reciclakids.network.ServicioDocenteEnMemoria
import com.reciclakids.network.datosEditables
import com.reciclakids.network.map
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

/** Robolectric da el hilo principal que usa viewModelScope. */
@RunWith(RobolectricTestRunner::class)
class EditarPerfilViewModelTest {

    private val cuenta = CuentaAdulto("Laura Restrepo", "laura.r@jardin.edu.co", RolAdulto.Docente, "Gotitas", "Jardín B")
    private val servicio = ServicioDocenteEnMemoria(cuenta, latenciaMs = 0)
    private val laura = DatosPerfilDocente("Laura Restrepo", "laura.r@jardin.edu.co", "Gotitas", "Jardín B")

    private fun esperar() = ShadowLooper.idleMainLooper()

    private fun vmDocente() = EditarPerfilViewModel(
        cargarFormulario = { servicio.perfil().map { FormularioPerfilDocente(it.datosEditables) } },
        guardarFormulario = { servicio.guardarPerfil(it.datos) },
    ).also {
        it.cargar()
        esperar()
    }

    private val EditarPerfilViewModel<FormularioPerfilDocente>.formulario: FormularioPerfilDocente
        get() = estado.datosCargados!!

    @Test
    fun arrancaConLoGuardadoYSinCambiosNoSePuedeGuardar() {
        val vm = vmDocente()

        assertEquals(laura, vm.formulario.datos)
        assertFalse(vm.formulario.listoParaGuardar(vm.guardado))
        // Los espacios de los extremos no cuentan como cambio.
        vm.formulario.nombre = "  Laura Restrepo "
        assertFalse(vm.formulario.hayCambios)
    }

    @Test
    fun conLasMismasReglasDelRegistroUnCampoVacioOUnCorreoMalEscritoNoSeGuardan() {
        val vm = vmDocente()

        vm.formulario.jardin = " "
        assertTrue(vm.formulario.hayCambios)
        assertFalse(vm.formulario.listoParaGuardar(vm.guardado))

        vm.formulario.jardin = "Gotitas del Saber"
        vm.formulario.correo = "laura.r@jardin"
        assertFalse(vm.formulario.listoParaGuardar(vm.guardado))

        vm.formulario.correo = "laura.r@jardin.edu.co"
        assertTrue(vm.formulario.listoParaGuardar(vm.guardado))
    }

    @Test
    fun guardarDejaElPerfilNuevoEnElServicio() {
        val vm = vmDocente()
        vm.formulario.nombre = "Laura Gómez "
        vm.formulario.grupo = "Jardín C"

        vm.guardar()
        esperar()

        assertEquals(EstadoGuardado.Guardado, vm.guardado)
        val perfil = runBlocking { (servicio.perfil() as Respuesta.Ok).dato }
        assertEquals("Laura Gómez", perfil.nombre)
        assertEquals("Jardín C", perfil.grupo)
    }

    @Test
    fun mientrasGuardaNoSeEnviaDosVeces() {
        val respuesta = CompletableDeferred<ResultadoEdicionPerfil>()
        var envios = 0
        val vm = EditarPerfilViewModel(
            cargarFormulario = { Respuesta.Ok(FormularioPerfilDocente(laura)) },
            guardarFormulario = {
                envios++
                respuesta.await()
            },
        )
        vm.cargar()
        esperar()
        vm.estado.datosCargados!!.nombre = "Laura Gómez"

        vm.guardar()
        vm.guardar()
        esperar()
        assertEquals(EstadoGuardado.Guardando, vm.guardado)
        assertEquals(1, envios)

        respuesta.complete(ResultadoEdicionPerfil.Guardado)
        esperar()
        assertEquals(EstadoGuardado.Guardado, vm.guardado)
    }

    @Test
    fun sinConexionAvisaYSePuedeReintentar() {
        val vm = vmDocente()
        vm.formulario.nombre = "Laura Gómez"
        servicio.sinConexion = true

        vm.guardar()
        esperar()
        assertEquals(EstadoGuardado.SinConexion, vm.guardado)
        assertTrue(vm.formulario.listoParaGuardar(vm.guardado))

        servicio.sinConexion = false
        vm.guardar()
        esperar()
        assertEquals(EstadoGuardado.Guardado, vm.guardado)
    }

    @Test
    fun elCorreoEnUsoBloqueaGuardarHastaEscribirOtro() {
        val vm = EditarPerfilViewModel(
            cargarFormulario = { Respuesta.Ok(FormularioPerfilAcudiente(DatosPerfilAcudiente("Mariana Ríos", "mariana.r@correo.com"))) },
            guardarFormulario = { ResultadoEdicionPerfil.CorreoEnUso },
        )
        vm.cargar()
        esperar()
        val formulario = vm.estado.datosCargados!!
        formulario.correo = "ana@correo.com "

        vm.guardar()
        esperar()

        assertEquals(EstadoGuardado.CorreoEnUso("ana@correo.com"), vm.guardado)
        assertTrue(formulario.correoEnUso(vm.guardado))
        assertFalse(formulario.listoParaGuardar(vm.guardado))
        formulario.correo = "mariana.rios@correo.com"
        assertFalse(formulario.correoEnUso(vm.guardado))
        assertTrue(formulario.listoParaGuardar(vm.guardado))
    }

    @Test
    fun sinConexionAlCargarMuestraElErrorYReintentarCarga() {
        servicio.sinConexion = true
        val vm = vmDocente()
        assertEquals(EstadoUi.Error, vm.estado)

        servicio.sinConexion = false
        vm.cargar()
        esperar()
        assertEquals(laura, vm.formulario.datos)
    }

    @Test
    fun volverACargarNoBorraLoQueSeEstaEscribiendo() {
        val vm = vmDocente()
        vm.formulario.nombre = "Laura Gómez"

        vm.cargar()
        esperar()

        assertEquals("Laura Gómez", vm.formulario.nombre)
    }
}
