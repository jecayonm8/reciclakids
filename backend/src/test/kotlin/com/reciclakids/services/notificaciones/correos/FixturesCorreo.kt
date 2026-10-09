package com.reciclakids.services.notificaciones.correos

import kotlin.test.assertFalse

/**
 * Niños ficticios del mismo grupo (Martín es además el hermano de Salomé). El correo de cada uno
 * solo puede nombrarlo a él: nunca a otro niño (Ley 1581 de 2012).
 */
internal val GrupoFicticio = listOf("Salomé", "Martín", "Sofía", "Tomás", "Valentina", "Emilio")

/** El correo nunca compara al niño con el grupo ni con otros niños. */
internal fun assertSinComparacionesConElGrupo(texto: String) {
    val minusculas = texto.lowercase()
    listOf("compañer", "promedio", "ranking", "del grupo", "que el resto", "los demás").forEach {
        assertFalse(it in minusculas, "El correo compara con el grupo: «$it»")
    }
}
