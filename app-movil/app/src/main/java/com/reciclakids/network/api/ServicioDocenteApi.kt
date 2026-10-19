package com.reciclakids.network.api

import com.reciclakids.model.AvisosDocente
import com.reciclakids.model.BibliotecaDocente
import com.reciclakids.model.Confusion
import com.reciclakids.model.DetalleNino
import com.reciclakids.model.EstadoNinoHoy
import com.reciclakids.model.EstadoReto
import com.reciclakids.model.GrupoHoy
import com.reciclakids.model.InsigniaNino
import com.reciclakids.model.NinoAgregado
import com.reciclakids.model.NinoHoy
import com.reciclakids.model.PerfilDocente
import com.reciclakids.model.ReporteSemanal
import com.reciclakids.model.RetoBorrador
import com.reciclakids.model.RetoDocente
import com.reciclakids.model.TableroDocente
import com.reciclakids.model.ValorDia
import com.reciclakids.model.ValorNino
import com.reciclakids.model.nombrePorDefecto
import com.reciclakids.network.DatosPerfilDocente
import com.reciclakids.network.Respuesta
import com.reciclakids.network.ResultadoEdicionPerfil
import com.reciclakids.network.ResultadoPublicacion
import com.reciclakids.network.ServicioDocente
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

/** Prefijo de los borradores que solo existen en este teléfono porque no hubo red al publicar. */
private const val PrefijoBorradorLocal = "local-"

/**
 * El Modo Docente (UI-21 a UI-28) contra la API /docente, con el token de la docente. Lo que no se
 * pudo publicar sin red queda como borrador en el teléfono mientras dure la sesión, y se publica
 * al reintentar.
 */
