package com.reciclakids.repositories

import com.reciclakids.models.CategoriaResiduo
import com.reciclakids.models.CorreosEnviados
import com.reciclakids.models.Insignia
import com.reciclakids.models.InsigniasGanadas
import com.reciclakids.models.Intentos
import com.reciclakids.models.Partidas
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.between
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.time.LocalDate
import java.time.OffsetDateTime

data class PartidaGuardada(
    val id: String,
    val ninoId: String,
    val retoId: String,
    val fecha: LocalDate,
    val terminada: Boolean,
    val aciertos: Int,
    val errores: Int,
    val rachaMaxima: Int,
    val tiempoMs: Long,
)

data class IntentoGuardado(
    val ninoId: String,
    val fecha: LocalDate,
    val momentoMs: Long,
    val residuoId: String,
    val categoriaElegida: CategoriaResiduo,
    val correcto: Boolean,
)

data class NuevoIntento(val momentoMs: Long, val residuoId: String, val categoriaElegida: CategoriaResiduo, val correcto: Boolean)

data class CorreoGuardado(
    val tipo: String,
    val destinatario: String,
    val insignia: Insignia?,
    val retosCompletados: Int,
    val retosPublicados: Int,
    val aciertos: Int?,
    val enviadoEn: OffsetDateTime,
)

/** Partidas, intentos, insignias y correos enviados. Se llama dentro de una transacción. */
class ResultadosRepositorio {

    fun partidaPorIdLocal(ninoId: String, idLocal: String): PartidaGuardada? =
        Partidas.selectAll().where { (Partidas.ninoId eq ninoId) and (Partidas.idLocal eq idLocal) }.singleOrNull()?.aPartida()

    fun crearPartida(partida: PartidaGuardada, idLocal: String, ahora: OffsetDateTime) {
        Partidas.insert {
            it[id] = partida.id
            it[ninoId] = partida.ninoId
            it[retoId] = partida.retoId
            it[Partidas.idLocal] = idLocal
            it[fecha] = partida.fecha
            it[terminada] = partida.terminada
            it[aciertos] = partida.aciertos.toShort()
            it[errores] = partida.errores.toShort()
            it[rachaMaxima] = partida.rachaMaxima.toShort()
            it[tiempoMs] = partida.tiempoMs
            it[recibidaEn] = ahora
        }
    }

    fun actualizarPartida(partida: PartidaGuardada, ahora: OffsetDateTime) {
        Partidas.update({ Partidas.id eq partida.id }) {
            it[terminada] = partida.terminada
            it[aciertos] = partida.aciertos.toShort()
            it[errores] = partida.errores.toShort()
            it[rachaMaxima] = partida.rachaMaxima.toShort()
            it[tiempoMs] = partida.tiempoMs
            it[recibidaEn] = ahora
        }
    }

    /** Claves (momento, residuo) que ya tiene la partida: lo que llegue repetido no se vuelve a contar. */
    fun clavesIntentos(partidaId: String): Set<Pair<Long, String>> =
        Intentos.selectAll().where { Intentos.partidaId eq partidaId }
            .mapTo(mutableSetOf()) { it[Intentos.momentoMs] to it[Intentos.residuoId] }

    fun agregarIntentos(partidaId: String, intentos: List<NuevoIntento>) {
        Intentos.batchInsert(intentos, ignore = true) { intento ->
            this[Intentos.partidaId] = partidaId
            this[Intentos.momentoMs] = intento.momentoMs
            this[Intentos.residuoId] = intento.residuoId
            this[Intentos.categoriaElegida] = intento.categoriaElegida.name
            this[Intentos.correcto] = intento.correcto
        }
    }

    fun partidasDe(ninoIds: Collection<String>, desde: LocalDate, hasta: LocalDate): List<PartidaGuardada> {
        if (ninoIds.isEmpty()) return emptyList()
        return Partidas.selectAll().where { (Partidas.ninoId inList ninoIds) and (Partidas.fecha.between(desde, hasta)) }
            .orderBy(Partidas.fecha to SortOrder.ASC)
            .map { it.aPartida() }
    }

    fun partidasDeReto(retoId: String): List<PartidaGuardada> =
        Partidas.selectAll().where { Partidas.retoId eq retoId }.map { it.aPartida() }

    fun intentosDe(ninoIds: Collection<String>, desde: LocalDate, hasta: LocalDate): List<IntentoGuardado> {
        if (ninoIds.isEmpty()) return emptyList()
        return Intentos.join(Partidas, JoinType.INNER, Intentos.partidaId, Partidas.id)
            .selectAll().where { (Partidas.ninoId inList ninoIds) and (Partidas.fecha.between(desde, hasta)) }
            .map {
                IntentoGuardado(
                    ninoId = it[Partidas.ninoId],
                    fecha = it[Partidas.fecha],
                    momentoMs = it[Intentos.momentoMs],
                    residuoId = it[Intentos.residuoId],
                    categoriaElegida = CategoriaResiduo.valueOf(it[Intentos.categoriaElegida]),
                    correcto = it[Intentos.correcto],
                )
            }
    }

    fun intentosDeReto(retoId: String): List<Boolean> =
        Intentos.join(Partidas, JoinType.INNER, Intentos.partidaId, Partidas.id)
            .selectAll().where { Partidas.retoId eq retoId }
            .map { it[Intentos.correcto] }

