package com.reciclakids.repositories

import com.reciclakids.models.ControlesParentales
import com.reciclakids.models.Dificultad
import com.reciclakids.models.Grupos
import com.reciclakids.models.Ninos
import com.reciclakids.models.SolicitudesEliminacion
import com.reciclakids.models.Usuarios
import com.reciclakids.models.Vinculaciones
import com.reciclakids.models.aColumna
import com.reciclakids.models.dificultadDeColumna
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.time.LocalDate
import java.time.OffsetDateTime

data class NinoGuardado(
    val id: String,
    val grupoId: String,
    val nombre: String,
    val edad: Int?,
    val avatar: String,
    val codigoVinculacion: String,
    val rachaEntreRetos: Int,
    val mejorRachaEntreRetos: Int,
)

/** Un hijo visto desde la cuenta de un acudiente. */
data class HijoGuardado(val nino: NinoGuardado, val grupo: String, val jardin: String, val vinculadoEn: OffsetDateTime, val autorizadaEn: OffsetDateTime)

data class ControlGuardado(
    val limiteMinutos: Int,
    val dificultadSugerida: Dificultad,
    val correosLogro: Boolean,
    val reporteSemanal: Boolean,
    val recordatorioSinJugar: Boolean = false,
)

data class AcudienteDeNino(val id: String, val nombre: String, val correo: String)

/** Niños, vinculaciones con acudientes y control parental. Se llama dentro de una transacción. */
class NinosRepositorio {

    fun porId(id: String): NinoGuardado? = Ninos.selectAll().where { Ninos.id eq id }.singleOrNull()?.aNino()

    fun porCodigoVinculacion(codigo: String): NinoGuardado? =
        Ninos.selectAll().where { Ninos.codigoVinculacion eq codigo.trim().uppercase() }.singleOrNull()?.aNino()

    fun existeCodigo(codigo: String): Boolean = Ninos.selectAll().where { Ninos.codigoVinculacion eq codigo }.any()

    fun delGrupo(grupoId: String): List<NinoGuardado> =
        Ninos.selectAll().where { Ninos.grupoId eq grupoId }.orderBy(Ninos.nombre to SortOrder.ASC).map { it.aNino() }

    fun crear(nino: NinoGuardado, ahora: OffsetDateTime) {
        Ninos.insert {
            it[id] = nino.id
            it[grupoId] = nino.grupoId
            it[nombre] = nino.nombre
            it[edad] = nino.edad?.toShort()
            it[avatar] = nino.avatar
            it[codigoVinculacion] = nino.codigoVinculacion
            it[rachaEntreRetos] = 0
            it[mejorRachaEntreRetos] = 0
            it[creadoEn] = ahora
        }
        ControlesParentales.insert {
            it[ninoId] = nino.id
            it[limiteMinutos] = 20
            it[dificultadSugerida] = Dificultad.Medio.aColumna()
            it[correosLogro] = true
            it[reporteSemanal] = true
            it[recordatorioSinJugar] = false
            it[actualizadoEn] = ahora
        }
    }

    fun cambiarCodigo(ninoId: String, codigo: String) {
        Ninos.update({ Ninos.id eq ninoId }) { it[codigoVinculacion] = codigo }
    }

    fun guardarRacha(ninoId: String, racha: Int, mejor: Int) {
        Ninos.update({ Ninos.id eq ninoId }) {
            it[rachaEntreRetos] = racha
            it[mejorRachaEntreRetos] = mejor
        }
    }

    /** Borra al niño y, en cascada, sus resultados, insignias, vinculaciones y correos. */
    fun eliminar(ninoId: String) {
        Ninos.deleteWhere { Ninos.id eq ninoId }
    }

    /** `false` si ya estaba vinculado. */
    fun vincular(acudienteId: String, ninoId: String, ahora: OffsetDateTime): Boolean {
        if (estaVinculado(acudienteId, ninoId)) return false
        Vinculaciones.insert {
            it[Vinculaciones.acudienteId] = acudienteId
            it[Vinculaciones.ninoId] = ninoId
            it[vinculadoEn] = ahora
            it[autorizadaEn] = ahora
        }
        return true
    }

