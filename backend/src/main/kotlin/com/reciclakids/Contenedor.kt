package com.reciclakids

import com.reciclakids.config.BaseDatos
import com.reciclakids.config.Configuracion
import com.reciclakids.plugins.Tokens
import com.reciclakids.repositories.CuentasRepositorio
import com.reciclakids.repositories.NinosRepositorio
import com.reciclakids.repositories.ResultadosRepositorio
import com.reciclakids.repositories.RetosRepositorio
import com.reciclakids.services.CuentasServicio
import com.reciclakids.services.DocenteServicio
import com.reciclakids.services.GeneradorCodigos
import com.reciclakids.services.NinoServicio
import com.reciclakids.services.NotificacionesServicio
import com.reciclakids.services.PadresServicio
import com.reciclakids.services.Reloj
import com.reciclakids.services.notificaciones.correos.EnviadorCorreo
import com.reciclakids.services.notificaciones.correos.EnviadorCorreoEnMemoria
import com.reciclakids.services.notificaciones.correos.EnviadorCorreoSes
import java.time.Clock

/** Dependencias del backend, armadas a mano: pocas piezas, sin framework de inyección. */
class Contenedor(
    val configuracion: Configuracion,
    val bd: BaseDatos,
    reloj: Clock = Clock.system(configuracion.zona),
    val enviador: EnviadorCorreo = enviadorPara(configuracion),
) {
    val reloj = Reloj(configuracion.zona, reloj)
    val tokens = Tokens(configuracion.jwt) { reloj.instant() }
    private val codigos = GeneradorCodigos()

    private val cuentasRepositorio = CuentasRepositorio()
    private val ninosRepositorio = NinosRepositorio()
    private val retosRepositorio = RetosRepositorio()
    private val resultadosRepositorio = ResultadosRepositorio()

    val notificaciones = NotificacionesServicio(
        bd, cuentasRepositorio, ninosRepositorio, retosRepositorio, resultadosRepositorio,
        enviador, tokens, this.reloj, configuracion.urlPublica,
    )

    val cuentas = CuentasServicio(
        bd, cuentasRepositorio, ninosRepositorio, tokens, this.reloj, notificaciones, configuracion.urlPublica,
    )

    val nino = NinoServicio(bd, retosRepositorio, ninosRepositorio, resultadosRepositorio, tokens, this.reloj, notificaciones)

    val docente = DocenteServicio(bd, cuentasRepositorio, ninosRepositorio, retosRepositorio, resultadosRepositorio, codigos, this.reloj)

    val padres = PadresServicio(bd, cuentasRepositorio, ninosRepositorio, retosRepositorio, resultadosRepositorio, this.reloj)

    companion object {
        fun enviadorPara(configuracion: Configuracion): EnviadorCorreo = when (configuracion.correos.proveedor) {
            "ses" -> EnviadorCorreoSes(configuracion.correos.remitente, configuracion.correos.regionSes)
            else -> EnviadorCorreoEnMemoria()
        }
    }
}
