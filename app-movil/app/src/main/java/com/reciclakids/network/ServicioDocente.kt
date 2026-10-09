package com.reciclakids.network

import com.reciclakids.model.AvisosDocente
import com.reciclakids.model.BibliotecaDocente
import com.reciclakids.model.DetalleNino
import com.reciclakids.model.GrupoHoy
import com.reciclakids.model.PerfilDocente
import com.reciclakids.model.ReporteSemanal
import com.reciclakids.model.Reto
import com.reciclakids.model.RetoBorrador
import com.reciclakids.model.TableroDocente
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Respuesta de una consulta: los datos o la falta de conexión con el backend. */
sealed interface Respuesta<out T> {
    data class Ok<out T>(val dato: T) : Respuesta<T>
    data object SinConexion : Respuesta<Nothing>
}

sealed interface ResultadoPublicacion {
    /** Publicado para hoy (con código) o programado para otro día. */
    data class Publicado(val reto: Reto) : ResultadoPublicacion

    /** No hubo conexión: el reto quedó como borrador en este dispositivo para reintentar. */
    data class GuardadoComoBorrador(val reto: Reto) : ResultadoPublicacion
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
    suspend fun reto(id: String): Respuesta<Reto?>

    suspend fun publicar(borrador: RetoBorrador): ResultadoPublicacion

    /**
     * Niños que ya entraron con el código del reto, en vivo: emite cada vez que el número cambia.
     * Sin conexión no emite y la pantalla deja de mostrar el contador.
     */
    fun ninosQueEntraron(retoId: String): Flow<Int>

    suspend fun grupoHoy(): Respuesta<GrupoHoy>

    /** `null` si el niño no es del grupo de esta docente. */
    suspend fun detalleNino(id: String): Respuesta<DetalleNino?>

    suspend fun reporteSemanal(): Respuesta<ReporteSemanal>

    /** Envía el reporte de la semana a los acudientes, cada uno con los datos de su hijo o hija. */
    suspend fun enviarReporteAcudientes(): Boolean

    suspend fun perfil(): Respuesta<PerfilDocente>

    suspend fun guardarAvisos(avisos: AvisosDocente): Boolean

    /** El código anterior deja de servir para vincular; devuelve el nuevo o `null` sin conexión. */
    suspend fun regenerarCodigoVinculacion(): String?

    /** Borra resultados, insignias y reportes del grupo. No se puede deshacer. */
    suspend fun eliminarDatosGrupo(): Boolean
}
