package com.reciclakids.services

import com.reciclakids.config.BaseDatos
import com.reciclakids.models.CatalogoResiduos
import com.reciclakids.models.HistorialNino
import com.reciclakids.models.Insignia
import com.reciclakids.models.dto.CodigoRetoDto
import com.reciclakids.models.dto.LotePartidasDto
import com.reciclakids.models.dto.NinoDto
import com.reciclakids.models.dto.PartidaDto
import com.reciclakids.models.dto.ProgresoNinoDto
import com.reciclakids.models.dto.RetoNinoDto
import com.reciclakids.models.dto.SesionNinoDto
import com.reciclakids.models.insigniasCumplidas
import com.reciclakids.plugins.DatosInvalidos
import com.reciclakids.plugins.ErrorApi
import com.reciclakids.plugins.NoEncontrado
import com.reciclakids.plugins.Prohibido
import com.reciclakids.plugins.SesionNino
import com.reciclakids.plugins.Tokens
import com.reciclakids.repositories.NinoGuardado
import com.reciclakids.repositories.NinosRepositorio
import com.reciclakids.repositories.NuevoIntento
import com.reciclakids.repositories.PartidaGuardada
import com.reciclakids.repositories.ResultadosRepositorio
import com.reciclakids.repositories.RetoGuardado
import com.reciclakids.repositories.RetosRepositorio
import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

/** Insignias que un niño acaba de ganar: Notificaciones avisa a sus acudientes (correo de logro). */
data class InsigniasGanadas(val ninoId: String, val insignias: List<Insignia>)

fun interface AvisoInsignias {
    suspend fun nuevas(ganadas: InsigniasGanadas)
}

/**
 * API de Retos para el niño y componente de Evaluación (C4 N3). El niño entra «tipo Kahoot»: el
 * código del reto del día más su nombre. Cada intento se vuelve a evaluar en el servidor.
 */
