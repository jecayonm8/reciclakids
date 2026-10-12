package com.reciclakids.services.notificaciones.correos

/*
 * Piezas comunes de las plantillas de correo. Gmail y Outlook ignoran el CSS externo y las
 * hojas de estilo, así que todo se maqueta con tablas y estilos en línea, en una sola columna
 * de 360 px. Sin JavaScript, sin fuentes web y sin imágenes indispensables: el correo se
 * entiende completo con las imágenes bloqueadas. Los fondos son sólidos para que el modo oscuro
 * de los clientes los invierta sin romper el contraste.
 *
 * Estas funciones reciben texto sin escapar y lo escapan ellas mismas; solo los parámetros
 * `filas`, `bloques` y `contenido` llevan marcado ya armado (y escapado) por otra pieza.
 */

/** Colores del sistema de diseño (tema claro) que usan los correos. */
internal object ColoresCorreo {
    /** Primario: encabezado del correo de logro y nombre de la insignia. */
    const val Primario = "#00687F"

    /**
     * Primario oscuro: encabezado del reporte, botón y enlaces. El diseño pide contraste 7:1 en
     * el botón y el blanco sobre el primario se queda en 6,4:1; sobre este tono llega a 8,5:1.
     */
    const val PrimarioOscuro = "#00546A"
    const val Boton = PrimarioOscuro
    const val Enlace = PrimarioOscuro
    const val Terciario = "#2E6B45"
    const val ContenedorTerciario = "#B6F0C6"
    const val EnContenedorTerciario = "#0B2612"

    /** Barras por debajo del 80 %: ámbar de aviso, nunca rojo. */
    const val Advertencia = "#E8A33D"
    const val Superficie = "#FFFFFF"
    const val FondoCorreo = "#DCE7EB"

    /** Recuadros de cifras y pie. */
    const val Recuadro = "#E9F0F2"
    const val PistaBarra = "#DBE4E7"
    const val Texto = "#171C1E"
    const val TextoSecundario = "#3F484B"
    const val Blanco = "#FFFFFF"
    const val InsigniaBorde = "#FFC94D"
    const val InsigniaFondo = "#FFF3D6"
    const val InsigniaMedalla = "#FFE9B8"
    const val InsigniaRecuadro = "#FFF8E9"
    const val InsigniaRecuadroBorde = "#E8C877"
}

/** Roboto si el cliente la tiene; si no, las del sistema. */
internal const val FuenteTexto = "Roboto, Arial, Helvetica, sans-serif"

/** Baloo 2 casi nunca carga en los clientes de correo: el respaldo es Trebuchet MS. */
internal const val FuenteMarca = "'Baloo 2', 'Trebuchet MS', Arial, sans-serif"

/** Relleno tras el texto oculto para que el cliente no complete la vista previa con el cuerpo. */
private val RellenoTextoOculto = "&zwnj;&nbsp;".repeat(60)

/** Atributos de toda tabla de maquetación: sin bordes ni rellenos y sin rol de tabla de datos. */
internal const val AtributosTabla = """role="presentation" cellpadding="0" cellspacing="0" border="0""""

/**
 * Documento completo: `lang="es"`, texto oculto de vista previa (el que se lee en la bandeja
 * antes de abrir) y la tarjeta blanca de 360 px con las [filas] del correo.
 */
internal fun documentoCorreo(asunto: String, textoOculto: String, filas: String): String {
    val titulo = escaparHtml(asunto)
    return """<!DOCTYPE html>
<html lang="es" dir="ltr">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta name="x-apple-disable-message-reformatting">
<meta name="format-detection" content="telephone=no, date=no, address=no, email=no">
<meta name="color-scheme" content="light">
<meta name="supported-color-schemes" content="light">
<title>$titulo</title>
</head>
<body style="margin:0;padding:0;width:100%;background-color:${ColoresCorreo.FondoCorreo};">
<div style="display:none;max-height:0;max-width:0;overflow:hidden;opacity:0;mso-hide:all;font-size:1px;line-height:1px;color:${ColoresCorreo.FondoCorreo};">${escaparHtml(textoOculto)}$RellenoTextoOculto</div>
<div role="article" aria-roledescription="email" aria-label="$titulo" lang="es" style="background-color:${ColoresCorreo.FondoCorreo};">
<table $AtributosTabla width="100%" bgcolor="${ColoresCorreo.FondoCorreo}" style="width:100%;background-color:${ColoresCorreo.FondoCorreo};">
<tr>
<td align="center" style="padding:16px 8px;">
<!--[if mso]><table $AtributosTabla width="360" align="center"><tr><td><![endif]-->
<table $AtributosTabla width="360" align="center" bgcolor="${ColoresCorreo.Superficie}" style="width:100%;max-width:360px;margin:0 auto;background-color:${ColoresCorreo.Superficie};border-radius:12px;border-collapse:separate;">
$filas
</table>
<!--[if mso]></td></tr></table><![endif]-->
</td>
</tr>
</table>
</div>
</body>
</html>
"""
}

/** Franja superior con la marca y una línea de contexto («Jardín Gotitas · Jardín B»). */
internal fun encabezadoCorreo(subtitulo: String, fondo: String): String = """<tr>
<td bgcolor="$fondo" style="padding:20px;background-color:$fondo;border-radius:12px 12px 0 0;">
<p style="margin:0;font-family:$FuenteMarca;font-size:24px;line-height:30px;font-weight:800;color:${ColoresCorreo.Blanco};">ReciclaKids</p>
<p style="margin:6px 0 0 0;font-family:$FuenteTexto;font-size:14px;line-height:20px;font-weight:400;color:${ColoresCorreo.Blanco};">${escaparHtml(subtitulo)}</p>
</td>
</tr>"""

