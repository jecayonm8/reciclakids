package com.reciclakids.services.notificaciones.correos

/**
 * UI-36 «Correo de logro». Se entiende completo sin cargar imágenes y sin abrir la app: la
 * insignia es un círculo decorativo y todo lo importante va en texto. Un solo botón, «Ver su
 * progreso», que abre el detalle de ese hijo.
 *
 * La regla de envío (máximo un correo de logro al día y solo si el acudiente lo tiene activado)
 * es del componente de Notificaciones; esta plantilla solo arma el correo.
 */
object PlantillaCorreoLogro {

    private const val TextoBoton = "Ver su progreso"

    /**
     * Emoji del asunto para cada insignia del piloto que tiene uno claro. Las demás van sin emoji.
     * Cuando exista el modelo `Insignia` en el backend, el emoji debería vivir allí.
     */
    private val EmojiPorInsignia = mapOf(
        "amiga tortuga" to "🐢",
        "rápido como pez" to "🐟",
        "pulpo ordenado" to "🐙",
        "agua cristalina" to "💧",
        "guardián del mar" to "🌊",
    )

    internal fun emojiDeInsignia(nombreInsignia: String): String? =
        EmojiPorInsignia[enUnaLinea(nombreInsignia).lowercase()]

    /** «Van 4 insignias de 8.», «Va 1 insignia de 8.» o «Ya completó las 8 insignias.». */
    internal fun conteoInsignias(ganadas: Int, totales: Int): String = when {
        ganadas == totales && totales > 1 -> "Ya completó las $totales insignias."
        ganadas == 1 -> "Va 1 insignia de $totales."
        else -> "Van $ganadas insignias de $totales."
    }

    fun renderizar(datos: DatosCorreoLogro): CorreoRenderizado {
        val nombre = enUnaLinea(datos.nombreNino)
        val insignia = enUnaLinea(datos.nombreInsignia)
        val emoji = emojiDeInsignia(insignia)
        val jardinYGrupo = "${enUnaLinea(datos.jardin)} · ${enUnaLinea(datos.grupo)}"
        val titulo = "$nombre ganó una insignia"
        val detalle = conPuntoFinal(enUnaLinea(datos.descripcionLogro)) + " " +
            conteoInsignias(datos.insigniasGanadas, datos.insigniasTotales)
        val aviso = "Recibes este correo porque autorizaste el seguimiento de $nombre en ReciclaKids. " +
            "Solo enviamos un correo de logro al día."

        val asunto = "$nombre ganó la insignia «$insignia»" + (emoji?.let { " $it" } ?: "")

        val cuerpo = columna(
            alineacion = "center",
            bloques = listOf(
                circuloDecorativo(
                    diametro = 120,
                    fondo = ColoresCorreo.InsigniaFondo,
                    borde = 6,
                    colorBorde = ColoresCorreo.InsigniaBorde,
                    contenido = emoji?.let(::escaparHtml) ?: "&nbsp;",
                ),
                parrafo(titulo, tamano = 22, alto = 29, etiqueta = "h1"),
                parrafo(insignia, tamano = 18, alto = 24, color = ColoresCorreo.Primario, peso = 500),
                parrafo(detalle, tamano = 15, alto = 23, color = ColoresCorreo.TextoSecundario),
                botonPrincipal(TextoBoton, datos.enlaceProgreso),
            ),
        )

        val html = documentoCorreo(
            asunto = asunto,
            textoOculto = detalle,
            filas = listOf(
                encabezadoCorreo(jardinYGrupo, fondo = ColoresCorreo.Primario),
                filaCuerpo(cuerpo),
                pieCorreo(aviso, datos.enlacesPie),
            ).joinToString("\n"),
        )

        val textoPlano = listOf(
            "ReciclaKids · $jardinYGrupo",
            "$titulo: «$insignia»",
            detalle,
            "$TextoBoton: ${validarEnlace(datos.enlaceProgreso)}",
            pieTextoPlano(aviso, datos.enlacesPie),
        ).joinToString("\n\n")

        return CorreoRenderizado(asunto = asunto, html = html, textoPlano = textoPlano)
    }
}
