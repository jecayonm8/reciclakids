package com.reciclakids.services.notificaciones.correos

/**
 * UI-37 «Correo de reporte semanal». Se envía los viernes a las 5:00 p. m. y se lee en cinco
 * segundos: retos, aciertos y racha arriba, una frase interpretativa y los aciertos por caneca.
 * Se entiende completo aunque no se toque el botón, y solo contiene datos de un niño o niña.
 */
object PlantillaCorreoReporteSemanal {

    private const val TextoBoton = "Abrir el reporte completo"

    /** Desde este porcentaje la barra va en verde; por debajo, en ámbar de aviso. */
    internal const val UmbralAciertos = 80

    internal fun colorBarraAciertos(porcentaje: Int): String =
        if (porcentaje >= UmbralAciertos) ColoresCorreo.Terciario else ColoresCorreo.Advertencia

    /** «1 insignia nueva: Amiga tortuga» o «2 insignias nuevas: Amiga tortuga y Racha de 5». */
    internal fun encabezadoInsignias(cantidad: Int): String =
        if (cantidad == 1) "1 insignia nueva" else "$cantidad insignias nuevas"

    /**
     * «Tiempo de juego: 1 h 12 min en la semana, por debajo del límite de 20 min diarios que
     * definiste.» Compara el promedio por día jugado con el límite diario, sin redondear.
     */
    internal fun fraseTiempoDeJuego(tiempo: TiempoDeJuego): String {
        if (tiempo.diasConJuego == 0) return "Tiempo de juego: esta semana no jugó."
        val total = "Tiempo de juego: ${formatearDuracion(tiempo.minutosSemana)} en la semana"
        val limite = tiempo.limiteDiarioMinutos ?: return "$total."
        val topeSemana = limite * tiempo.diasConJuego
        val relacion = when {
            tiempo.minutosSemana < topeSemana -> "por debajo del"
            tiempo.minutosSemana == topeSemana -> "igual al"
            else -> "por encima del"
        }
        return "$total, $relacion límite de ${formatearDuracion(limite)} diarios que definiste."
    }

    fun renderizar(datos: DatosCorreoReporteSemanal): CorreoRenderizado {
        val nombre = enUnaLinea(datos.nombreNino)
        val semana = "Semana ${formatearRangoFechas(datos.inicioSemana, datos.finSemana)}"
        val asunto = "El resumen de la semana de $nombre"
        val aciertos = formatearPorcentaje(datos.porcentajeAciertos)
        val resumen = "${datos.retosCompletados} de ${datos.retosPublicados} retos · $aciertos de aciertos · " +
            "racha de ${datos.racha}"
        val filasCaneca = TipoCaneca.entries.mapNotNull { caneca ->
            datos.aciertosPorCaneca[caneca]?.let { porcentaje -> caneca to porcentaje }
        }
        val insignias = datos.insigniasSemana.map(::enUnaLinea)
        val mensaje = datos.mensajeInterpretativo?.let {
            MensajeInterpretativo(sinSaltos(it.antes), enUnaLinea(it.resaltado), sinSaltos(it.despues))
        }
        val tiempo = fraseTiempoDeJuego(datos.tiempoDeJuego)
        val aviso = "Enviado los viernes a las 5:00${EspacioFijo}p.${EspacioFijo}m. Solo contiene información de $nombre."

        val bloques = buildList {
            add(cifras(datos, aciertos))
            mensaje?.let { add(recuadroMensaje(it)) }
            if (filasCaneca.isNotEmpty()) add(aciertosPorCaneca(filasCaneca))
            if (insignias.isNotEmpty()) add(recuadroInsignias(insignias))
            add(botonPrincipal(TextoBoton, datos.enlaceReporte))
            add(parrafo(tiempo, tamano = 13, alto = 20, color = ColoresCorreo.TextoSecundario))
        }

        val html = documentoCorreo(
            asunto = asunto,
            textoOculto = resumen,
            filas = listOf(
                encabezadoCorreo(semana, fondo = ColoresCorreo.PrimarioOscuro),
                filaCuerpo(columna(bloques)),
                pieCorreo(aviso, datos.enlacesPie),
            ).joinToString("\n"),
        )

        val textoPlano = buildList {
            add("ReciclaKids · $semana")
            add(asunto)
            add(
                listOf(
                    "Retos: ${datos.retosCompletados} de ${datos.retosPublicados}",
                    "Aciertos: $aciertos",
                    "Racha: ${datos.racha}",
                ).joinToString("\n"),
            )
            mensaje?.let { add(it.textoCompleto) }
            if (filasCaneca.isNotEmpty()) {
                add(
                    (listOf("Aciertos por caneca:") + filasCaneca.map { (caneca, porcentaje) ->
                        "- ${caneca.categoria} (${caneca.nombre}): ${formatearPorcentaje(porcentaje)}"
                    }).joinToString("\n"),
                )
            }
            if (insignias.isNotEmpty()) add("${encabezadoInsignias(insignias.size)}: ${unirConY(insignias)}.")
            add("$TextoBoton: ${validarEnlace(datos.enlaceReporte)}")
            add(tiempo)
            add(pieTextoPlano(aviso, datos.enlacesPie))
        }.joinToString("\n\n")

        return CorreoRenderizado(asunto = asunto, html = html, textoPlano = textoPlano)
    }

