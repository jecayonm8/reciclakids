package com.reciclakids.network.api

import com.reciclakids.model.CuentaAdulto
import com.reciclakids.network.RegistroAcudiente
import com.reciclakids.network.RegistroDocente
import com.reciclakids.network.ResultadoRegistro
import com.reciclakids.network.ResultadoSesion
import com.reciclakids.network.ServicioAcceso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Acceso de los adultos contra la API de Autenticación (/auth y /cuenta). Los tokens quedan en
 * [sesion]; [alcance] solo sirve para avisar el cierre de sesión al backend sin hacer esperar a la
 * pantalla.
 */
class ServicioAccesoApi(
    private val cliente: ClienteApi,
    private val sesion: SesionAdultoGuardada,
    private val alcance: CoroutineScope,
) : ServicioAcceso {

    override suspend fun iniciarSesion(correo: String, contrasena: String): ResultadoSesion =
        when (val respuesta = cliente.post<SesionDto>("auth/sesion", InicioSesionSolicitud(correo.trim(), contrasena), Credencial.Ninguna)) {
            is RespuestaApi.Exito -> {
                sesion.guardar(respuesta.dato)
                ResultadoSesion.Iniciada(respuesta.dato.cuenta.aModelo())
            }
            // 401 credenciales_invalidas. Un 422 (correo mal escrito) tampoco tiene cuenta.
            is RespuestaApi.Rechazo -> if (respuesta.estado == 401 || respuesta.estado == 422) {
                ResultadoSesion.CredencialesInvalidas
            } else {
                ResultadoSesion.SinConexion
            }
            RespuestaApi.SinConexion, RespuestaApi.SesionVencida -> ResultadoSesion.SinConexion
        }

    override suspend fun registrarDocente(datos: RegistroDocente): ResultadoRegistro = registro(
        cliente.post<Unit>(
            "auth/registro/docente",
            RegistroDocenteSolicitud(datos.nombre, datos.correo, datos.jardin, datos.grupo, datos.contrasena),
            Credencial.Ninguna,
        )
    )

    // Solo se llama tras la casilla explícita de UI-07: por eso autorizaDatosMenor va en true.
    override suspend fun registrarAcudiente(datos: RegistroAcudiente): ResultadoRegistro = registro(
        cliente.post<Unit>(
            "auth/registro/acudiente",
            RegistroAcudienteSolicitud(datos.nombre, datos.correo, datos.contrasena, datos.codigoVinculacion, autorizaDatosMenor = true),
            Credencial.Ninguna,
        )
    )

    // Siempre 202 en el backend: cualquier respuesta suya cuenta como enviado.
    override suspend fun enviarEnlaceRecuperacion(correo: String): Boolean =
        when (cliente.post<Unit>("auth/recuperacion", RecuperacionSolicitud(correo.trim()), Credencial.Ninguna)) {
            is RespuestaApi.Exito, is RespuestaApi.Rechazo -> true
            RespuestaApi.SinConexion, RespuestaApi.SesionVencida -> false
        }

    override suspend fun sesionGuardada(): CuentaAdulto? {
        val guardada = sesion.leer() ?: return null
        return when (val respuesta = cliente.get<CuentaDto>("cuenta")) {
            is RespuestaApi.Exito -> {
                sesion.actualizarCuenta(respuesta.dato)
                respuesta.dato.aModelo()
            }
            // Sin red se entra con la cuenta que quedó guardada: cada pantalla muestra su estado sin conexión.
            RespuestaApi.SinConexion -> guardada.cuenta.aModelo()
            RespuestaApi.SesionVencida, is RespuestaApi.Rechazo -> null
        }
    }

    override fun cerrarSesion() {
        val guardada = sesion.leer() ?: return
        sesion.borrar()
        // Si no hay red, el token de refresco vence solo a los 30 días; en el teléfono ya no queda.
        alcance.launch { cliente.post<Unit>("auth/cierre", RefrescoSolicitud(guardada.tokenRefresco), Credencial.Ninguna) }
    }

    private fun registro(respuesta: RespuestaApi<Unit>): ResultadoRegistro = when (respuesta) {
        is RespuestaApi.Exito -> ResultadoRegistro.Creada
        is RespuestaApi.Rechazo -> when (respuesta.error) {
            "correo_en_uso" -> ResultadoRegistro.CorreoEnUso
            "codigo_invalido" -> ResultadoRegistro.CodigoInvalido
            else -> ResultadoRegistro.SinConexion
        }
        RespuestaApi.SinConexion, RespuestaApi.SesionVencida -> ResultadoRegistro.SinConexion
    }
}