    fun estaVinculado(acudienteId: String, ninoId: String): Boolean =
        Vinculaciones.selectAll().where { (Vinculaciones.acudienteId eq acudienteId) and (Vinculaciones.ninoId eq ninoId) }.any()

    fun tieneAcudiente(ninoId: String): Boolean = Vinculaciones.selectAll().where { Vinculaciones.ninoId eq ninoId }.any()

    fun acudientesDe(ninoId: String): List<AcudienteDeNino> =
        Vinculaciones.join(Usuarios, JoinType.INNER, Vinculaciones.acudienteId, Usuarios.id)
            .selectAll().where { Vinculaciones.ninoId eq ninoId }
            .map { AcudienteDeNino(it[Usuarios.id], it[Usuarios.nombre], it[Usuarios.correo]) }

    fun hijosDe(acudienteId: String): List<HijoGuardado> =
        Vinculaciones.join(Ninos, JoinType.INNER, Vinculaciones.ninoId, Ninos.id)
            .join(Grupos, JoinType.INNER, Ninos.grupoId, Grupos.id)
            .selectAll().where { Vinculaciones.acudienteId eq acudienteId }
            .orderBy(Vinculaciones.vinculadoEn to SortOrder.ASC)
            .map {
                HijoGuardado(it.aNino(), it[Grupos.nombre], it[Grupos.jardin], it[Vinculaciones.vinculadoEn], it[Vinculaciones.autorizadaEn])
            }

    fun control(ninoId: String): ControlGuardado =
        ControlesParentales.selectAll().where { ControlesParentales.ninoId eq ninoId }.singleOrNull()?.let {
            ControlGuardado(
                limiteMinutos = it[ControlesParentales.limiteMinutos].toInt(),
                dificultadSugerida = dificultadDeColumna(it[ControlesParentales.dificultadSugerida]),
                correosLogro = it[ControlesParentales.correosLogro],
                reporteSemanal = it[ControlesParentales.reporteSemanal],
                recordatorioSinJugar = it[ControlesParentales.recordatorioSinJugar],
            )
        } ?: ControlGuardado(20, Dificultad.Medio, correosLogro = true, reporteSemanal = true)

    fun guardarControl(ninoId: String, control: ControlGuardado, ahora: OffsetDateTime) {
        ControlesParentales.update({ ControlesParentales.ninoId eq ninoId }) {
            it[limiteMinutos] = control.limiteMinutos.toShort()
            it[dificultadSugerida] = control.dificultadSugerida.aColumna()
            it[correosLogro] = control.correosLogro
            it[reporteSemanal] = control.reporteSemanal
            it[recordatorioSinJugar] = control.recordatorioSinJugar
            it[actualizadoEn] = ahora
        }
    }

    fun solicitudEliminacion(ninoId: String): LocalDate? =
        SolicitudesEliminacion.selectAll().where { SolicitudesEliminacion.ninoId eq ninoId }.singleOrNull()
            ?.get(SolicitudesEliminacion.solicitadaEl)

    fun solicitarEliminacion(ninoId: String, acudienteId: String, fecha: LocalDate) {
        if (solicitudEliminacion(ninoId) != null) return
        SolicitudesEliminacion.insert {
            it[SolicitudesEliminacion.ninoId] = ninoId
            it[solicitadaPor] = acudienteId
            it[solicitadaEl] = fecha
        }
    }

    fun cancelarEliminacion(ninoId: String) {
        SolicitudesEliminacion.deleteWhere { SolicitudesEliminacion.ninoId eq ninoId }
    }

    private fun ResultRow.aNino() = NinoGuardado(
        id = this[Ninos.id],
        grupoId = this[Ninos.grupoId],
        nombre = this[Ninos.nombre],
        edad = this[Ninos.edad]?.toInt(),
        avatar = this[Ninos.avatar],
        codigoVinculacion = this[Ninos.codigoVinculacion],
        rachaEntreRetos = this[Ninos.rachaEntreRetos],
        mejorRachaEntreRetos = this[Ninos.mejorRachaEntreRetos],
    )
}