    /** Tres recuadros: retos, aciertos y racha. */
    private fun cifras(datos: DatosCorreoReporteSemanal, aciertos: String): String {
        val colorAciertos =
            if (datos.porcentajeAciertos >= UmbralAciertos) ColoresCorreo.Terciario else ColoresCorreo.Texto
        fun recuadro(valor: String, etiqueta: String, color: String = ColoresCorreo.Texto) =
            """<td width="100" valign="top" bgcolor="${ColoresCorreo.Recuadro}" style="width:100px;padding:12px 10px;background-color:${ColoresCorreo.Recuadro};border-radius:12px;">""" +
                "<p style=\"margin:0;font-family:$FuenteTexto;font-size:28px;line-height:31px;font-weight:400;color:$color;white-space:nowrap;\">${escaparHtml(valor)}</p>" +
                "<p style=\"margin:2px 0 0 0;font-family:$FuenteTexto;font-size:13px;line-height:18px;font-weight:400;color:${ColoresCorreo.TextoSecundario};\">${escaparHtml(etiqueta)}</p>" +
                "</td>"
        val hueco = "<td width=\"10\" style=\"width:10px;font-size:0;line-height:0;\">&nbsp;</td>"
        return "<table $AtributosTabla width=\"100%\" style=\"width:100%;\"><tr>" +
            recuadro("${datos.retosCompletados}/${datos.retosPublicados}", "retos") + hueco +
            recuadro(aciertos, "aciertos", colorAciertos) + hueco +
            recuadro("${datos.racha}", "racha") +
            "</tr></table>"
    }

    /** Recuadro verde con la frase interpretativa y su parte resaltada en negrilla. */
    private fun recuadroMensaje(mensaje: MensajeInterpretativo): String =
        "<table $AtributosTabla width=\"100%\" style=\"width:100%;\"><tr>" +
            "<td bgcolor=\"${ColoresCorreo.ContenedorTerciario}\" style=\"padding:14px;background-color:${ColoresCorreo.ContenedorTerciario};" +
            "border-radius:12px;font-family:$FuenteTexto;font-size:15px;line-height:22px;color:${ColoresCorreo.EnContenedorTerciario};\">" +
            escaparHtml(mensaje.antes) +
            "<strong style=\"font-weight:700;\">${escaparHtml(mensaje.resaltado)}</strong>" +
            escaparHtml(mensaje.despues) +
            "</td></tr></table>"