    fun contarIntentos(ninoId: String): Int =
        Intentos.join(Partidas, JoinType.INNER, Intentos.partidaId, Partidas.id)
            .selectAll().where { Partidas.ninoId eq ninoId }.count().toInt()

    /** Retos distintos que el niño terminó al menos una vez. */
    fun retosCompletados(ninoId: String): Int =
        Partidas.selectAll().where { (Partidas.ninoId eq ninoId) and (Partidas.terminada eq true) }
            .map { it[Partidas.retoId] }.toSet().size

    fun diasConReto(ninoId: String): Int =
        Partidas.selectAll().where { (Partidas.ninoId eq ninoId) and (Partidas.terminada eq true) }
            .map { it[Partidas.fecha] }.toSet().size

    fun insignias(ninoId: String): Map<Insignia, LocalDate> =
        InsigniasGanadas.selectAll().where { InsigniasGanadas.ninoId eq ninoId }
            .associate { Insignia.valueOf(it[InsigniasGanadas.insignia]) to it[InsigniasGanadas.ganadaEl] }

    fun insigniasDe(ninoIds: Collection<String>, desde: LocalDate, hasta: LocalDate): Int {
        if (ninoIds.isEmpty()) return 0
        return InsigniasGanadas.selectAll()
            .where { (InsigniasGanadas.ninoId inList ninoIds) and (InsigniasGanadas.ganadaEl.between(desde, hasta)) }
            .count().toInt()
    }

    fun otorgar(ninoId: String, insignia: Insignia, fecha: LocalDate) {
        InsigniasGanadas.insertIgnore {
            it[InsigniasGanadas.ninoId] = ninoId
            it[InsigniasGanadas.insignia] = insignia.name
            it[ganadaEl] = fecha
        }
    }

    fun registrarCorreo(
        id: String,
        acudienteId: String,
        ninoId: String,
        tipo: String,
        destinatario: String,
        insignia: Insignia?,
        retosCompletados: Int,
        retosPublicados: Int,
        aciertos: Int?,
        ahora: OffsetDateTime,
    ) {
        CorreosEnviados.insert {
            it[CorreosEnviados.id] = id
            it[CorreosEnviados.acudienteId] = acudienteId
            it[CorreosEnviados.ninoId] = ninoId
            it[CorreosEnviados.tipo] = tipo
            it[CorreosEnviados.destinatario] = destinatario
            it[CorreosEnviados.insignia] = insignia?.name
            it[CorreosEnviados.retosCompletados] = retosCompletados.toShort()
            it[CorreosEnviados.retosPublicados] = retosPublicados.toShort()
            it[CorreosEnviados.aciertos] = aciertos?.toShort()
            it[enviadoEn] = ahora
        }
    }

    fun correos(acudienteId: String, ninoId: String): List<CorreoGuardado> =
        CorreosEnviados.selectAll()
            .where { (CorreosEnviados.acudienteId eq acudienteId) and (CorreosEnviados.ninoId eq ninoId) }
            .orderBy(CorreosEnviados.enviadoEn to SortOrder.DESC)
            .limit(50)
            .map {
                CorreoGuardado(
                    tipo = it[CorreosEnviados.tipo],
                    destinatario = it[CorreosEnviados.destinatario],
                    insignia = it[CorreosEnviados.insignia]?.let(Insignia::valueOf),
                    retosCompletados = it[CorreosEnviados.retosCompletados].toInt(),
                    retosPublicados = it[CorreosEnviados.retosPublicados].toInt(),
                    aciertos = it[CorreosEnviados.aciertos]?.toInt(),
                    enviadoEn = it[CorreosEnviados.enviadoEn],
                )
            }

    /** Correos de logro enviados a ese acudiente sobre ese niño desde [desde]: máximo uno al día (UI-33). */
    fun correosLogroDesde(acudienteId: String, ninoId: String, desde: OffsetDateTime): Int =
        CorreosEnviados.selectAll().where {
            (CorreosEnviados.acudienteId eq acudienteId) and (CorreosEnviados.ninoId eq ninoId) and
                (CorreosEnviados.tipo eq "LOGRO") and (CorreosEnviados.enviadoEn greaterEq desde)
        }.count().toInt()

    /** Borra resultados, insignias y correos de [ninoIds] (botón «Eliminar datos del grupo»). */
    fun borrarDe(ninoIds: Collection<String>) {
        for (id in ninoIds) {
            Partidas.deleteWhere { Partidas.ninoId eq id }
            InsigniasGanadas.deleteWhere { InsigniasGanadas.ninoId eq id }
            CorreosEnviados.deleteWhere { CorreosEnviados.ninoId eq id }
        }
    }

    private fun ResultRow.aPartida() = PartidaGuardada(
        id = this[Partidas.id],
        ninoId = this[Partidas.ninoId],
        retoId = this[Partidas.retoId],
        fecha = this[Partidas.fecha],
        terminada = this[Partidas.terminada],
        aciertos = this[Partidas.aciertos].toInt(),
        errores = this[Partidas.errores].toInt(),
        rachaMaxima = this[Partidas.rachaMaxima].toInt(),
        tiempoMs = this[Partidas.tiempoMs],
    )
}
