package com.reciclakids.services.notificaciones.correos

/** Correo con el enlace para crear una contraseña nueva (UI-06). Vence a los 30 minutos. */
object PlantillaCorreoRecuperacion {

    private const val TextoBoton = "Crear contraseña nueva"

    fun renderizar(nombre: String, enlace: String): CorreoRenderizado {
        val saludo = "Hola, ${enUnaLinea(nombre)}"
        val explicacion = "Pediste crear una contraseña nueva para tu cuenta de ReciclaKids. " +
            "El enlace sirve una sola vez y vence en 30 minutos."
        val aviso = "Si no lo pediste, ignora este correo: tu contraseña sigue igual."
        val asunto = "Crea tu contraseña nueva de ReciclaKids"

        val cuerpo = columna(
            bloques = listOf(
                parrafo(saludo, tamano = 22, alto = 29, etiqueta = "h1"),
                parrafo(explicacion, tamano = 15, alto = 23, color = ColoresCorreo.TextoSecundario),
                botonPrincipal(TextoBoton, enlace),
                parrafo(aviso, tamano = 13, alto = 20, color = ColoresCorreo.TextoSecundario),
            ),
        )
        val html = documentoCorreo(
            asunto = asunto,
            textoOculto = explicacion,
            filas = listOf(encabezadoCorreo("Tu cuenta", fondo = ColoresCorreo.PrimarioOscuro), filaCuerpo(cuerpo)).joinToString("\n"),
        )
        val textoPlano = listOf(saludo, explicacion, "$TextoBoton: ${validarEnlace(enlace)}", aviso).joinToString("\n\n")
        return CorreoRenderizado(asunto, html, textoPlano)
    }
}
