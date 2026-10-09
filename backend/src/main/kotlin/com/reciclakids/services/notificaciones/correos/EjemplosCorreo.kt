package com.reciclakids.services.notificaciones.correos

import java.time.LocalDate

/**
 * Datos ficticios del diseño (Salomé M., Jardín Gotitas · Jardín B) para la vista previa de
 * las plantillas. No corresponden a ningún niño real y nunca se envían.
 */
object EjemplosCorreo {

    private const val Base = "https://reciclakids.example/padres"

    val enlacesPie = EnlacesPieCorreo(
        preferencias = "$Base/preferencias-correo",
        privacidad = "$Base/privacidad",
        baja = "$Base/dejar-de-recibir",
    )

    val logro = DatosCorreoLogro(
        nombreNino = "Salomé",
        jardin = "Jardín Gotitas",
        grupo = "Jardín B",
        nombreInsignia = "Amiga tortuga",
        descripcionLogro = "Terminó el reto de hoy sin ninguna ayuda y dejó su acuario más limpio.",
        insigniasGanadas = 4,
        insigniasTotales = 8,
        enlaceProgreso = "$Base/hijos/demo/progreso",
        enlacesPie = enlacesPie,
    )

    val reporteSemanal = DatosCorreoReporteSemanal(
        nombreNino = "Salomé",
        inicioSemana = LocalDate.of(2025, 9, 15),
        finSemana = LocalDate.of(2025, 9, 19),
        retosCompletados = 4,
        retosPublicados = 5,
        porcentajeAciertos = 88,
        racha = 5,
        mensajeInterpretativo = MensajeInterpretativo.mejoraEn(TipoCaneca.Verde, desde = 74, hasta = 92),
        aciertosPorCaneca = mapOf(
            TipoCaneca.Blanca to 94,
            TipoCaneca.Verde to 92,
            TipoCaneca.Negra to 61,
        ),
        insigniasSemana = listOf("Amiga tortuga", "Racha de 5"),
        tiempoDeJuego = TiempoDeJuego(minutosSemana = 72, diasConJuego = 4, limiteDiarioMinutos = 20),
        enlaceReporte = "$Base/hijos/demo/reporte-semanal/2025-09-15",
        enlacesPie = enlacesPie,
    )
}
