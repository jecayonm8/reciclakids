package com.reciclakids.ui.common

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import java.util.Locale

/**
 * Voz del Modo Niño: el juego se tiene que poder completar sin leer.
 *
 * PROVISIONAL: usa el motor de texto a voz del teléfono mientras llegan las locuciones grabadas
 * en español de Colombia. Si el teléfono no tiene voz en español, se queda en silencio.
 */
interface Locutor {
    /** Dice [texto] y corta lo que se estuviera diciendo. */
    fun decir(texto: String)
}

private object LocutorMudo : Locutor {
    override fun decir(texto: String) = Unit
}

private class LocutorTextoAVoz(context: Context) : Locutor {
    private var iniciado = false
    private val motor = TextToSpeech(context.applicationContext) { estado ->
        iniciado = estado == TextToSpeech.SUCCESS
    }

    // El idioma se elige la primera vez que hay algo que decir, con el motor ya iniciado.
    private val enEspanol by lazy {
        listOf(Locale.forLanguageTag("es-CO"), Locale.forLanguageTag("es")).any { idioma ->
            motor.setLanguage(idioma) >= TextToSpeech.LANG_AVAILABLE
        }
    }

    override fun decir(texto: String) {
        if (iniciado && enEspanol) motor.speak(texto, TextToSpeech.QUEUE_FLUSH, null, texto)
    }

    fun cerrar() {
        motor.stop()
        motor.shutdown()
    }
}

@Composable
fun rememberLocutor(): Locutor {
    if (LocalInspectionMode.current) return LocutorMudo
    val context = LocalContext.current
    val locutor = remember(context) { LocutorTextoAVoz(context) }
    DisposableEffect(locutor) { onDispose { locutor.cerrar() } }
    return locutor
}
