package com.reciclakids.ui.padres

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Normalizer
import java.time.LocalDate

/** «reciclakids-datos-salome-m-2026-11-05.json»: sin tildes ni espacios, para guardarlo en cualquier carpeta. */
fun nombreArchivoExportacion(nombreNino: String, fecha: LocalDate): String {
    val limpio = Normalizer.normalize(nombreNino, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
    return "reciclakids-datos-${limpio.ifEmpty { "nino" }}-$fecha.json"
}

/** Escribe [contenido] en el archivo que eligió el acudiente. `false` si no se pudo. */
suspend fun escribirArchivo(contexto: Context, destino: Uri, contenido: String): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        val salida = contexto.contentResolver.openOutputStream(destino) ?: return@runCatching false
        salida.use { it.write(contenido.toByteArray(Charsets.UTF_8)) }
        true
    }.getOrDefault(false)
}
