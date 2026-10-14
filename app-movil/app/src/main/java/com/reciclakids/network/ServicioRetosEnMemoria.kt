package com.reciclakids.network

import com.reciclakids.model.CatalogoResiduos
import com.reciclakids.model.CategoriaResiduo
import com.reciclakids.model.Dificultad
import com.reciclakids.model.Nino
import com.reciclakids.model.Reto
import com.reciclakids.model.residuosPorReto
import kotlinx.coroutines.delay
import java.time.LocalDate

/**
 * Implementación PROVISIONAL mientras no exista el backend: lo que publica el Modo Docente vive
 * en su propio servicio en memoria y no llega aquí, así que se dejan unos códigos de
 * demostración para recorrer el Modo Niño. Se reemplaza por el cliente del backend.
 *
 * - 4729: reto de hoy, dificultad media, las tres canecas (el código de los prototipos).
 * - 1111: reto de hoy, fácil, aprovechables y orgánicos. 9999: reto de hoy, difícil, las tres canecas.
 * - 1234: reto de ayer, ya vencido.
 */
class ServicioRetosEnMemoria(private val latenciaMs: Long = 300) : ServicioRetos {

    private class RetoDeDemostracion(
        val diasAtras: Long,
        val dificultad: Dificultad,
        val categorias: Set<CategoriaResiduo> = CategoriaResiduo.entries.toSet(),
    )

    private val retos = mapOf(
        "4729" to RetoDeDemostracion(0, Dificultad.Medio),
        "1111" to RetoDeDemostracion(0, Dificultad.Facil, setOf(CategoriaResiduo.Aprovechable, CategoriaResiduo.Organico)),
        "9999" to RetoDeDemostracion(0, Dificultad.Dificil),
        "1234" to RetoDeDemostracion(1, Dificultad.Medio),
    )

    private val grupo = listOf(
        "Salomé M.", "Juan T.", "Ana L.", "Emilio R.", "Sara P.",
        "Tomás G.", "Valeria C.", "Mateo S.", "Luciana B.", "Simón H.",
    ).mapIndexed { i, nombre -> Nino(id = "nino-${i + 1}", nombre = nombre) }

    override suspend fun buscarReto(codigo: String, hoy: LocalDate): ResultadoCodigo {
        delay(latenciaMs)
        val demo = retos[codigo] ?: return ResultadoCodigo.Invalido
        val fecha = hoy.minusDays(demo.diasAtras)
        if (fecha != hoy) return ResultadoCodigo.Vencido
        return ResultadoCodigo.Valido(Reto(codigo, fecha, demo.dificultad, residuosPara(demo), demo.categorias))
    }

    override suspend fun ninosDelGrupo(reto: Reto): List<Nino> {
        delay(latenciaMs)
        return grupo
    }

    /** Residuos de las canecas del reto: 4 en fácil, 6 en medio y 8 en difícil. */
    private fun residuosPara(demo: RetoDeDemostracion) = CatalogoResiduos.todos
        .filter { it.categoria in demo.categorias }
        .take(demo.dificultad.residuosPorReto)
}
