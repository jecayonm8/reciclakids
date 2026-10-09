package com.reciclakids.network

import com.reciclakids.model.AvisosDocente
import com.reciclakids.model.BibliotecaDocente
import com.reciclakids.model.CategoriaResiduo
import com.reciclakids.model.CuentaAdulto
import com.reciclakids.model.DetalleNino
import com.reciclakids.model.EstadoNinoHoy
import com.reciclakids.model.EstadoReto
import com.reciclakids.model.GrupoHoy
import com.reciclakids.model.InsigniaNino
import com.reciclakids.model.NinoHoy
import com.reciclakids.model.PerfilDocente
import com.reciclakids.model.ReporteSemanal
import com.reciclakids.model.RetoDocente
import com.reciclakids.model.RetoBorrador
import com.reciclakids.model.TableroDocente
import com.reciclakids.model.ValorDia
import com.reciclakids.model.ValorNino
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.Normalizer
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Implementación PROVISIONAL mientras el backend no expone retos, resultados ni reportes: todo
 * vive en memoria y se pierde al cerrar la app, también el borrador que «queda en el
 * dispositivo» cuando falla la publicación (pendiente de Room). Con [conDatosDemo] arranca con
 * el grupo de los prototipos; sin él, la docente ve los estados vacíos de cada pantalla.
 */
