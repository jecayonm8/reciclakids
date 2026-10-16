package com.reciclakids.services

import com.reciclakids.config.BaseDatos
import com.reciclakids.models.CatalogoResiduos
import com.reciclakids.models.CategoriaResiduo
import com.reciclakids.models.EstadoReto
import com.reciclakids.models.Grupos
import com.reciclakids.models.Insignia
import com.reciclakids.models.dto.AgregarNinoSolicitud
import com.reciclakids.models.dto.AvisosDocenteDto
import com.reciclakids.models.dto.BibliotecaDto
import com.reciclakids.models.dto.ConfusionDto
import com.reciclakids.models.dto.DetalleNinoDto
import com.reciclakids.models.dto.EstadoNinoHoyDto
import com.reciclakids.models.dto.GrupoHoyDto
import com.reciclakids.models.dto.InsigniaNinoDto
import com.reciclakids.models.dto.NinoAgregadoDto
import com.reciclakids.models.dto.NinoHoyDto
import com.reciclakids.models.dto.PerfilDocenteDto
import com.reciclakids.models.dto.PerfilDocenteSolicitud
import com.reciclakids.models.dto.ReporteSemanalDto
import com.reciclakids.models.dto.RetoBorradorSolicitud
import com.reciclakids.models.dto.RetoDocenteDto
import com.reciclakids.models.dto.TableroDto
import com.reciclakids.models.dto.TipoEstadoNino
import com.reciclakids.models.dto.ValorDiaDto
import com.reciclakids.models.dto.ValorNinoDto
import com.reciclakids.models.residuosDelReto
import com.reciclakids.plugins.DatosInvalidos
import com.reciclakids.plugins.NoEncontrado
import com.reciclakids.plugins.Prohibido
import com.reciclakids.repositories.CuentasRepositorio
import com.reciclakids.repositories.GrupoDocente
import com.reciclakids.repositories.IntentoGuardado
import com.reciclakids.repositories.NinoGuardado
import com.reciclakids.repositories.NinosRepositorio
import com.reciclakids.repositories.PartidaGuardada
import com.reciclakids.repositories.ResultadosRepositorio
import com.reciclakids.repositories.RetoGuardado
import com.reciclakids.repositories.RetosRepositorio
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.security.SecureRandom
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Lo que necesita el Modo Docente (UI-21 a UI-28). Toda consulta parte del grupo de la docente
 * autenticada: nunca ve niños de otro grupo (ADR-09).
 */
