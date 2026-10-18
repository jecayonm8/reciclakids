package com.reciclakids.network.api

import androidx.room.Room
import com.reciclakids.local.BaseDatosReciclaKids
import com.reciclakids.local.LimitesJuegoLocales
import com.reciclakids.local.RepositorioJuego
import com.reciclakids.model.CategoriaResiduo
import com.reciclakids.model.ControlParental
import com.reciclakids.model.Dificultad
import com.reciclakids.model.EstadoNinoHoy
import com.reciclakids.model.RetoBorrador
import com.reciclakids.model.RolAdulto
import com.reciclakids.network.RegistroAcudiente
import com.reciclakids.network.RegistroDocente
import com.reciclakids.network.Respuesta
import com.reciclakids.network.ResultadoCodigo
import com.reciclakids.network.ResultadoPublicacion
import com.reciclakids.network.ResultadoRegistro
import com.reciclakids.network.ResultadoSesion
import com.reciclakids.network.SincronizadorResultados
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.time.DayOfWeek

/**
 * El recorrido completo docente → niño → acudiente con las clases de la app contra un backend de
 * verdad. No corre en la CI: hay que levantar el backend (`docker compose up --build` en
 * backend/) y definir la URL de su API, por ejemplo
 * `RECICLAKIDS_API_RECORRIDO=http://localhost:8080/api/v1/ ./gradlew testDebugUnitTest --tests "*RecorridoContraBackendTest"`.
 */
@RunWith(RobolectricTestRunner::class)
class RecorridoContraBackendTest {

    private val url = System.getenv("RECICLAKIDS_API_RECORRIDO").orEmpty()

    @Before
    fun soloConBackend() {
        assumeTrue("Sin RECICLAKIDS_API_RECORRIDO no hay backend contra el cual probar", url.isNotBlank())
    }

    /** Cada actor en su propio teléfono: su almacén cifrado y su sesión. */
    private inner class Telefono {
        val almacen = AlmacenEnMemoria()
        val sesion = SesionAdultoGuardada(almacen)
        val cliente = ClienteApi(url, OkHttp.create(), sesion)
        val trabajos = Job()
        val acceso = ServicioAccesoApi(cliente, sesion, CoroutineScope(Dispatchers.IO + trabajos))
    }