    /**
     * Una fila por caneca con datos. Las barras se leen sin color: cada fila lleva la muestra de
     * la caneca, el nombre de la categoría y el porcentaje; la barra es solo un apoyo visual.
     */
    private fun aciertosPorCaneca(filas: List<Pair<TipoCaneca, Int>>): String = buildString {
        append("<table $AtributosTabla width=\"100%\" style=\"width:100%;\">")
        append("<tr><td>")
        append(parrafo("Aciertos por caneca", tamano = 15, alto = 20, peso = 500, etiqueta = "h2"))
        append("</td></tr>")
        filas.forEach { (caneca, porcentaje) ->
            append("<tr><td style=\"padding-top:10px;\">")
            append(filaCaneca(caneca, porcentaje))
            append("</td></tr>")
            append("<tr><td style=\"padding-top:10px;\">")
            append(barra(porcentaje))
            append("</td></tr>")
        }
        append("</table>")
    }

    private fun filaCaneca(caneca: TipoCaneca, porcentaje: Int): String {
        val muestra = "<table $AtributosTabla aria-hidden=\"true\"><tr>" +
            "<td width=\"14\" height=\"14\" bgcolor=\"${caneca.color}\" style=\"width:14px;height:14px;" +
            "background-color:${caneca.color};border:2px solid ${caneca.borde};border-radius:5px;font-size:0;line-height:0;\">&nbsp;</td>" +
            "</tr></table>"
        val estiloTexto = "font-family:$FuenteTexto;font-size:14px;line-height:20px;color:${ColoresCorreo.Texto};"
        return "<table $AtributosTabla width=\"100%\" style=\"width:100%;\"><tr>" +
            "<td width=\"28\" valign=\"middle\" style=\"width:28px;\">$muestra</td>" +
            "<td valign=\"middle\" style=\"${estiloTexto}font-weight:400;\">${escaparHtml(caneca.categoria)}</td>" +
            "<td valign=\"middle\" align=\"right\" style=\"${estiloTexto}font-weight:500;white-space:nowrap;\">" +
            "${escaparHtml(formatearPorcentaje(porcentaje))}</td>" +
            "</tr></table>"
    }

    /** Barra de 10 px: verde desde el 80 %, ámbar por debajo. Oculta a lectores de pantalla. */
    private fun barra(porcentaje: Int): String {
        val color = colorBarraAciertos(porcentaje)
        val celdaVacia = "font-size:0;line-height:0;"
        val relleno = if (porcentaje > 0) {
            "<td width=\"$porcentaje%\" height=\"10\" bgcolor=\"$color\" style=\"width:$porcentaje%;height:10px;" +
                "background-color:$color;border-radius:5px;$celdaVacia\">&nbsp;</td>"
        } else {
            ""
        }
        val resto = if (porcentaje < 100) {
            "<td width=\"${100 - porcentaje}%\" height=\"10\" style=\"width:${100 - porcentaje}%;height:10px;$celdaVacia\">&nbsp;</td>"
        } else {
            ""
        }
        return "<table $AtributosTabla aria-hidden=\"true\" width=\"100%\" bgcolor=\"${ColoresCorreo.PistaBarra}\" " +
            "style=\"width:100%;background-color:${ColoresCorreo.PistaBarra};border-radius:5px;\"><tr>$relleno$resto</tr></table>"
    }

    /** Recuadro dorado con las insignias de la semana, cada nombre en negrilla. */
    private fun recuadroInsignias(insignias: List<String>): String {
        val nombres = unirConY(insignias.map { "<strong style=\"font-weight:700;\">${escaparHtml(it)}</strong>" })
        val medalla = circuloDecorativo(diametro = 44, fondo = ColoresCorreo.InsigniaMedalla)
        return "<table $AtributosTabla width=\"100%\" bgcolor=\"${ColoresCorreo.InsigniaRecuadro}\" style=\"width:100%;" +
            "background-color:${ColoresCorreo.InsigniaRecuadro};border:1px solid ${ColoresCorreo.InsigniaRecuadroBorde};border-radius:12px;\"><tr>" +
            "<td width=\"44\" valign=\"middle\" style=\"width:44px;padding:12px 0 12px 12px;\">$medalla</td>" +
            "<td valign=\"middle\" style=\"padding:12px;font-family:$FuenteTexto;font-size:14px;line-height:20px;color:${ColoresCorreo.Texto};\">" +
            "${escaparHtml(encabezadoInsignias(insignias.size))}: $nombres</td>" +
            "</tr></table>"
    }
}
