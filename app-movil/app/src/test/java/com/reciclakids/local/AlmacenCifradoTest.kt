package com.reciclakids.local

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.ConscryptMode
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * El Android Keystore no existe en Robolectric: la prueba usa una llave AES de la JVM con el
 * mismo cifrado (y el proveedor de la JVM, no Conscrypt). Lo que se comprueba es que en disco
 * nunca queda el token legible.
 */
@RunWith(RobolectricTestRunner::class)
@ConscryptMode(ConscryptMode.Mode.OFF)
class AlmacenCifradoTest {

    private val preferencias = RuntimeEnvironment.getApplication().getSharedPreferences("prueba-cifrado", Context.MODE_PRIVATE)

    private fun llave(): SecretKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

    @Test
    fun guardaCifradoYLeeElValorOriginal() {
        val llave = llave()
        val almacen = AlmacenCifrado(preferencias) { llave }

        almacen.guardar("sesion.adulto", """{"tokenRefresco":"secreto-123"}""")

        val enDisco = preferencias.getString("sesion.adulto", null).orEmpty()
        assertTrue(enDisco.isNotEmpty())
        assertFalse(enDisco.contains("secreto-123"))
        assertEquals("""{"tokenRefresco":"secreto-123"}""", AlmacenCifrado(preferencias) { llave }.leer("sesion.adulto"))
    }

    @Test
    fun conOtraLlaveElValorSeDescartaEnVezDeFallar() {
        val primera = llave()
        AlmacenCifrado(preferencias) { primera }.guardar("sesion.adulto", "secreto")

        // Como tras restaurar un respaldo en otro teléfono: la llave del Keystore ya no es la misma.
        val segunda = llave()
        val otro = AlmacenCifrado(preferencias) { segunda }

        assertNull(otro.leer("sesion.adulto"))
        assertNull(preferencias.getString("sesion.adulto", null))
    }

    @Test
    fun unValorNoSePuedeMoverAOtraClave() {
        val llave = llave()
        val almacen = AlmacenCifrado(preferencias) { llave }
        almacen.guardar("sesion.nino.n1", "token-de-salome")

        preferencias.edit().putString("sesion.nino.n2", preferencias.getString("sesion.nino.n1", null)).commit()

        assertNull(almacen.leer("sesion.nino.n2"))
        assertEquals("token-de-salome", almacen.leer("sesion.nino.n1"))
    }

    @Test
    fun borrar() {
        val llave = llave()
        val almacen = AlmacenCifrado(preferencias) { llave }
        almacen.guardar("clave", "valor")

        almacen.borrar("clave")

        assertNull(almacen.leer("clave"))
    }
}
