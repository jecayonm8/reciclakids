package com.reciclakids.repositories

import com.reciclakids.models.Grupos
import com.reciclakids.models.Recuperaciones
import com.reciclakids.models.Rol
import com.reciclakids.models.Sesiones
import com.reciclakids.models.Usuarios
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.time.OffsetDateTime

data class Usuario(val id: String, val rol: Rol, val nombre: String, val correo: String, val hashContrasena: String)

data class SesionGuardada(val id: String, val usuarioId: String, val expiraEn: OffsetDateTime, val revocada: Boolean)

data class RecuperacionGuardada(val id: String, val usuarioId: String, val expiraEn: OffsetDateTime, val usada: Boolean)

data class GrupoDocente(val id: String, val jardin: String, val nombre: String)

/** Acceso a usuarios, sesiones y recuperaciones. Se llama dentro de una transacción. */
class CuentasRepositorio {

    fun usuarioPorCorreo(correo: String): Usuario? =
        Usuarios.selectAll().where { Usuarios.correo.lowerCase() eq correo.lowercase() }.singleOrNull()?.aUsuario()

    fun usuarioPorId(id: String): Usuario? = Usuarios.selectAll().where { Usuarios.id eq id }.singleOrNull()?.aUsuario()

    fun crearUsuario(id: String, rol: Rol, nombre: String, correo: String, hash: String, ahora: OffsetDateTime) {
        Usuarios.insert {
            it[Usuarios.id] = id
            it[Usuarios.rol] = rol.name
            it[Usuarios.nombre] = nombre
            it[Usuarios.correo] = correo
            it[Usuarios.hashContrasena] = hash
            it[Usuarios.creadoEn] = ahora
        }
    }

    fun actualizarUsuario(id: String, nombre: String, correo: String) {
        Usuarios.update({ Usuarios.id eq id }) {
            it[Usuarios.nombre] = nombre
            it[Usuarios.correo] = correo
        }
    }

    fun cambiarContrasena(id: String, hash: String) {
        Usuarios.update({ Usuarios.id eq id }) { it[Usuarios.hashContrasena] = hash }
    }

    fun grupoDeDocente(docenteId: String): GrupoDocente? =
        Grupos.selectAll().where { Grupos.docenteId eq docenteId }.singleOrNull()?.let {
            GrupoDocente(it[Grupos.id], it[Grupos.jardin], it[Grupos.nombre])
        }

    fun idsDeGrupos(): List<String> = Grupos.selectAll().map { it[Grupos.id] }

    fun grupoPorId(id: String): GrupoDocente? =
        Grupos.selectAll().where { Grupos.id eq id }.singleOrNull()?.let { GrupoDocente(it[Grupos.id], it[Grupos.jardin], it[Grupos.nombre]) }

    fun crearGrupo(id: String, docenteId: String, jardin: String, nombre: String, ahora: OffsetDateTime) {
        Grupos.insert {
            it[Grupos.id] = id
            it[Grupos.docenteId] = docenteId
            it[Grupos.jardin] = jardin
            it[Grupos.nombre] = nombre
            it[Grupos.avisoPublicacion] = true
            it[Grupos.avisoResumenDiario] = true
            it[Grupos.avisoNinosSinJugar] = false
            it[Grupos.creadoEn] = ahora
        }
    }

    fun crearSesion(id: String, usuarioId: String, hashRefresco: String, expiraEn: OffsetDateTime, ahora: OffsetDateTime) {
        Sesiones.insert {
            it[Sesiones.id] = id
            it[Sesiones.usuarioId] = usuarioId
            it[Sesiones.hashRefresco] = hashRefresco
            it[Sesiones.expiraEn] = expiraEn
            it[Sesiones.revocada] = false
            it[Sesiones.creadaEn] = ahora
        }
    }

    fun sesionPorHash(hash: String): SesionGuardada? =
        Sesiones.selectAll().where { Sesiones.hashRefresco eq hash }.singleOrNull()?.let {
            SesionGuardada(it[Sesiones.id], it[Sesiones.usuarioId], it[Sesiones.expiraEn], it[Sesiones.revocada])
        }

    fun revocarSesion(id: String) {
        Sesiones.update({ Sesiones.id eq id }) { it[Sesiones.revocada] = true }
    }

    fun revocarSesionesDe(usuarioId: String) {
        Sesiones.update({ (Sesiones.usuarioId eq usuarioId) and (Sesiones.revocada eq false) }) { it[Sesiones.revocada] = true }
    }

    fun crearRecuperacion(id: String, usuarioId: String, hashToken: String, expiraEn: OffsetDateTime, ahora: OffsetDateTime) {
        Recuperaciones.insert {
            it[Recuperaciones.id] = id
            it[Recuperaciones.usuarioId] = usuarioId
            it[Recuperaciones.hashToken] = hashToken
            it[Recuperaciones.expiraEn] = expiraEn
            it[Recuperaciones.usada] = false
            it[Recuperaciones.creadaEn] = ahora
        }
    }

    fun recuperacionPorHash(hash: String): RecuperacionGuardada? =
        Recuperaciones.selectAll().where { Recuperaciones.hashToken eq hash }.singleOrNull()?.let {
            RecuperacionGuardada(it[Recuperaciones.id], it[Recuperaciones.usuarioId], it[Recuperaciones.expiraEn], it[Recuperaciones.usada])
        }

    fun marcarRecuperacionUsada(id: String) {
        Recuperaciones.update({ Recuperaciones.id eq id }) { it[Recuperaciones.usada] = true }
    }

    private fun org.jetbrains.exposed.v1.core.ResultRow.aUsuario() = Usuario(
        id = this[Usuarios.id],
        rol = Rol.valueOf(this[Usuarios.rol]),
        nombre = this[Usuarios.nombre],
        correo = this[Usuarios.correo],
        hashContrasena = this[Usuarios.hashContrasena],
    )
}
