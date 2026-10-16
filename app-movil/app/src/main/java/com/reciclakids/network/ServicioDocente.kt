package com.reciclakids.network

import com.reciclakids.model.AvisosDocente
import com.reciclakids.model.BibliotecaDocente
import com.reciclakids.model.DetalleNino
import com.reciclakids.model.GrupoHoy
import com.reciclakids.model.NinoAgregado
import com.reciclakids.model.PerfilDocente
import com.reciclakids.model.ReporteSemanal
import com.reciclakids.model.RetoDocente
import com.reciclakids.model.RetoBorrador
import com.reciclakids.model.TableroDocente
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Respuesta de una consulta: los datos o la falta de conexión con el backend. */
sealed interface Respuesta<out T> {
    data class Ok<out T>(val dato: T) : Respuesta<T>
    data object SinConexion : Respuesta<Nothing>
}

fun <T, R> Respuesta<T>.map(transformar: (T) -> R): Respuesta<R> = when (this) {
    is Respuesta.Ok -> Respuesta.Ok(transformar(dato))
    Respuesta.SinConexion -> Respuesta.SinConexion
}

sealed interface ResultadoPublicacion {
    /** Publicado para hoy (con código) o programado para otro día. */
    data class Publicado(val reto: RetoDocente) : ResultadoPublicacion

    /** No hubo conexión: el reto quedó como borrador en este dispositivo para reintentar. */
    data class GuardadoComoBorrador(val reto: RetoDocente) : ResultadoPublicacion
}

/**
 * Lo que necesita el Modo Docente (UI-21 a UI-28). Una docente solo ve los datos de su grupo;
 * el backend hace cumplir esa regla en cada consulta (RNF de control de acceso por rol).
 */
interface ServicioDocente {
    /** Fecha del día escolar según el reloj del servicio. */
    fun hoy(): LocalDate

    suspend fun tablero(): Respuesta<TableroDocente>

    suspend fun biblioteca(): Respuesta<BibliotecaDocente>

    /** `null` si el reto ya no existe. */
    suspend fun reto(id: String): Respuesta<RetoDocente?>

    suspend fun publicar(borrador: RetoBorrador): ResultadoPublicacion

    /**
     * Niños que ya entraron con el código del reto, en vivo: emite cada vez que el número cambia.
     * Sin conexión no emite y la pantalla deja de mostrar el contador.
     */
    fun ninosQueEntraron(retoId: String): Flow<Int>

    suspend fun grupoHoy(): Respuesta<GrupoHoy>

    /** `null` si el niño no es del grupo de esta docente. */
    suspend fun detalleNino(id: String): Respuesta<DetalleNino?>

    /**
     * Agrega un niño al grupo. Solo viajan su nombre y la inicial del apellido (una letra): se
     * guarda «Salomé M.», nunca el apellido completo (Ley 1581 de 2012). Cada niño recibe su
     * propio código de vinculación para su acudiente.
     */
    suspend fun agregarNino(nombre: String, inicialApellido: String): Respuesta<NinoAgregado>

    /**
     * El código anterior de ese niño deja de servir para vincular; si su acudiente ya estaba
     * vinculado, sigue vinculado. Devuelve el nuevo, o `null` sin conexión.
     */
    suspend fun regenerarCodigoVinculacion(ninoId: String): String?

    /** Quita al niño del grupo y borra sus resultados, insignias y reportes. No se puede deshacer. */
    suspend fun quitarNino(ninoId: String): Boolean

    suspend fun reporteSemanal(): Respuesta<ReporteSemanal>

    /** Envía el reporte de la semana a los acudientes, cada uno con los datos de su hijo o hija. */
    suspend fun enviarReporteAcudientes(): Boolean

    suspend fun perfil(): Respuesta<PerfilDocente>

    /** Cambia nombre, correo, jardín y grupo. Con el correo cambia también el usuario para iniciar sesión. */
    suspend fun guardarPerfil(datos: DatosPerfilDocente): ResultadoEdicionPerfil

    suspend fun guardarAvisos(avisos: AvisosDocente): Boolean

    /** Borra resultados, insignias y reportes del grupo. No se puede deshacer. */
    suspend fun eliminarDatosGrupo(): Boolean
}
