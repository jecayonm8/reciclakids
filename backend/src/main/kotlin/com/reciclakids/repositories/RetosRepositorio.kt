package com.reciclakids.repositories

import com.reciclakids.models.CategoriaResiduo
import com.reciclakids.models.Dificultad
import com.reciclakids.models.EstadoReto
import com.reciclakids.models.Ingresos
import com.reciclakids.models.RetoResiduos
import com.reciclakids.models.Retos
import com.reciclakids.models.aColumna
import com.reciclakids.models.categoriasDeColumna
import com.reciclakids.models.dificultadDeColumna
import com.reciclakids.models.estadoDeColumna
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.between
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.time.LocalDate
import java.time.OffsetDateTime

/**
 * Un reto guardado. En la base un reto con fecha futura queda PUBLICADO con su código ya
 * reservado: se muestra como «Programado» hasta que llega su día (ver [estadoVisible]).
 */
data class RetoGuardado(
    val id: String,
    val grupoId: String,
    val nombre: String,
    val categorias: Set<CategoriaResiduo>,
    val dificultad: Dificultad,
    val fecha: LocalDate?,
    val estado: EstadoReto,
    val codigo: String?,
    val reutilizadoDe: String?,
    val creadoEn: OffsetDateTime,
) {
    fun estadoVisible(hoy: LocalDate): EstadoReto =
        if (estado == EstadoReto.Publicado && fecha != null && fecha.isAfter(hoy)) EstadoReto.Programado else estado

    /** Solo el día del reto su código deja entrar a los niños (RNF-05). */
    fun codigoActivo(hoy: LocalDate): String? = codigo?.takeIf { estado == EstadoReto.Publicado && fecha == hoy }
}

/** Retos, sus residuos e ingresos de niños. Se llama dentro de una transacción. */
class RetosRepositorio {

    fun porId(id: String): RetoGuardado? = Retos.selectAll().where { Retos.id eq id }.singleOrNull()?.aReto()

    fun delGrupo(grupoId: String): List<RetoGuardado> =
        Retos.selectAll().where { Retos.grupoId eq grupoId }
            .orderBy(Retos.fecha to SortOrder.DESC_NULLS_FIRST, Retos.creadoEn to SortOrder.DESC)
            .map { it.aReto() }

    fun publicadoPorCodigo(codigo: String, fecha: LocalDate): RetoGuardado? =
        Retos.selectAll().where {
            (Retos.codigo eq codigo) and (Retos.fecha eq fecha) and (Retos.estado eq EstadoReto.Publicado.aColumna())
        }.singleOrNull()?.aReto()

    /** Para distinguir un código vencido (fue de un día que ya pasó) de uno que no existe o aún no empieza. */
    fun codigoVencido(codigo: String, hoy: LocalDate): Boolean =
        Retos.selectAll().where { (Retos.codigo eq codigo) and (Retos.fecha less hoy) }.any()

    fun codigoUsado(codigo: String, fecha: LocalDate): Boolean =
        Retos.selectAll().where { (Retos.codigo eq codigo) and (Retos.fecha eq fecha) }.any()

    /** El reto de hoy del grupo: el último publicado para hoy. */
    fun deHoy(grupoId: String, hoy: LocalDate): RetoGuardado? =
        Retos.selectAll().where {
            (Retos.grupoId eq grupoId) and (Retos.fecha eq hoy) and (Retos.estado eq EstadoReto.Publicado.aColumna())
        }.orderBy(Retos.creadoEn to SortOrder.DESC).firstOrNull()?.aReto()

    fun publicadosEntre(grupoId: String, desde: LocalDate, hasta: LocalDate): List<RetoGuardado> =
        Retos.selectAll().where {
            (Retos.grupoId eq grupoId) and (Retos.fecha.between(desde, hasta)) and (Retos.estado eq EstadoReto.Publicado.aColumna())
        }.orderBy(Retos.fecha to SortOrder.ASC).map { it.aReto() }

    fun guardar(reto: RetoGuardado, residuos: List<String>) {
        val existe = Retos.selectAll().where { Retos.id eq reto.id }.any()
        if (existe) {
            Retos.update({ Retos.id eq reto.id }) { it.llenar(reto) }
            RetoResiduos.deleteWhere { RetoResiduos.retoId eq reto.id }
        } else {
            Retos.insert {
                it[id] = reto.id
                it[grupoId] = reto.grupoId
                it[creadoEn] = reto.creadoEn
                it.llenar(reto)
            }
        }
        RetoResiduos.batchInsert(residuos.withIndex()) { (orden, residuo) ->
            this[RetoResiduos.retoId] = reto.id
            this[RetoResiduos.orden] = orden.toShort()
            this[RetoResiduos.residuoId] = residuo
        }
    }

    fun residuos(retoId: String): List<String> =
        RetoResiduos.selectAll().where { RetoResiduos.retoId eq retoId }
            .orderBy(RetoResiduos.orden to SortOrder.ASC)
            .map { it[RetoResiduos.residuoId] }

    fun registrarIngreso(ninoId: String, retoId: String, ahora: OffsetDateTime) {
        Ingresos.insertIgnore {
            it[Ingresos.ninoId] = ninoId
            it[Ingresos.retoId] = retoId
            it[entroEn] = ahora
        }
    }

    fun ingresos(retoId: String): Set<String> =
        Ingresos.selectAll().where { Ingresos.retoId eq retoId }.mapTo(mutableSetOf()) { it[Ingresos.ninoId] }

    fun borrarIngresosDeNinos(ninoIds: Collection<String>) {
        for (id in ninoIds) Ingresos.deleteWhere { Ingresos.ninoId eq id }
    }

    private fun org.jetbrains.exposed.v1.core.statements.UpdateBuilder<*>.llenar(reto: RetoGuardado) {
        this[Retos.nombre] = reto.nombre
        this[Retos.categorias] = reto.categorias.aColumna()
        this[Retos.dificultad] = reto.dificultad.aColumna()
        this[Retos.fecha] = reto.fecha
        this[Retos.estado] = reto.estado.aColumna()
        this[Retos.codigo] = reto.codigo
        this[Retos.reutilizadoDe] = reto.reutilizadoDe
    }

    private fun ResultRow.aReto() = RetoGuardado(
        id = this[Retos.id],
        grupoId = this[Retos.grupoId],
        nombre = this[Retos.nombre],
        categorias = categoriasDeColumna(this[Retos.categorias]),
        dificultad = dificultadDeColumna(this[Retos.dificultad]),
        fecha = this[Retos.fecha],
        estado = estadoDeColumna(this[Retos.estado]),
        codigo = this[Retos.codigo],
        reutilizadoDe = this[Retos.reutilizadoDe],
        creadoEn = this[Retos.creadoEn],
    )
}
