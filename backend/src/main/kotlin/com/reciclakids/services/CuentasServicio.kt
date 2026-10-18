package com.reciclakids.services

import at.favre.lib.crypto.bcrypt.BCrypt
import com.reciclakids.config.BaseDatos
import com.reciclakids.models.Rol
import com.reciclakids.models.dto.CuentaDto
import com.reciclakids.models.dto.RegistroAcudienteSolicitud
import com.reciclakids.models.dto.RegistroDocenteSolicitud
import com.reciclakids.models.dto.RolAdulto
import com.reciclakids.models.dto.SesionDto
import com.reciclakids.plugins.Conflicto
import com.reciclakids.plugins.DatosInvalidos
import com.reciclakids.plugins.NoAutenticado
import com.reciclakids.plugins.Tokens
import com.reciclakids.repositories.CuentasRepositorio
import com.reciclakids.repositories.NinosRepositorio
import com.reciclakids.repositories.Usuario
import java.time.Duration

/** Envía los correos de la cuenta (por ahora, el enlace de recuperación). */
fun interface NotificadorCuentas {
    suspend fun enlaceRecuperacion(correo: String, nombre: String, enlace: String)
}

/**
 * Componente de seguridad (C4 N3): registro, inicio de sesión, refresco y recuperación. Las
 * contraseñas se guardan con BCrypt, que incluye su propia sal (RNF de hash + salt).
 */