class DocenteServicio(
    private val bd: BaseDatos,
    private val cuentas: CuentasRepositorio,
    private val ninos: NinosRepositorio,
    private val retos: RetosRepositorio,
    private val resultados: ResultadosRepositorio,
    private val codigos: GeneradorCodigos,
    private val reloj: Reloj,
) {
    private val azar = SecureRandom()

    suspend fun grupoId(docenteId: String): String = bd.transaccion { grupoDe(docenteId).id }

    suspend fun tablero(docenteId: String): TableroDto = bd.transaccion {
        val grupo = grupoDe(docenteId)
        val hoy = reloj.hoy()
        val docente = cuentas.usuarioPorId(docenteId)!!
        val total = ninos.delGrupo(grupo.id).size
        val retoHoy = retos.deHoy(grupo.id, hoy)
        val partidas = retoHoy?.let { resultados.partidasDeReto(it.id) }.orEmpty()
        TableroDto(
            hoy = hoy.toString(),
            nombreDocente = docente.nombre,
            grupo = grupo.nombre,
            retoHoy = retoHoy?.let { retoDto(it, hoy) },
            completaron = partidas.filter { it.terminada }.map { it.ninoId }.toSet().size,
            totalNinos = total,
            aciertosPromedio = retoHoy?.let { aciertosDeReto(it.id) },
        )
    }

    suspend fun biblioteca(docenteId: String): BibliotecaDto = bd.transaccion {
        val grupo = grupoDe(docenteId)
        val hoy = reloj.hoy()
        BibliotecaDto(hoy.toString(), retos.delGrupo(grupo.id).map { retoDto(it, hoy) })
    }

    suspend fun reto(docenteId: String, retoId: String): RetoDocenteDto = bd.transaccion {
        retoDto(retoDelGrupo(docenteId, retoId), reloj.hoy())
    }

    /** Publica hoy (con código) o programa para otro día: el código queda reservado para esa fecha. */
    suspend fun publicar(docenteId: String, borrador: RetoBorradorSolicitud): RetoDocenteDto = bd.transaccion {
        val grupo = grupoDe(docenteId)
        val hoy = reloj.hoy()
        val fecha = runCatching { LocalDate.parse(borrador.fecha) }.getOrElse { throw DatosInvalidos("Fecha inválida") }
        if (fecha.isBefore(hoy)) throw DatosInvalidos("No se puede publicar un reto para un día que ya pasó", "fecha_pasada")
        val categorias = borrador.categorias.toSet()
        if (categorias.isEmpty()) throw DatosInvalidos("Elige al menos una caneca", "sin_canecas")
        val existente = borrador.idExistente?.let { id ->
            retos.porId(id)?.takeIf { it.grupoId == grupo.id } ?: throw NoEncontrado("reto_no_encontrado", "Ese reto ya no existe")
        }
        if (existente != null && existente.estadoVisible(hoy) == EstadoReto.Publicado) {
            throw DatosInvalidos("Lo ya publicado no se edita: duplícalo", "reto_publicado")
        }
        val reutilizado = borrador.reutilizadoDe?.let { id -> retos.porId(id)?.takeIf { it.grupoId == grupo.id }?.id }
        val codigo = existente?.codigo?.takeIf { existente.fecha == fecha } ?: codigoLibre(fecha)
        val reto = RetoGuardado(
            id = existente?.id ?: nuevoId(),
            grupoId = grupo.id,
            nombre = borrador.nombre?.trim()?.takeIf { it.isNotEmpty() }?.take(120) ?: nombrePorDefecto(categorias),
            categorias = categorias,
            dificultad = borrador.dificultad,
            fecha = fecha,
            estado = EstadoReto.Publicado,
            codigo = codigo,
            reutilizadoDe = reutilizado ?: existente?.reutilizadoDe,
            creadoEn = existente?.creadoEn ?: reloj.ahora(),
        )
        retos.guardar(reto, residuosDelReto(categorias, borrador.dificultad, kotlin.random.Random(azar.nextLong())).map { it.id })
        retoDto(reto, hoy)
    }

    suspend fun ninosQueEntraron(docenteId: String, retoId: String): Int = bd.transaccion {
        retos.ingresos(retoDelGrupo(docenteId, retoId).id).size
    }

    suspend fun grupoHoy(docenteId: String): GrupoHoyDto = bd.transaccion {
        val grupo = grupoDe(docenteId)
        val retoHoy = retos.deHoy(grupo.id, reloj.hoy())
        val partidas = retoHoy?.let { resultados.partidasDeReto(it.id) }.orEmpty().groupBy { it.ninoId }
        val ingresos = retoHoy?.let { retos.ingresos(it.id) }.orEmpty()
        val total = retoHoy?.dificultad?.residuosPorReto ?: 0
        GrupoHoyDto(
            nombreGrupo = grupo.nombre,
            retoHoy = retoHoy?.nombre,
            ninos = ninos.delGrupo(grupo.id).map { nino ->
                NinoHoyDto(nino.id, nino.nombre, estadoHoy(partidas[nino.id].orEmpty(), nino.id in ingresos, total))
            },
        )
    }

    suspend fun detalleNino(docenteId: String, ninoId: String): DetalleNinoDto = bd.transaccion {
        val nino = ninoDelGrupo(docenteId, ninoId)
        val hoy = reloj.hoy()
        val desde = inicioSemana(hoy).minusWeeks(SemanasDetalle - 1L)
        val intentos = resultados.intentosDe(listOf(nino.id), desde, hoy)
        val ganadas = resultados.insignias(nino.id)
        DetalleNinoDto(
            id = nino.id,
            nombre = nino.nombre,
            aciertosPorSemana = (0 until SemanasDetalle)
                .map { desde.plusWeeks(it.toLong()) }
                .mapNotNull { lunes -> porcentaje(intentos.filter { inicioSemana(it.fecha) == lunes }) },
            aciertosPorCategoria = porCategoria(intentos),
            confusion = confusionFrecuente(intentos),
            insignias = Insignia.entries.map { InsigniaNinoDto(it.nombre, it in ganadas) },
            codigoVinculacion = nino.codigoVinculacion,
            acudienteVinculado = ninos.tieneAcudiente(nino.id),
        )
    }

    suspend fun agregarNino(docenteId: String, datos: AgregarNinoSolicitud): NinoAgregadoDto = bd.transaccion {
        val grupo = grupoDe(docenteId)
        val nombre = Validacion.nombreNino(datos.nombre, datos.inicialApellido)
        if (datos.edad != null && datos.edad !in 2..7) throw DatosInvalidos("La edad va de 2 a 7 años", "edad_invalida")
        val delGrupo = ninos.delGrupo(grupo.id)
        if (delGrupo.size >= MaxNinosPorGrupo) throw DatosInvalidos("El grupo ya tiene $MaxNinosPorGrupo niños", "grupo_lleno")
        val nino = NinoGuardado(
            id = nuevoId(),
            grupoId = grupo.id,
            nombre = nombre,
            edad = datos.edad,
            avatar = avatarLibre(delGrupo),
            codigoVinculacion = codigoVinculacionLibre(),
            rachaEntreRetos = 0,
            mejorRachaEntreRetos = 0,
        )
        ninos.crear(nino, reloj.ahora())
        NinoAgregadoDto(nino.id, nino.nombre, nino.codigoVinculacion)
    }

    /** El código anterior deja de servir; los acudientes ya vinculados no se ven afectados. */
    suspend fun regenerarCodigo(docenteId: String, ninoId: String): String = bd.transaccion {
        val nino = ninoDelGrupo(docenteId, ninoId)
        codigoVinculacionLibre().also { ninos.cambiarCodigo(nino.id, it) }
    }

    /** Quita al niño del grupo y borra todos sus datos. No se puede deshacer. */
    suspend fun quitarNino(docenteId: String, ninoId: String) = bd.transaccion {
        ninos.eliminar(ninoDelGrupo(docenteId, ninoId).id)
    }

    suspend fun reporteSemanal(docenteId: String): ReporteSemanalDto = bd.transaccion {
        val grupo = grupoDe(docenteId)
        reporteDelGrupo(grupo.id, ultimaSemanaCompleta(reloj.hoy()))
    }

    suspend fun perfil(docenteId: String): PerfilDocenteDto = bd.transaccion {
        val grupo = grupoDe(docenteId)
        val docente = cuentas.usuarioPorId(docenteId)!!
        val fila = Grupos.selectAll().where { Grupos.id eq grupo.id }.single()
        PerfilDocenteDto(
            nombre = docente.nombre,
            correo = docente.correo,
            jardin = grupo.jardin,
            grupo = grupo.nombre,
            totalNinos = ninos.delGrupo(grupo.id).size,
            avisos = AvisosDocenteDto(fila[Grupos.avisoPublicacion], fila[Grupos.avisoResumenDiario], fila[Grupos.avisoNinosSinJugar]),
        )
    }

    suspend fun actualizarPerfil(docenteId: String, datos: PerfilDocenteSolicitud): PerfilDocenteDto {
        val nombre = Validacion.texto(datos.nombre, "tu nombre", 120)
        val correo = Validacion.correo(datos.correo)
        val jardin = Validacion.texto(datos.jardin, "el jardín infantil", 120)
        val nombreGrupo = Validacion.texto(datos.grupo, "el grupo", 80)
        bd.transaccion {
            val grupo = grupoDe(docenteId)
            val otro = cuentas.usuarioPorCorreo(correo)
            if (otro != null && otro.id != docenteId) throw CuentasServicio.correoEnUso()
            cuentas.actualizarUsuario(docenteId, nombre, correo)
            Grupos.update({ Grupos.id eq grupo.id }) {
                it[Grupos.jardin] = jardin
                it[Grupos.nombre] = nombreGrupo
            }
        }
        return perfil(docenteId)
    }

    suspend fun guardarAvisos(docenteId: String, avisos: AvisosDocenteDto) = bd.transaccion {
        val grupo = grupoDe(docenteId)
        Grupos.update({ Grupos.id eq grupo.id }) {
            it[avisoPublicacion] = avisos.confirmarPublicacion
            it[avisoResumenDiario] = avisos.resumenDiario
            it[avisoNinosSinJugar] = avisos.ninosSinJugar
        }
    }

    /** Borra resultados, insignias y reportes del grupo; los niños y la biblioteca de retos se quedan. */
    suspend fun eliminarDatosGrupo(docenteId: String) = bd.transaccion {
        val grupo = grupoDe(docenteId)
        val ids = ninos.delGrupo(grupo.id).map { it.id }
        resultados.borrarDe(ids)
        retos.borrarIngresosDeNinos(ids)
        ids.forEach { ninos.guardarRacha(it, 0, 0) }
    }

    /** Semana escolar (lunes a viernes) de [lunes], comparada con la anterior (UI-26). */
    internal fun JdbcTransaction.reporteDelGrupo(grupoId: String, lunes: LocalDate): ReporteSemanalDto {
        val viernes = lunes.plusDays(4)
        val ninosGrupo = ninos.delGrupo(grupoId)
        val ids = ninosGrupo.map { it.id }
        val actual = Semana.de(retos.publicadosEntre(grupoId, lunes, viernes), resultados.partidasDe(ids, lunes, viernes), resultados.intentosDe(ids, lunes, viernes), ids.size)
        val previaLunes = lunes.minusWeeks(1)
        val previa = Semana.de(
            retos.publicadosEntre(grupoId, previaLunes, previaLunes.plusDays(4)),
            resultados.partidasDe(ids, previaLunes, previaLunes.plusDays(4)),
            resultados.intentosDe(ids, previaLunes, previaLunes.plusDays(4)),
            ids.size,
        )
        val insignias = resultados.insigniasDe(ids, lunes, viernes)
        val insigniasPrevia = resultados.insigniasDe(ids, previaLunes, previaLunes.plusDays(4))
        return ReporteSemanalDto(
            inicio = lunes.toString(),
            fin = viernes.toString(),
            completadosPct = actual.completadosPct,
            cambioCompletados = actual.completadosPct - previa.completadosPct,
            aciertosPct = actual.aciertosPct,
            cambioAciertos = actual.aciertosPct - previa.aciertosPct,
            retosPublicados = actual.retos.size,
            retosReutilizados = actual.retos.count { it.reutilizadoDe != null },
            insignias = insignias,
            cambioInsignias = insignias - insigniasPrevia,
            participacionPorDia = (0L..4L).map { lunes.plusDays(it) }.map { dia ->
                val jugaron = actual.partidas.filter { it.fecha == dia && it.terminada }.map { it.ninoId }.toSet().size
                ValorDiaDto(dia.toString(), if (ids.isEmpty()) 0 else jugaron * 100 / ids.size)
            },
            completadosPorNino = if (actual.partidas.isEmpty()) emptyList() else ninosGrupo.map { nino ->
                val completos = actual.partidas.filter { it.ninoId == nino.id && it.terminada }.map { it.retoId }.toSet().size
                ValorNinoDto(nino.nombre, if (actual.retos.isEmpty()) 0 else completos * 100 / actual.retos.size)
            },
        )
    }

    private class Semana(val retos: List<RetoGuardado>, val partidas: List<PartidaGuardada>, val completadosPct: Int, val aciertosPct: Int) {
        companion object {
            fun de(retos: List<RetoGuardado>, partidas: List<PartidaGuardada>, intentos: List<IntentoGuardado>, ninos: Int): Semana {
                val posibles = retos.size * ninos
                val completos = partidas.filter { it.terminada }.map { it.ninoId to it.retoId }.toSet().size
                return Semana(
                    retos = retos,
                    partidas = partidas,
                    completadosPct = if (posibles == 0) 0 else completos * 100 / posibles,
                    aciertosPct = porcentaje(intentos) ?: 0,
                )
            }
        }
    }

    private fun JdbcTransaction.grupoDe(docenteId: String): GrupoDocente =
        cuentas.grupoDeDocente(docenteId) ?: throw Prohibido("La cuenta no tiene grupo")

    private fun JdbcTransaction.ninoDelGrupo(docenteId: String, ninoId: String): NinoGuardado {
        val grupo = grupoDe(docenteId)
        return ninos.porId(ninoId)?.takeIf { it.grupoId == grupo.id }
            ?: throw NoEncontrado("nino_no_encontrado", "Ese niño no está en tu grupo")
    }

    private fun JdbcTransaction.retoDelGrupo(docenteId: String, retoId: String): RetoGuardado {
        val grupo = grupoDe(docenteId)
        return retos.porId(retoId)?.takeIf { it.grupoId == grupo.id }
            ?: throw NoEncontrado("reto_no_encontrado", "Ese reto ya no existe")
    }

    private fun JdbcTransaction.retoDto(reto: RetoGuardado, hoy: LocalDate) = RetoDocenteDto(
        id = reto.id,
        nombre = reto.nombre,
        categorias = reto.categorias.toList(),
        dificultad = reto.dificultad,
        fecha = reto.fecha?.toString(),
        estado = reto.estadoVisible(hoy),
        codigo = reto.codigo,
        aciertosPromedio = aciertosDeReto(reto.id),
        reutilizadoDe = reto.reutilizadoDe,
    )

    private fun JdbcTransaction.aciertosDeReto(retoId: String): Int? {
        val intentos = resultados.intentosDeReto(retoId)
        return if (intentos.isEmpty()) null else intentos.count { it } * 100 / intentos.size
    }

    private fun JdbcTransaction.codigoLibre(fecha: LocalDate): String {
        repeat(IntentosCodigo) {
            val codigo = codigos.codigoReto()
            if (!retos.codigoUsado(codigo, fecha)) return codigo
        }
        error("No hay códigos de reto libres para $fecha")
    }

    private fun JdbcTransaction.codigoVinculacionLibre(): String {
        repeat(IntentosCodigo) {
            val codigo = codigos.codigoVinculacion()
            if (!ninos.existeCodigo(codigo)) return codigo
        }
        error("No hay códigos de vinculación libres")
    }

    companion object {
        private const val SemanasDetalle = 4
        private const val MaxNinosPorGrupo = 40
        private const val IntentosCodigo = 50

        /** Avatares del piloto. Las ilustraciones llegan con el ilustrador; aquí solo viaja el nombre. */
        val Avatares = listOf(
            "Pez azul", "Tortuga verde", "Pulpo morado", "Cangrejo rojo",
            "Caballito amarillo", "Estrella naranja", "Delfín celeste", "Medusa rosada",
        )

        private fun avatarLibre(delGrupo: List<NinoGuardado>): String {
            val usados = delGrupo.groupingBy { it.avatar }.eachCount()
            return Avatares.minBy { usados[it] ?: 0 }
        }

        fun nombrePorDefecto(categorias: Set<CategoriaResiduo>): String {
            val nombres = CategoriaResiduo.entries.filter { it in categorias }.map {
                when (it) {
                    CategoriaResiduo.Aprovechable -> "aprovechables"
                    CategoriaResiduo.NoAprovechable -> "no aprovechables"
                    CategoriaResiduo.Organico -> "orgánicos"
                }
            }
            val lista = if (nombres.size <= 1) nombres.joinToString() else nombres.dropLast(1).joinToString(", ") + " y " + nombres.last()
            return "Reto de $lista"
        }

        fun inicioSemana(fecha: LocalDate): LocalDate = fecha.with(DayOfWeek.MONDAY)

        /** El reporte es de la última semana escolar terminada: la anterior a la actual. */
        fun ultimaSemanaCompleta(hoy: LocalDate): LocalDate = inicioSemana(hoy).minusWeeks(1)

        fun porcentaje(intentos: List<IntentoGuardado>): Int? =
            if (intentos.isEmpty()) null else intentos.count { it.correcto } * 100 / intentos.size

        fun porCategoria(intentos: List<IntentoGuardado>): Map<CategoriaResiduo, Int> =
            intentos.groupBy { CatalogoResiduos.porId(it.residuoId)!!.categoria }
                .mapValues { (_, lista) -> porcentaje(lista)!! }

        fun confusionFrecuente(intentos: List<IntentoGuardado>): ConfusionDto? =
            intentos.filter { !it.correcto }
                .groupingBy { it.residuoId to it.categoriaElegida }
                .eachCount()
                .maxByOrNull { it.value }
                ?.key
                ?.let { (residuoId, elegida) ->
                    val residuo = CatalogoResiduos.porId(residuoId)!!
                    ConfusionDto(residuo.nombre, residuo.categoria, elegida)
                }

        /** Cómo va un niño con el reto de hoy: terminado, a mitad o sin empezar. */
        fun estadoHoy(partidas: List<PartidaGuardada>, entro: Boolean, totalResiduos: Int): EstadoNinoHoyDto {
            val terminada = partidas.filter { it.terminada }.minByOrNull { it.errores }
            if (terminada != null) {
                return EstadoNinoHoyDto(
                    tipo = TipoEstadoNino.Completo,
                    minutos = ceil(terminada.tiempoMs / 60_000.0).roundToInt().coerceAtLeast(1),
                    aciertos = (totalResiduos - terminada.errores).coerceIn(0, totalResiduos),
                    total = totalResiduos,
                )
            }
            val enCurso = partidas.maxByOrNull { it.aciertos }
            return when {
                enCurso != null -> EstadoNinoHoyDto(TipoEstadoNino.EnCurso, residuoActual = (enCurso.aciertos + 1).coerceAtMost(totalResiduos))
                entro -> EstadoNinoHoyDto(TipoEstadoNino.EnCurso, residuoActual = 1)
                else -> EstadoNinoHoyDto(TipoEstadoNino.SinEmpezar)
            }
        }
    }
}
