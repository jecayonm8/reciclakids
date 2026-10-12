package com.reciclakids.model

import com.reciclakids.model.CategoriaResiduo.Aprovechable
import com.reciclakids.model.CategoriaResiduo.NoAprovechable
import com.reciclakids.model.CategoriaResiduo.Organico
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class PadresTest {

    private val lunes = LocalDate.of(2026, 9, 14)

    /** Una semana de un solo día con estos aciertos sobre 10 residuos por caneca. */
    private fun semana(inicio: LocalDate = lunes, vararg aciertos: Pair<CategoriaResiduo, Int>) = SemanaHijo(
        inicio = inicio,
        retosPublicados = 5,
        dias = if (aciertos.isEmpty()) {
            emptyList()
        } else {
            listOf(DiaDeJuego(inicio, 15, true, aciertos.associate { it.first to Conteo(it.second, 10) }, rachaMaxima = 3))
        },
        insignias = emptyList(),
    )

    @Test
    fun elMensajeDestacaLaCanecaQueMasMejoro() {
        val antes = semana(lunes.minusWeeks(1), Aprovechable to 9, Organico to 7, NoAprovechable to 5)
        val ahora = semana(lunes, Aprovechable to 9, Organico to 9, NoAprovechable to 6)

        assertEquals(MensajeSemana.Mejoro(Organico, antes = 70, ahora = 90), mensajeSemana(ahora, antes))
    }

    @Test
    fun sinMejoraClaraSugierePracticarLaCanecaMasDificil() {
        val antes = semana(lunes.minusWeeks(1), Aprovechable to 9, NoAprovechable to 5)
        val ahora = semana(lunes, Aprovechable to 9, NoAprovechable to 5)

        assertEquals(MensajeSemana.Practicar(NoAprovechable, 50), mensajeSemana(ahora, antes))
        // Sin semana anterior no hay con qué comparar.
        assertEquals(MensajeSemana.Practicar(NoAprovechable, 50), mensajeSemana(ahora, null))
    }

    @Test
    fun siTodoVaBienLoDiceSinNumeros() {
        val ahora = semana(lunes, Aprovechable to 9, Organico to 8, NoAprovechable to 10)

        assertEquals(MensajeSemana.VaMuyBien, mensajeSemana(ahora, null))
    }

    @Test
    fun unaSemanaSinJuegoNoTieneMensajeNiAciertos() {
        val vacia = semana(lunes)

        assertNull(mensajeSemana(vacia, null))
        assertNull(vacia.aciertos)
        assertEquals(0, vacia.rachaMaxima)
        assertEquals(listOf(0, 0, 0), vacia.minutosPorDia(hasta = lunes.plusDays(2)).map { it.second })
    }

    @Test
    fun losConteosSeSumanPorCaneca() {
        assertEquals(Conteo(7, 10), Conteo(3, 4) + Conteo(4, 6))
        assertEquals(70, Conteo(7, 10).porcentaje)
        assertNull(Conteo(0, 0).porcentaje)
        assertEquals(LocalDate.of(2026, 9, 14), inicioSemana(LocalDate.of(2026, 9, 20)))
    }

    @Test
    fun losMinutosSeQuedanEntreCincoYCuarentaYCinco() {
        assertEquals(5, ControlParental().conMinutos(0).limiteMinutosDiarios)
        assertEquals(45, ControlParental().conMinutos(60).limiteMinutosDiarios)
        assertEquals(25, ControlParental().conMinutos(25).limiteMinutosDiarios)
        assertEquals(25 * 60_000L, ControlParental(limiteMinutosDiarios = 25).limiteMs)
    }
}