class ServicioDocenteEnMemoria(
    cuenta: CuentaAdulto,
    private val latenciaMs: Long = 600,
    private val reloj: Clock = Clock.systemDefaultZone(),
    private val azar: Random = Random.Default,
    conDatosDemo: Boolean = true,
) : ServicioDocente {

    /** Mientras sea `true` todo responde como si no hubiera red: sirve para probar los estados de error. */
    @Volatile
    var sinConexion = false

    private val mutex = Mutex()
    private val nombre = cuenta.nombre
    private val correo = cuenta.correo
    private val jardin = cuenta.jardin.orEmpty()
    private val grupo = cuenta.grupo.orEmpty()
    private val ninos: List<NinoDemo> = if (conDatosDemo) DatosDemoDocente.ninos() else emptyList()
    private val retos: MutableList<RetoDocente> =
        if (conDatosDemo) DatosDemoDocente.retos(hoy()).toMutableList() else mutableListOf()

    /** Estado de cada niño por reto: id del reto → id del niño → estado. */
    private val progreso: MutableMap<String, Map<String, EstadoNinoHoy>> =
        if (conDatosDemo) mutableMapOf(DatosDemoDocente.IdRetoHoy to ninos.associate { it.id to it.estadoHoy }) else mutableMapOf()
    private var hayResultados = conDatosDemo
    private var avisos = AvisosDocente()
    private var codigoVinculacion = "${iniciales(grupo)}-2M91"
    private var siguienteId = 1

    override fun hoy(): LocalDate = LocalDate.now(reloj)

    override suspend fun tablero(): Respuesta<TableroDocente> = consultar {
        val hoy = hoy()
        val retoHoy = retoDeHoy(hoy)
        val completos = retoHoy?.let { progreso[it.id] }.orEmpty().values.filterIsInstance<EstadoNinoHoy.Completo>()
        TableroDocente(
            hoy = hoy,
            nombreDocente = nombre,
            grupo = grupo,
            retoHoy = retoHoy,
            completaron = completos.size,
            totalNinos = ninos.size,
            aciertosPromedio = completos.takeIf { it.isNotEmpty() }
                ?.let { lista -> (lista.sumOf { it.aciertos } * 100f / lista.sumOf { it.total }).roundToInt() },
        )
    }

    override suspend fun biblioteca(): Respuesta<BibliotecaDocente> = consultar {
        BibliotecaDocente(hoy(), retos.toList())
    }

    override suspend fun reto(id: String): Respuesta<RetoDocente?> = consultar { retos.firstOrNull { it.id == id } }

    override suspend fun publicar(borrador: RetoBorrador): ResultadoPublicacion {
        delay(latenciaMs)
        return mutex.withLock {
            val hoy = hoy()
            val base = RetoDocente(
                id = borrador.idExistente ?: "reto-${siguienteId++}",
                nombre = borrador.nombre ?: nombrePorDefecto(borrador.categorias),
                categorias = borrador.categorias,
                dificultad = borrador.dificultad,
                fecha = borrador.fecha,
                estado = EstadoReto.Borrador,
                reutilizadoDe = borrador.reutilizadoDe,
            )
            if (sinConexion) {
                guardar(base)
                ResultadoPublicacion.GuardadoComoBorrador(base)
            } else {
                val reto = if (borrador.fecha.isAfter(hoy)) {
                    base.copy(estado = EstadoReto.Programado)
                } else {
                    base.copy(fecha = hoy, estado = EstadoReto.Publicado, codigo = nuevoCodigo(hoy))
                }
                guardar(reto)
                ResultadoPublicacion.Publicado(reto)
            }
        }
    }

    // En memoria nadie más escribe resultados: basta con emitir una vez. El cliente del backend
    // emitirá de nuevo cada vez que entre un niño.
    override fun ninosQueEntraron(retoId: String): Flow<Int> = flow {
        delay(latenciaMs)
        if (!sinConexion) {
            emit(mutex.withLock { progreso[retoId].orEmpty().values.count { it != EstadoNinoHoy.SinEmpezar } })
        }
    }

    override suspend fun grupoHoy(): Respuesta<GrupoHoy> = consultar {
        val retoHoy = retoDeHoy(hoy())
        val estados = retoHoy?.let { progreso[it.id] }.orEmpty()
        GrupoHoy(
            nombreGrupo = grupo,
            retoHoy = retoHoy?.nombre,
            ninos = ninos.map { NinoHoy(it.id, it.nombre, estados[it.id] ?: EstadoNinoHoy.SinEmpezar) },
        )
    }

    override suspend fun detalleNino(id: String): Respuesta<DetalleNino?> = consultar {
        ninos.firstOrNull { it.id == id }?.let { nino ->
            DetalleNino(
                id = nino.id,
                nombre = nino.nombre,
                aciertosPorSemana = if (hayResultados) nino.aciertosPorSemana else emptyList(),
                aciertosPorCategoria = if (hayResultados) nino.aciertosPorCategoria else emptyMap(),
                confusion = nino.confusion.takeIf { hayResultados },
                insignias = DatosDemoDocente.Insignias.mapIndexed { i, insignia ->
                    InsigniaNino(insignia, ganada = hayResultados && i < nino.insigniasGanadas)
                },
            )
        }
    }

    override suspend fun reporteSemanal(): Respuesta<ReporteSemanal> = consultar {
        val inicio = hoy().with(DayOfWeek.MONDAY).minusWeeks(1)
        val fin = inicio.plusDays(4)
        if (!hayResultados || ninos.isEmpty()) {
            ReporteSemanal(inicio, fin, 0, 0, 0, 0, 0, 0, 0, 0, emptyList(), emptyList())
        } else {
            val completados = ninos.map { it.completadosSemana }.average().roundToInt()
            val aciertos = ninos.map { it.aciertosPorSemana.last() }.average().roundToInt()
            val aciertosPrevios = ninos.map { it.aciertosPorSemana[it.aciertosPorSemana.size - 2] }.average().roundToInt()
            ReporteSemanal(
                inicio = inicio,
                fin = fin,
                completadosPct = completados,
                cambioCompletados = completados - DatosDemoDocente.CompletadosSemanaPrevia,
                aciertosPct = aciertos,
                cambioAciertos = aciertos - aciertosPrevios,
                retosPublicados = DatosDemoDocente.RetosPublicadosSemana,
                retosReutilizados = DatosDemoDocente.RetosReutilizadosSemana,
                insignias = DatosDemoDocente.InsigniasSemana,
                cambioInsignias = DatosDemoDocente.InsigniasSemana - DatosDemoDocente.InsigniasSemanaPrevia,
                participacionPorDia = DatosDemoDocente.ParticipacionPorDia.mapIndexed { i, valor ->
                    ValorDia(inicio.plusDays(i.toLong()), valor)
                },
                completadosPorNino = ninos.map { ValorNino(it.nombre, it.completadosSemana) },
            )
        }
    }

    override suspend fun enviarReporteAcudientes(): Boolean = accion {}

    override suspend fun perfil(): Respuesta<PerfilDocente> = consultar {
        PerfilDocente(nombre, correo, jardin, grupo, ninos.size, codigoVinculacion, avisos)
    }

    override suspend fun guardarAvisos(avisos: AvisosDocente): Boolean = accion { this.avisos = avisos }

    override suspend fun regenerarCodigoVinculacion(): String? {
        delay(latenciaMs)
        if (sinConexion) return null
        return mutex.withLock {
            val sufijo = (1..4).map { CaracteresCodigo[azar.nextInt(CaracteresCodigo.length)] }.joinToString("")
            "${iniciales(grupo)}-$sufijo".also { codigoVinculacion = it }
        }
    }

    override suspend fun eliminarDatosGrupo(): Boolean = accion {
        hayResultados = false
        progreso.clear()
        retos.replaceAll { it.copy(aciertosPromedio = null) }
    }

    private suspend fun <T> consultar(bloque: () -> T): Respuesta<T> {
        delay(latenciaMs)
        if (sinConexion) return Respuesta.SinConexion
        return Respuesta.Ok(
            mutex.withLock {
                activarProgramados(hoy())
                bloque()
            }
        )
    }

    private suspend fun accion(bloque: () -> Unit): Boolean {
        delay(latenciaMs)
        if (sinConexion) return false
        mutex.withLock { bloque() }
        return true
    }

    /** Un reto programado se activa solo el día que le toca, con su código del día. */
    private fun activarProgramados(hoy: LocalDate) {
        retos.replaceAll { reto ->
            val fecha = reto.fecha
            if (reto.estado == EstadoReto.Programado && fecha != null && !fecha.isAfter(hoy)) {
                reto.copy(estado = EstadoReto.Publicado, codigo = nuevoCodigo(hoy))
            } else {
                reto
            }
        }
    }

    /** El más reciente publicado para hoy: es el que muestra el tablero. */
    private fun retoDeHoy(hoy: LocalDate): RetoDocente? = retos.firstOrNull { it.codigoActivo(hoy) != null }

    /** Reemplaza el reto con el mismo id o lo agrega al principio de la biblioteca. */
    private fun guardar(reto: RetoDocente) {
        val i = retos.indexOfFirst { it.id == reto.id }
        if (i >= 0) {
            retos.removeAt(i)
        }
        retos.add(0, reto)
    }

    /** Cuatro dígitos que no choquen con otro código activo hoy. */
    private fun nuevoCodigo(hoy: LocalDate): String {
        val activos = retos.mapNotNull { it.codigoActivo(hoy) }.toSet()
        return generateSequence { azar.nextInt(10_000).toString().padStart(4, '0') }.first { it !in activos }
    }

    private fun nombrePorDefecto(categorias: Set<CategoriaResiduo>): String =
        if (categorias.size == CategoriaResiduo.entries.size) {
            "Las tres canecas"
        } else {
            "Reto de " + categorias.sorted().joinToString(" y ") {
                when (it) {
                    CategoriaResiduo.Aprovechable -> "aprovechables"
                    CategoriaResiduo.NoAprovechable -> "no aprovechables"
                    CategoriaResiduo.Organico -> "orgánicos"
                }
            }
        }

    private companion object {
        /** Sin 0/O ni 1/I para que el acudiente no los confunda al escribirlos. */
        const val CaracteresCodigo = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

        /** «Jardín B» → «JB». */
        fun iniciales(grupo: String): String {
            val sinTildes = Normalizer.normalize(grupo, Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")
            val letras = sinTildes.split(' ').mapNotNull { palabra -> palabra.firstOrNull { it.isLetterOrDigit() } }
            return (letras.joinToString("") + "RK").take(2).uppercase()
        }
    }
}