/** Fila del cuerpo, con el margen de 20 px del diseño. */
internal fun filaCuerpo(contenido: String): String = """<tr>
<td style="padding:20px;">
$contenido
</td>
</tr>"""

/** Apila bloques en una columna con [separacion] px entre ellos (el `gap` del diseño). */
internal fun columna(bloques: List<String>, separacion: Int = 16, alineacion: String = "left"): String =
    buildString {
        append("<table $AtributosTabla width=\"100%\" style=\"width:100%;\">\n")
        bloques.forEachIndexed { indice, bloque ->
            val arriba = if (indice == 0) 0 else separacion
            append("<tr><td align=\"$alineacion\" style=\"padding-top:${arriba}px;text-align:$alineacion;\">")
            append(bloque)
            append("</td></tr>\n")
        }
        append("</table>")
    }

/** Párrafo de texto con la tipografía de los modos adultos. */
internal fun parrafo(
    texto: String,
    tamano: Int,
    alto: Int,
    color: String = ColoresCorreo.Texto,
    peso: Int = 400,
    etiqueta: String = "p",
): String =
    "<$etiqueta style=\"margin:0;font-family:$FuenteTexto;font-size:${tamano}px;line-height:${alto}px;" +
        "font-weight:$peso;color:$color;\">${escaparHtml(texto)}</$etiqueta>"

/** Botón de ancho completo y 48 px de alto. Es la única llamada a la acción del correo. */
internal fun botonPrincipal(texto: String, enlace: String): String = """<table $AtributosTabla width="100%" style="width:100%;">
<tr>
<td align="center" height="48" bgcolor="${ColoresCorreo.Boton}" style="height:48px;background-color:${ColoresCorreo.Boton};border-radius:24px;">
<a href="${atributoEnlace(enlace)}" target="_blank" rel="noopener" style="display:block;height:48px;line-height:48px;font-family:$FuenteTexto;font-size:16px;font-weight:500;color:${ColoresCorreo.Blanco};text-decoration:none;text-align:center;border-radius:24px;">${escaparHtml(texto)}</a>
</td>
</tr>
</table>"""

/** Pie con el motivo del envío y los enlaces de preferencias, privacidad y baja. */
internal fun pieCorreo(aviso: String, enlaces: EnlacesPieCorreo): String {
    val estiloEnlace = "font-family:$FuenteTexto;font-size:12px;line-height:18px;color:${ColoresCorreo.Enlace};text-decoration:underline;"
    fun enlace(texto: String, url: String) =
        "<a href=\"${atributoEnlace(url)}\" target=\"_blank\" rel=\"noopener\" style=\"$estiloEnlace\">${escaparHtml(texto)}</a>"
    val enlacesHtml = listOf(
        enlace(TextosPie.Preferencias, enlaces.preferencias),
        enlace(TextosPie.Privacidad, enlaces.privacidad),
        enlace(TextosPie.Baja, enlaces.baja),
    ).joinToString(" &nbsp;·&nbsp; ")
    return """<tr>
<td bgcolor="${ColoresCorreo.Recuadro}" style="padding:16px 20px;background-color:${ColoresCorreo.Recuadro};border-radius:0 0 12px 12px;">
${parrafo(aviso, tamano = 12, alto = 18, color = ColoresCorreo.TextoSecundario)}
<p style="margin:8px 0 0 0;font-family:$FuenteTexto;font-size:12px;line-height:18px;color:${ColoresCorreo.TextoSecundario};">$enlacesHtml</p>
</td>
</tr>"""
}

/** El mismo pie en texto plano, con las URL completas. */
internal fun pieTextoPlano(aviso: String, enlaces: EnlacesPieCorreo): String = listOf(
    "------------------------------",
    aviso,
    "${TextosPie.Preferencias}: ${validarEnlace(enlaces.preferencias)}",
    "${TextosPie.Privacidad}: ${validarEnlace(enlaces.privacidad)}",
    "${TextosPie.Baja}: ${validarEnlace(enlaces.baja)}",
).joinToString("\n")

internal object TextosPie {
    const val Preferencias = "Preferencias de correo"
    const val Privacidad = "Privacidad"
    const val Baja = "Dejar de recibir"
}

/**
 * Círculo decorativo de color sólido (la medalla o la ilustración de la insignia). Va oculto a
 * los lectores de pantalla porque no aporta información: todo lo importante está en texto.
 */
internal fun circuloDecorativo(diametro: Int, fondo: String, borde: Int = 0, colorBorde: String = fondo, contenido: String = "&nbsp;"): String {
    val interior = diametro - 2 * borde
    val estiloBorde = if (borde > 0) "border:${borde}px solid $colorBorde;" else ""
    val tamanoLetra = if (contenido == "&nbsp;") 0 else interior * 45 / 100
    return """<table $AtributosTabla aria-hidden="true" align="center" style="margin:0 auto;"><tr><td width="$interior" height="$interior" align="center" valign="middle" bgcolor="$fondo" style="width:${interior}px;height:${interior}px;${estiloBorde}border-radius:${diametro / 2}px;background-color:$fondo;font-size:${tamanoLetra}px;line-height:${interior}px;text-align:center;">$contenido</td></tr></table>"""
}
