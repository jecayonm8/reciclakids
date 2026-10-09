package com.reciclakids.network

import com.reciclakids.model.ControlParental
import com.reciclakids.model.HijoVinculado
import com.reciclakids.model.HistorialCorreos
import com.reciclakids.model.InicioPadres
import com.reciclakids.model.LogrosHijo
import com.reciclakids.model.PerfilAcudiente
import com.reciclakids.model.PrivacidadHijo
import com.reciclakids.model.ReporteHijo
import java.time.LocalDate

sealed interface ResultadoVinculacion {
    data class Vinculado(val hijo: HijoVinculado) : ResultadoVinculacion

    /** El código ya estaba usado en esta cuenta. */
    data class YaVinculado(val hijo: HijoVinculado) : ResultadoVinculacion

    data object CodigoInvalido : ResultadoVinculacion
    data object SinConexion : ResultadoVinculacion
}

/**
 * Lo que necesita el Modo Padres (UI-29 a UI-35). Cada consulta recibe el hijo o hija elegido y
 * el backend verifica que esté vinculado a la cuenta: un acudiente nunca ve a otros niños.
 */
interface ServicioPadres {
    /** Fecha del día según el reloj del servicio. */
    fun hoy(): LocalDate

    /** Nunca vacía: la cuenta se crea con el código de vinculación de un hijo o hija. */
    suspend fun hijos(): Respuesta<List<HijoVinculado>>

    suspend fun inicio(hijoId: String): Respuesta<InicioPadres>

    /** [semana] es el lunes de la semana a ver; `null` es la más reciente. */
    suspend fun reporte(hijoId: String, semana: LocalDate?): Respuesta<ReporteHijo>

    suspend fun logros(hijoId: String): Respuesta<LogrosHijo>

    suspend fun correos(hijoId: String): Respuesta<HistorialCorreos>

    suspend fun controlParental(hijoId: String): Respuesta<ControlParental>

    suspend fun guardarControlParental(hijoId: String, control: ControlParental): Boolean

    suspend fun privacidad(hijoId: String): Respuesta<PrivacidadHijo>

    /** Devuelve la fecha en que quedó la solicitud, o `null` sin conexión. */
    suspend fun solicitarEliminacion(hijoId: String): LocalDate?

    suspend fun cancelarSolicitudEliminacion(hijoId: String): Boolean

    suspend fun perfil(): Respuesta<PerfilAcudiente>

    /** Vincula otro hijo o hija con el código que entrega su docente. */
    suspend fun vincular(codigo: String): ResultadoVinculacion
}