class NinoServicio(
    private val bd: BaseDatos,
    private val retos: RetosRepositorio,
    private val ninos: NinosRepositorio,
    private val resultados: ResultadosRepositorio,
    private val tokens: Tokens,
    private val reloj: Reloj,
    private val avisoInsignias: AvisoInsignias,
) {
    suspend fun buscarCodigo(codigo: String): CodigoRetoDto = bd.transaccion {
        val reto = retoDeHoy(codigo)
        CodigoRetoDto(retoDto(reto), ninos.delGrupo(reto.grupoId).map { it.aDto() })
    }

    suspend fun abrirSesion(codigo: String, ninoId: String): SesionNinoDto = bd.transaccion {
        val reto = retoDeHoy(codigo)
        val nino = ninos.porId(ninoId)?.takeIf { it.grupoId == reto.grupoId }
            ?: throw NoEncontrado("nino_no_encontrado", "Ese niño no está en el grupo del reto")
        retos.registrarIngreso(nino.id, reto.id, reloj.ahora())
        val token = tokens.accesoNino(nino.id, nino.grupoId)
        SesionNinoDto(token.token, token.segundos, nino.aDto(), retoDto(reto), ninos.control(nino.id).limiteMinutos)
    }

    suspend fun progreso(sesion: SesionNino): ProgresoNinoDto = bd.transaccion {
        val nino = ninoDeSesion(sesion)
        ProgresoNinoDto(
            retosCompletados = resultados.retosCompletados(nino.id),
            rachaEntreRetos = nino.rachaEntreRetos,
            mejorRachaEntreRetos = nino.mejorRachaEntreRetos,
            insignias = resultados.insignias(nino.id).keys.sortedBy { it.ordinal },
            limiteMinutosDiarios = ninos.control(nino.id).limiteMinutos,
        )
    }

    /**
     * Recibe lo que el teléfono jugó, aunque sea de días anteriores (ADR-04). Es idempotente: una
     * partida se reconoce por su id local y los intentos repetidos no se cuentan dos veces.
     */
    suspend fun recibirPartidas(sesion: SesionNino, partidas: List<PartidaDto>): LotePartidasDto {
        if (partidas.size > MaxPartidasPorLote) throw DatosInvalidos("Manda como máximo $MaxPartidasPorLote partidas por lote")
        val nuevas = mutableListOf<Insignia>()
        bd.transaccion {
            var nino = ninoDeSesion(sesion)
            for (partida in partidas) {
                nino = guardarPartida(nino, partida, nuevas)
            }
        }
        if (nuevas.isNotEmpty()) avisoInsignias.nuevas(InsigniasGanadas(sesion.ninoId, nuevas))
        return LotePartidasDto(partidas.size, nuevas)
    }

    private fun JdbcTransaction.guardarPartida(nino: NinoGuardado, dto: PartidaDto, nuevas: MutableList<Insignia>): NinoGuardado {
        if (dto.idLocal.isBlank() || dto.idLocal.length > 64) throw DatosInvalidos("Cada partida necesita su id local")
        val reto = retos.porId(dto.retoId)?.takeIf { it.grupoId == nino.grupoId && it.fecha != null }
            ?: throw Prohibido("Ese reto no es del grupo del niño")
        val ahora = reloj.ahora()
        val anterior = resultados.partidaPorIdLocal(nino.id, dto.idLocal)
        val partida = PartidaGuardada(
            id = anterior?.id ?: nuevoId(),
            ninoId = nino.id,
            retoId = reto.id,
            fecha = reto.fecha!!,
            // Una partida terminada no vuelve a abrirse aunque llegue un lote viejo.
            terminada = dto.terminada || anterior?.terminada == true,
            aciertos = dto.aciertos.coerceIn(0, Short.MAX_VALUE.toInt()),
            errores = dto.errores.coerceIn(0, Short.MAX_VALUE.toInt()),
            rachaMaxima = dto.rachaMaxima.coerceIn(0, Short.MAX_VALUE.toInt()),
            tiempoMs = dto.tiempoMs.coerceIn(0, TiempoMaximoPartidaMs),
        )
        if (anterior == null) resultados.crearPartida(partida, dto.idLocal, ahora) else resultados.actualizarPartida(partida, ahora)

        // Componente de Evaluación: el servidor decide si cada intento fue correcto.
        val yaGuardados = resultados.clavesIntentos(partida.id)
        val intentos = dto.intentos
            .map { intento ->
                val residuo = CatalogoResiduos.porId(intento.residuoId) ?: throw DatosInvalidos("Residuo desconocido: ${intento.residuoId}")
                NuevoIntento(intento.momentoMs, residuo.id, intento.categoriaElegida, residuo.categoria == intento.categoriaElegida)
            }
            .filter { (it.momentoMs to it.residuoId) !in yaGuardados }
            .distinctBy { it.momentoMs to it.residuoId }
            .sortedBy { it.momentoMs }
        resultados.agregarIntentos(partida.id, intentos)

        // Racha entre retos: un acierto la alarga y un error la corta (decisión del 2026-10-13).
        var racha = nino.rachaEntreRetos
        var mejor = nino.mejorRachaEntreRetos
        for (intento in intentos) {
            racha = if (intento.correcto) racha + 1 else 0
            mejor = maxOf(mejor, racha)
        }
        ninos.guardarRacha(nino.id, racha, mejor)
        val actualizado = nino.copy(rachaEntreRetos = racha, mejorRachaEntreRetos = mejor)

        if (partida.terminada && anterior?.terminada != true) {
            val historial = HistorialNino(
                retosCompletados = resultados.retosCompletados(nino.id),
                diasConReto = resultados.diasConReto(nino.id),
                mejorRachaEntreRetos = mejor,
            )
            val ya = resultados.insignias(nino.id).keys
            val ganadas = insigniasCumplidas(partida.errores, reto.dificultad, partida.tiempoMs, historial) - ya
            for (insignia in ganadas.sortedBy { it.ordinal }) {
                resultados.otorgar(nino.id, insignia, partida.fecha)
                nuevas += insignia
            }
        }
        return actualizado
    }

    private fun ninoDeSesion(sesion: SesionNino): NinoGuardado =
        ninos.porId(sesion.ninoId)?.takeIf { it.grupoId == sesion.grupoId }
            ?: throw Prohibido("El niño ya no está en este grupo")

    /** El reto publicado para hoy con ese código; 410 si era de un día que ya pasó y 404 en otro caso. */
    private fun retoDeHoy(codigo: String): RetoGuardado {
        val limpio = codigo.trim()
        if (!CodigoReto.matches(limpio)) throw codigoInvalido()
        val hoy = reloj.hoy()
        return retos.publicadoPorCodigo(limpio, hoy)
            ?: if (retos.codigoVencido(limpio, hoy)) throw codigoVencido() else throw codigoInvalido()
    }

    private fun retoDto(reto: RetoGuardado) = RetoNinoDto(
        id = reto.id,
        codigo = reto.codigo!!,
        fecha = reto.fecha.toString(),
        dificultad = reto.dificultad,
        categorias = reto.categorias.toList(),
        residuos = retos.residuos(reto.id),
    )

    companion object {
        private val CodigoReto = Regex("^\\d{4}$")
        private const val MaxPartidasPorLote = 100
        /** Una partida no dura más de una hora: el tiempo diario máximo es de 45 minutos. */
        private const val TiempoMaximoPartidaMs = 3_600_000L

        fun codigoInvalido() = NoEncontrado("codigo_invalido", "Ese código no es. ¡Probemos otra vez!")
        fun codigoVencido() = ErrorApi(HttpStatusCode.Gone, "codigo_vencido", "Ese código ya descansó. Pídele uno nuevo a la profe")
    }
}

fun NinoGuardado.aDto() = NinoDto(id, nombre, avatar)
