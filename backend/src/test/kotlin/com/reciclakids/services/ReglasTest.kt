package com.reciclakids.services

import com.reciclakids.models.CategoriaResiduo
import com.reciclakids.models.dto.TipoEstadoNino
import com.reciclakids.plugins.DatosInvalidos
import com.reciclakids.repositories.IntentoGuardado
import com.reciclakids.repositories.PartidaGuardada
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReglasTest {

    @Test
    fun delNinoSoloSeGuardaLaInicialDelApellido() {
        assertEquals("Salomé M.", Validacion.nombreNino(" salomé ", "m"))
        assertFailsWith<DatosInvalidos> { Validacion.nombreNino("Salomé", "Martínez") }
        assertFailsWith<DatosInvalidos> { Validacion.nombreNino("Salomé", "4") }
        assertFailsWith<DatosInvalidos> { Validacion.nombreNino(" ", "M") }
    }

    @Test
    fun validaCorreoYContrasena() {
        assertEquals("laura@jardin.co", Validacion.correo(" Laura@Jardin.CO "))
        assertFailsWith<DatosInvalidos> { Validacion.correo("laura@") }
        assertFailsWith<DatosInvalidos> { Validacion.contrasena("1234567") }
        assertEquals("12345678", Validacion.contrasena("12345678"))
    }

    @Test
    fun formatoDeLosCodigos() {
        val codigos = GeneradorCodigos()
        repeat(200) {
            assertTrue(Regex("^\\d{4}$").matches(codigos.codigoReto()))
            val vinculacion = codigos.codigoVinculacion()
            assertTrue(Regex("^[A-HJ-NP-Z]{2}-[A-HJ-NP-Z2-9]{4}$").matches(vinculacion), vinculacion)
        }
    }

    @Test
    fun nombrePorDefectoDelReto() {
        assertEquals("Reto de orgánicos", DocenteServicio.nombrePorDefecto(setOf(CategoriaResiduo.Organico)))
        assertEquals(
            "Reto de aprovechables, no aprovechables y orgánicos",
            DocenteServicio.nombrePorDefecto(CategoriaResiduo.entries.toSet()),
        )
    }

    @Test
    fun elReporteEsDeLaSemanaEscolarAnterior() {
        val martes = LocalDate.of(2026, 10, 13)
        assertEquals(LocalDate.of(2026, 10, 12), DocenteServicio.inicioSemana(martes))
        assertEquals(LocalDate.of(2026, 10, 5), DocenteServicio.ultimaSemanaCompleta(martes))
    }

    @Test
    fun estadoDelNinoConElRetoDeHoy() {
        val base = PartidaGuardada("p", "n", "r", LocalDate.of(2026, 10, 13), false, 2, 1, 2, 60_000)
        assertEquals(TipoEstadoNino.SinEmpezar, DocenteServicio.estadoHoy(emptyList(), entro = false, totalResiduos = 6).tipo)
        assertEquals(1, DocenteServicio.estadoHoy(emptyList(), entro = true, totalResiduos = 6).residuoActual)
        val enCurso = DocenteServicio.estadoHoy(listOf(base), entro = true, totalResiduos = 6)
        assertEquals(TipoEstadoNino.EnCurso, enCurso.tipo)
        assertEquals(3, enCurso.residuoActual)
        val completo = DocenteServicio.estadoHoy(listOf(base.copy(terminada = true, aciertos = 6, tiempoMs = 150_000)), entro = true, totalResiduos = 6)
        assertEquals(TipoEstadoNino.Completo, completo.tipo)
        assertEquals(3, completo.minutos)
        assertEquals(5, completo.aciertos)
        assertEquals(6, completo.total)
    }

    @Test
    fun porcentajesYConfusion() {
        fun intento(residuo: String, elegida: CategoriaResiduo, correcto: Boolean) =
            IntentoGuardado("n", LocalDate.of(2026, 10, 13), 0, residuo, elegida, correcto)
        val intentos = listOf(
            intento("botella", CategoriaResiduo.Aprovechable, true),
            intento("empaque", CategoriaResiduo.Aprovechable, false),
            intento("empaque", CategoriaResiduo.Aprovechable, false),
            intento("empaque", CategoriaResiduo.NoAprovechable, true),
        )
        assertEquals(50, DocenteServicio.porcentaje(intentos))
        assertNull(DocenteServicio.porcentaje(emptyList()))
        assertEquals(mapOf(CategoriaResiduo.Aprovechable to 100, CategoriaResiduo.NoAprovechable to 33), DocenteServicio.porCategoria(intentos))
        val confusion = DocenteServicio.confusionFrecuente(intentos)!!
        assertEquals("empaque metalizado", confusion.residuo)
        assertEquals(CategoriaResiduo.NoAprovechable, confusion.correcta)
        assertEquals(CategoriaResiduo.Aprovechable, confusion.elegida)
    }

    @Test
    fun elCorreoDelReporteDestacaLaMayorMejora() {
        fun intento(residuo: String, correcto: Boolean) =
            IntentoGuardado("n", LocalDate.of(2026, 10, 13), 0, residuo, CategoriaResiduo.Organico, correcto)
        val previos = listOf(intento("cascara", true), intento("cascara", false))
        val ahora = listOf(intento("cascara", true), intento("restos", true))
        val mensaje = NotificacionesServicio.mejora(ahora, previos)!!
        assertEquals("Esta semana mejoró separando orgánicos: de 50 % a 100 % de aciertos.", mensaje.textoCompleto)
        assertNull(NotificacionesServicio.mejora(previos, previos))
    }
}