class ServicioDocenteApi(
    private val cliente: ClienteApi,
    private val sesion: SesionAdultoGuardada? = null,
    reloj: Clock = Clock.systemDefaultZone(),
    /** Cada cuánto se pregunta cuántos niños entraron mientras la docente muestra el código (UI-24). */
    private val intervaloIngresosMs: Long = 4_000,
) : ServicioDocente {

    private val hoyServidor = HoyServidor(reloj)

    /** Borrador local → id del reto programado que editaba, si editaba uno. */
    private class BorradorLocal(val reto: RetoDocente, val idEnServidor: String?)

    private val mutex = Mutex()
    private val borradores = mutableListOf<BorradorLocal>()

    override fun hoy(): LocalDate = hoyServidor.hoy()

    override suspend fun tablero(): Respuesta<TableroDocente> = cliente.get<TableroDto>("docente/tablero").comoRespuesta {
        hoyServidor.recordar(it.hoy)
        TableroDocente(
            hoy = LocalDate.parse(it.hoy),
            nombreDocente = it.nombreDocente,
            grupo = it.grupo,
            retoHoy = it.retoHoy?.aModelo(),
            completaron = it.completaron,
            totalNinos = it.totalNinos,
            aciertosPromedio = it.aciertosPromedio,
        )
    }

    override suspend fun biblioteca(): Respuesta<BibliotecaDocente> {
        val locales = mutex.withLock { borradores.map { it.reto } }
        return cliente.get<BibliotecaDto>("docente/retos").comoRespuesta {
            hoyServidor.recordar(it.hoy)
            BibliotecaDocente(LocalDate.parse(it.hoy), locales + it.retos.map { reto -> reto.aModelo() })
        }
    }

    override suspend fun reto(id: String): Respuesta<RetoDocente?> {
        if (id.startsWith(PrefijoBorradorLocal)) return Respuesta.Ok(mutex.withLock { borradores.firstOrNull { it.reto.id == id }?.reto })
        return cliente.get<RetoDocenteDto>("docente/retos/$id").comoRespuestaOpcional { it.aModelo() }
    }

    override suspend fun publicar(borrador: RetoBorrador): ResultadoPublicacion {
        val local = borrador.idExistente?.let { id -> mutex.withLock { borradores.firstOrNull { it.reto.id == id } } }
        val solicitud = RetoBorradorSolicitud(
            categorias = borrador.categorias.sorted(),
            dificultad = borrador.dificultad,
            fecha = borrador.fecha.toString(),
            idExistente = if (local != null) local.idEnServidor else borrador.idExistente,
            nombre = borrador.nombre,
            reutilizadoDe = borrador.reutilizadoDe?.takeUnless { it.startsWith(PrefijoBorradorLocal) },
        )
        val respuesta = cliente.post<RetoDocenteDto>("docente/retos", solicitud)
        val publicado = (respuesta as? RespuestaApi.Exito)?.dato?.let { runCatching { it.aModelo() }.getOrNull() }
        if (publicado != null) {
            if (local != null) mutex.withLock { borradores.remove(local) }
            return ResultadoPublicacion.Publicado(publicado)
        }
        // Sin red (o si el backend no lo aceptó) nada se pierde: queda como borrador para reintentar.
        val reto = RetoDocente(
            id = local?.reto?.id ?: "$PrefijoBorradorLocal${UUID.randomUUID()}",
            nombre = borrador.nombre ?: nombrePorDefecto(borrador.categorias),
            categorias = borrador.categorias,
            dificultad = borrador.dificultad,
            fecha = borrador.fecha,
            estado = EstadoReto.Borrador,
            reutilizadoDe = borrador.reutilizadoDe,
        )
        mutex.withLock {
            borradores.removeAll { it.reto.id == reto.id }
            borradores.add(0, BorradorLocal(reto, local?.idEnServidor ?: borrador.idExistente))
        }
        return ResultadoPublicacion.GuardadoComoBorrador(reto)
    }

    /** Pregunta cada [intervaloIngresosMs] mientras alguien observa; sin red no emite. */
    override fun ninosQueEntraron(retoId: String): Flow<Int> {
        if (retoId.startsWith(PrefijoBorradorLocal)) return emptyFlow()
        return flow {
            while (true) {
                val respuesta = cliente.get<IngresosDto>("docente/retos/$retoId/ingresos")
                if (respuesta is RespuestaApi.Exito) emit(respuesta.dato.ninosQueEntraron)
                delay(intervaloIngresosMs)
            }
        }.distinctUntilChanged()
    }

    override suspend fun grupoHoy(): Respuesta<GrupoHoy> = cliente.get<GrupoHoyDto>("docente/grupo/hoy").comoRespuesta { grupo ->
        GrupoHoy(grupo.nombreGrupo, grupo.retoHoy, grupo.ninos.map { NinoHoy(it.id, it.nombre, it.estado.aModelo()) })
    }

    override suspend fun detalleNino(id: String): Respuesta<DetalleNino?> =
        cliente.get<DetalleNinoDto>("docente/ninos/$id").comoRespuestaOpcional {
            DetalleNino(
                id = it.id,
                nombre = it.nombre,
                aciertosPorSemana = it.aciertosPorSemana,
                aciertosPorCategoria = it.aciertosPorCategoria,
                confusion = it.confusion?.let { c -> Confusion(conArticulo(c.residuo), c.correcta, c.elegida) },
                insignias = it.insignias.map { insignia -> InsigniaNino(insignia.nombre, insignia.ganada) },
                codigoVinculacion = it.codigoVinculacion,
                acudienteVinculado = it.acudienteVinculado,
            )
        }

    override suspend fun agregarNino(nombre: String, inicialApellido: String): Respuesta<NinoAgregado> =
        cliente.post<NinoAgregadoDto>("docente/ninos", AgregarNinoSolicitud(nombre.trim(), inicialApellido.trim()))
            .comoRespuesta { NinoAgregado(it.id, it.nombre, it.codigoVinculacion) }

    override suspend fun regenerarCodigoVinculacion(ninoId: String): String? =
        (cliente.post<CodigoVinculacionDto>("docente/ninos/$ninoId/codigo-vinculacion") as? RespuestaApi.Exito)?.dato?.codigoVinculacion

    // Un 404 es que ya no estaba en el grupo: el resultado es el mismo.
    override suspend fun quitarNino(ninoId: String): Boolean {
        val respuesta = cliente.delete<Unit>("docente/ninos/$ninoId")
        return respuesta.exitosa || (respuesta is RespuestaApi.Rechazo && respuesta.estado == 404)
    }

    override suspend fun reporteSemanal(): Respuesta<ReporteSemanal> =
        cliente.get<ReporteSemanalDto>("docente/reportes/semanal").comoRespuesta {
            ReporteSemanal(
                inicio = LocalDate.parse(it.inicio),
                fin = LocalDate.parse(it.fin),
                completadosPct = it.completadosPct,
                cambioCompletados = it.cambioCompletados,
                aciertosPct = it.aciertosPct,
                cambioAciertos = it.cambioAciertos,
                retosPublicados = it.retosPublicados,
                retosReutilizados = it.retosReutilizados,
                insignias = it.insignias,
                cambioInsignias = it.cambioInsignias,
                participacionPorDia = it.participacionPorDia.map { dia -> ValorDia(LocalDate.parse(dia.fecha), dia.porcentaje) },
                completadosPorNino = it.completadosPorNino.map { nino -> ValorNino(nino.nombre, nino.porcentaje) },
            )
        }

    override suspend fun enviarReporteAcudientes(): Boolean = cliente.post<EnvioReporteDto>("docente/reportes/semanal/envio").exitosa

    override suspend fun perfil(): Respuesta<PerfilDocente> = cliente.get<PerfilDocenteDto>("docente/perfil").comoRespuesta { it.aModelo() }

    override suspend fun guardarPerfil(datos: DatosPerfilDocente): ResultadoEdicionPerfil =
        when (val respuesta = cliente.put<PerfilDocenteDto>("docente/perfil", PerfilDocenteSolicitud(datos.nombre, datos.correo, datos.jardin, datos.grupo))) {
            is RespuestaApi.Exito -> {
                val perfil = respuesta.dato
                sesion?.leer()?.let { sesion.actualizarCuenta(it.cuenta.copy(nombre = perfil.nombre, correo = perfil.correo, jardin = perfil.jardin, grupo = perfil.grupo)) }
                ResultadoEdicionPerfil.Guardado
            }
            is RespuestaApi.Rechazo -> if (respuesta.error == "correo_en_uso") ResultadoEdicionPerfil.CorreoEnUso else ResultadoEdicionPerfil.SinConexion
            RespuestaApi.SinConexion, RespuestaApi.SesionVencida -> ResultadoEdicionPerfil.SinConexion
        }

    override suspend fun guardarAvisos(avisos: AvisosDocente): Boolean =
        cliente.put<Unit>("docente/avisos", AvisosDocenteDto(avisos.confirmarPublicacion, avisos.resumenDiario, avisos.ninosSinJugar)).exitosa

    override suspend fun eliminarDatosGrupo(): Boolean = cliente.delete<Unit>("docente/grupo/datos").exitosa
}