class CuentasServicio(
    private val bd: BaseDatos,
    private val cuentas: CuentasRepositorio,
    private val ninos: NinosRepositorio,
    private val tokens: Tokens,
    private val reloj: Reloj,
    private val notificador: NotificadorCuentas,
    private val urlPublica: String,
) {
    suspend fun registrarDocente(datos: RegistroDocenteSolicitud) {
        val nombre = Validacion.texto(datos.nombre, "tu nombre", 120)
        val correo = Validacion.correo(datos.correo)
        val jardin = Validacion.texto(datos.jardin, "el jardín infantil", 120)
        val grupo = Validacion.texto(datos.grupo, "el grupo", 80)
        val hash = hashear(Validacion.contrasena(datos.contrasena))
        bd.transaccion {
            if (cuentas.usuarioPorCorreo(correo) != null) throw correoEnUso()
            val ahora = reloj.ahora()
            val id = nuevoId()
            cuentas.crearUsuario(id, Rol.DOCENTE, nombre, correo, hash, ahora)
            cuentas.crearGrupo(nuevoId(), id, jardin, grupo, ahora)
        }
    }

    suspend fun registrarAcudiente(datos: RegistroAcudienteSolicitud) {
        if (!datos.autorizaDatosMenor) {
            throw DatosInvalidos("Sin la autorización de datos del menor no se puede crear la cuenta", "autorizacion_requerida")
        }
        val nombre = Validacion.texto(datos.nombre, "tu nombre", 120)
        val correo = Validacion.correo(datos.correo)
        val hash = hashear(Validacion.contrasena(datos.contrasena))
        bd.transaccion {
            val nino = ninos.porCodigoVinculacion(datos.codigoVinculacion) ?: throw codigoInvalido()
            if (cuentas.usuarioPorCorreo(correo) != null) throw correoEnUso()
            val ahora = reloj.ahora()
            val id = nuevoId()
            cuentas.crearUsuario(id, Rol.ACUDIENTE, nombre, correo, hash, ahora)
            ninos.vincular(id, nino.id, ahora)
        }
    }

    suspend fun iniciarSesion(correo: String, contrasena: String): SesionDto {
        val usuario = bd.transaccion { cuentas.usuarioPorCorreo(correo.trim()) }
        // Sin usuario igual se compara contra un hash: el tiempo de respuesta no revela si existe.
        val valida = BCrypt.verifyer().verify(contrasena.toCharArray(), usuario?.hashContrasena ?: HashFicticio).verified
        if (usuario == null || !valida) throw NoAutenticado("credenciales_invalidas", "Correo o contraseña incorrectos")
        return bd.transaccion { abrirSesion(usuario) }
    }

    /** Rota el token de refresco. Si llega uno ya revocado, alguien lo copió: se cierran todas las sesiones. */
    suspend fun refrescar(tokenRefresco: String): SesionDto {
        val sesion = bd.transaccion { cuentas.sesionPorHash(Tokens.hash(tokenRefresco)) } ?: throw sesionVencida()
        if (sesion.revocada) {
            // En su propia transacción: el error de abajo no debe deshacer el cierre.
            bd.transaccion { cuentas.revocarSesionesDe(sesion.usuarioId) }
            throw sesionVencida()
        }
        if (sesion.expiraEn.isBefore(reloj.ahora())) throw sesionVencida()
        return bd.transaccion {
            cuentas.revocarSesion(sesion.id)
            val usuario = cuentas.usuarioPorId(sesion.usuarioId) ?: throw sesionVencida()
            abrirSesion(usuario)
        }
    }

    suspend fun cerrarSesion(tokenRefresco: String) = bd.transaccion {
        cuentas.sesionPorHash(Tokens.hash(tokenRefresco))?.let { cuentas.revocarSesion(it.id) }
    }

    /** Nunca revela si el correo tiene cuenta: responde igual en los dos casos. */
    suspend fun solicitarRecuperacion(correo: String) {
        val token = tokens.refresco()
        val usuario = bd.transaccion {
            val usuario = cuentas.usuarioPorCorreo(correo.trim()) ?: return@transaccion null
            val ahora = reloj.ahora()
            cuentas.crearRecuperacion(nuevoId(), usuario.id, Tokens.hash(token), ahora.plus(DuracionRecuperacion), ahora)
            usuario
        } ?: return
        notificador.enlaceRecuperacion(usuario.correo, usuario.nombre, "$urlPublica/restablecer?token=$token")
    }

    suspend fun restablecer(token: String, contrasena: String) {
        val hash = hashear(Validacion.contrasena(contrasena))
        bd.transaccion {
            val recuperacion = cuentas.recuperacionPorHash(Tokens.hash(token))
            if (recuperacion == null || recuperacion.usada || recuperacion.expiraEn.isBefore(reloj.ahora())) {
                throw DatosInvalidos("El enlace venció o ya se usó. Pide uno nuevo desde la app.", "enlace_vencido")
            }
            cuentas.cambiarContrasena(recuperacion.usuarioId, hash)
            cuentas.marcarRecuperacionUsada(recuperacion.id)
            cuentas.revocarSesionesDe(recuperacion.usuarioId)
        }
    }

    suspend fun cuenta(usuarioId: String): CuentaDto = bd.transaccion {
        val usuario = cuentas.usuarioPorId(usuarioId) ?: throw NoAutenticado()
        cuentaDto(usuario)
    }

    private fun abrirSesion(usuario: Usuario): SesionDto {
        val acceso = tokens.accesoAdulto(usuario.id, usuario.rol)
        val refresco = tokens.refresco()
        val ahora = reloj.ahora()
        cuentas.crearSesion(nuevoId(), usuario.id, Tokens.hash(refresco), ahora.plus(Tokens.DuracionRefresco), ahora)
        return SesionDto(acceso.token, acceso.segundos, refresco, cuentaDto(usuario))
    }

    private fun cuentaDto(usuario: Usuario): CuentaDto {
        val grupo = if (usuario.rol == Rol.DOCENTE) cuentas.grupoDeDocente(usuario.id) else null
        return CuentaDto(
            nombre = usuario.nombre,
            correo = usuario.correo,
            rol = if (usuario.rol == Rol.DOCENTE) RolAdulto.Docente else RolAdulto.Acudiente,
            jardin = grupo?.jardin,
            grupo = grupo?.nombre,
        )
    }

    companion object {
        private const val CostoBcrypt = 12
        private val DuracionRecuperacion: Duration = Duration.ofMinutes(30)
        private val HashFicticio = BCrypt.withDefaults().hashToString(CostoBcrypt, "no-es-una-contrasena".toCharArray())

        fun hashear(contrasena: String): String = BCrypt.withDefaults().hashToString(CostoBcrypt, contrasena.toCharArray())

        fun correoEnUso() = Conflicto("correo_en_uso", "Ese correo ya tiene una cuenta")
        fun codigoInvalido() = DatosInvalidos("Ese código de vinculación no existe. Pídeselo a la docente.", "codigo_invalido")
        private fun sesionVencida() = NoAutenticado("sesion_vencida", "Tu sesión venció. Inicia sesión de nuevo.")
    }
}