    @Test
    fun docenteNinoYAcudienteDePuntaAPunta() = runBlocking {
        val unico = System.currentTimeMillis()
        val correoDocente = "docente.$unico@jardin.edu.co"
        val correoAcudiente = "acudiente.$unico@correo.com"

        // --- La docente se registra, entra, agrega a Salomé y publica el reto de hoy ---
        val docente = Telefono()
        assertEquals(ResultadoRegistro.Creada, docente.acceso.registrarDocente(RegistroDocente("Laura Gómez", correoDocente, "Arcoíris", "Jardín B", "clave-de-prueba")))
        assertEquals(ResultadoRegistro.CorreoEnUso, docente.acceso.registrarDocente(RegistroDocente("Otra", correoDocente, "Arcoíris", "Jardín B", "clave-de-prueba")))
        val sesionDocente = docente.acceso.iniciarSesion(correoDocente, "clave-de-prueba")
        assertEquals(RolAdulto.Docente, (sesionDocente as ResultadoSesion.Iniciada).cuenta.rol)
        val servicioDocente = ServicioDocenteApi(docente.cliente, docente.sesion)
        val hoy = (servicioDocente.tablero() as Respuesta.Ok).dato.hoy
        val salome = (servicioDocente.agregarNino("Salomé", "M") as Respuesta.Ok).dato
        assertEquals("Salomé M.", salome.nombre)
        val publicado = servicioDocente.publicar(RetoBorrador(CategoriaResiduo.entries.toSet(), Dificultad.Facil, hoy))
        val reto = (publicado as ResultadoPublicacion.Publicado).reto
        val codigo = checkNotNull(reto.codigoActivo(hoy))

        // --- Salomé entra con el código, elige su nombre y juega en el teléfono del salón ---
        val salon = Telefono()
        val baseDatos = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), BaseDatosReciclaKids::class.java)
            .allowMainThreadQueries().build()
        val limites = LimitesJuegoLocales(baseDatos.juegoDao())
        val sesionesNino = SesionesNino(salon.cliente, TokensNino(salon.almacen), limites::guardar)
        val retos = ServicioRetosApi(salon.cliente, sesionesNino)
        assertEquals(ResultadoCodigo.Invalido, retos.buscarReto(if (codigo == "0000") "0001" else "0000", hoy))
        val retoNino = (retos.buscarReto(codigo, hoy) as ResultadoCodigo.Valido).reto
        assertEquals(reto.id, retoNino.id)
        assertEquals(Dificultad.Facil, retoNino.dificultad)
        assertEquals(4, retoNino.residuos.size)
        val nino = retos.ninosDelGrupo(retoNino).single { it.id == salome.id }
        retos.abrirSesion(retoNino, nino)
        assertNotNull(sesionesNino.token(nino.id))
        assertEquals(ControlParental().limiteMinutosDiarios, limites.observar(nino.id).first().limiteMinutosDiarios)
        assertEquals(1, withTimeout(10_000) { servicioDocente.ninosQueEntraron(reto.id).first() })

        val juego = RepositorioJuego(baseDatos)
        val partida = juego.abrirPartida(nino.id, retoNino)
        var estado = partida.estado
        var primero = true
        while (!estado.terminada) {
            val correcta = checkNotNull(estado.residuoActual).categoria
            // Un error al principio: el backend lo vuelve a evaluar y corta la racha.
            val elegida = if (primero) CategoriaResiduo.entries.first { it != correcta } else correcta
            primero = false
            val intento = estado.intentar(elegida)
            juego.registrarIntento(partida.id, intento, elegida, tiempoMs = 5_000)
            estado = intento.estado
        }
        juego.terminarPartida(partida.id, nino.id, retoNino, estado, tiempoMs = 40_000)
        val sincronizador = SincronizadorResultados(baseDatos, ServicioResultadosApi(salon.cliente, sesionesNino)) { hoy }
        assertTrue(sincronizador.sincronizar())
        assertEquals(0, juego.intentosPendientes().first())
        // Reenviar lo mismo no duplica nada (idempotente por idLocal).
        baseDatos.openHelper.writableDatabase.execSQL("UPDATE intento SET sincronizado = 0")
        baseDatos.openHelper.writableDatabase.execSQL("UPDATE partida SET terminadaEnviada = 0")
        assertTrue(sincronizador.sincronizar())

        // --- La docente ve que Salomé terminó ---
        val grupo = (servicioDocente.grupoHoy() as Respuesta.Ok).dato
        val estadoSalome = grupo.ninos.single { it.id == salome.id }.estado
        assertEquals(EstadoNinoHoy.Completo(minutos = 1, aciertos = 3, total = 4), estadoSalome)
        assertEquals(1, (servicioDocente.tablero() as Respuesta.Ok).dato.completaron)

        // --- La mamá se registra con el código de Salomé y ve su progreso ---
        val casa = Telefono()
        val conCodigoMalo = RegistroAcudiente("Marta Ruiz", correoAcudiente, "clave-de-prueba", "ZZ-ZZZZ")
        assertEquals(ResultadoRegistro.CodigoInvalido, casa.acceso.registrarAcudiente(conCodigoMalo))
        assertEquals(ResultadoRegistro.Creada, casa.acceso.registrarAcudiente(conCodigoMalo.copy(codigoVinculacion = salome.codigoVinculacion)))
        val sesionAcudiente = casa.acceso.iniciarSesion(correoAcudiente, "clave-de-prueba")
        assertEquals(RolAdulto.Acudiente, (sesionAcudiente as ResultadoSesion.Iniciada).cuenta.rol)
        val padres = ServicioPadresApi(casa.cliente, casa.sesion)
        assertEquals(listOf(salome.id), (padres.hijos() as Respuesta.Ok).dato.map { it.id })
        val inicio = (padres.inicio(salome.id) as Respuesta.Ok).dato
        assertFalse(inicio.sinDatos)
        // La semana escolar va de lunes a viernes: lo jugado un fin de semana no sale en ella.
        if (hoy.dayOfWeek < DayOfWeek.SATURDAY) {
            val dia = inicio.semana.dias.single()
            assertTrue(dia.completo)
            assertEquals(1, dia.errores)
        }
        assertEquals(1, (padres.logros(salome.id) as Respuesta.Ok).dato.retosCompletados)
        // 5 intentos (un error y cuatro aciertos) aunque se enviaron dos veces: no se duplican.
        assertEquals(5, (padres.privacidad(salome.id) as Respuesta.Ok).dato.registros)
        assertTrue(checkNotNull((servicioDocente.detalleNino(salome.id) as Respuesta.Ok).dato).acudienteVinculado)

        // El control parental llega al niño con su próxima sincronización.
        val control = (padres.controlParental(salome.id) as Respuesta.Ok).dato
        assertTrue(padres.guardarControlParental(salome.id, control.copy(limiteMinutosDiarios = 15, recordatorioSinJugar = true)))
        assertTrue((padres.controlParental(salome.id) as Respuesta.Ok).dato.recordatorioSinJugar)
        sesionesNino.actualizarLimite(nino.id, checkNotNull(sesionesNino.token(nino.id)))
        assertEquals(15, limites.observar(nino.id).first().limiteMinutosDiarios)

        // --- La sesión se refresca sola cuando vence el token de acceso, y cerrarla la invalida ---
        val guardada = checkNotNull(casa.sesion.leer())
        casa.sesion.guardar(SesionDto("token-vencido", 0, guardada.tokenRefresco, guardada.cuenta))
        assertTrue(padres.perfil() is Respuesta.Ok)
        val rotada = checkNotNull(casa.sesion.leer())
        assertNotEquals(guardada.tokenRefresco, rotada.tokenRefresco)
        assertEquals(RolAdulto.Acudiente, casa.acceso.sesionGuardada()?.rol)

        casa.acceso.cerrarSesion()
        casa.trabajos.children.toList().joinAll()
        // Con el token de refresco de la sesión cerrada ya no se entra.
        val otroTelefono = Telefono()
        otroTelefono.sesion.guardar(SesionDto("token-vencido", 0, rotada.tokenRefresco, rotada.cuenta))
        assertEquals(RespuestaApi.SesionVencida, otroTelefono.cliente.get<CuentaDto>("cuenta"))
        baseDatos.close()
    }
}