private fun RetoDocenteDto.aModelo() = RetoDocente(
    id = id,
    nombre = nombre,
    categorias = categorias.toSet(),
    dificultad = dificultad,
    fecha = fecha?.let(LocalDate::parse),
    estado = estado,
    codigo = codigo,
    aciertosPromedio = aciertosPromedio,
    reutilizadoDe = reutilizadoDe,
)

private fun EstadoNinoHoyDto.aModelo(): EstadoNinoHoy = when (tipo) {
    TipoEstadoNino.Completo -> EstadoNinoHoy.Completo(minutos ?: 0, aciertos ?: 0, total ?: 0)
    TipoEstadoNino.EnCurso -> EstadoNinoHoy.EnCurso(residuoActual ?: 1)
    TipoEstadoNino.SinEmpezar -> EstadoNinoHoy.SinEmpezar
}

private fun PerfilDocenteDto.aModelo() = PerfilDocente(
    nombre = nombre,
    correo = correo,
    jardin = jardin,
    grupo = grupo,
    totalNinos = totalNinos,
    avisos = AvisosDocente(avisos.confirmarPublicacion, avisos.resumenDiario, avisos.ninosSinJugar),
)

/** «empaque metalizado» → «el empaque metalizado», para la frase de UI-27. */
internal fun conArticulo(residuo: String): String {
    val primera = residuo.substringBefore(' ')
    return if (primera.endsWith("a")) "la $residuo" else "el $residuo"
}
