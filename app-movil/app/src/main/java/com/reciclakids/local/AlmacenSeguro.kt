package com.reciclakids.local

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Textos pequeños y secretos (los tokens de sesión) guardados en el teléfono. */
interface AlmacenSeguro {
    fun leer(clave: String): String?

    fun guardar(clave: String, valor: String)

    fun borrar(clave: String)
}

/**
 * Guarda cada valor cifrado con AES-256-GCM. La llave vive en el Android Keystore y no se puede
 * sacar del teléfono; en las preferencias solo queda el texto cifrado (con la clave como dato
 * asociado, para que un valor no se pueda copiar a otra clave). Si la llave ya no sirve —por
 * ejemplo, tras restaurar un respaldo en otro teléfono— el valor se descarta y hay que volver a
 * iniciar sesión.
 */
class AlmacenCifrado(
    private val preferencias: SharedPreferences,
    private val llave: () -> SecretKey,
) : AlmacenSeguro {

    override fun leer(clave: String): String? {
        val guardado = preferencias.getString(clave, null) ?: return null
        return runCatching { descifrar(clave, guardado) }.getOrElse {
            borrar(clave)
            null
        }
    }

    override fun guardar(clave: String, valor: String) {
        // Si el Keystore falla, no queda nada en disco: la sesión dura lo que dure la app abierta.
        val cifrado = runCatching { cifrar(clave, valor) }.getOrNull()
        // commit y no apply: un token de refresco rotado que no llega al disco cierra la sesión.
        if (cifrado == null) borrar(clave) else preferencias.edit().putString(clave, cifrado).commit()
    }

    override fun borrar(clave: String) {
        preferencias.edit().remove(clave).commit()
    }

    private fun cifrar(clave: String, valor: String): String {
        val cifrador = Cipher.getInstance(Transformacion)
        cifrador.init(Cipher.ENCRYPT_MODE, llave())
        cifrador.updateAAD(clave.toByteArray())
        val iv = cifrador.iv
        return Base64.encodeToString(iv + cifrador.doFinal(valor.toByteArray()), Base64.NO_WRAP)
    }

    private fun descifrar(clave: String, guardado: String): String {
        val bytes = Base64.decode(guardado, Base64.NO_WRAP)
        val cifrador = Cipher.getInstance(Transformacion)
        cifrador.init(Cipher.DECRYPT_MODE, llave(), GCMParameterSpec(BitsEtiqueta, bytes, 0, BytesIv))
        cifrador.updateAAD(clave.toByteArray())
        return String(cifrador.doFinal(bytes, BytesIv, bytes.size - BytesIv))
    }

    companion object {
        private const val Transformacion = "AES/GCM/NoPadding"
        private const val BytesIv = 12
        private const val BitsEtiqueta = 128

        /** Archivo de preferencias: se excluye de los respaldos (res/xml/backup_rules.xml). */
        const val Archivo = "sesiones_cifradas"
        private const val AliasLlave = "reciclakids.sesiones"

        fun keystore(context: Context): AlmacenCifrado {
            val llave by lazy { llaveDelKeystore() }
            return AlmacenCifrado(context.getSharedPreferences(Archivo, Context.MODE_PRIVATE)) { llave }
        }

        private fun llaveDelKeystore(): SecretKey {
            val keystore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            (keystore.getKey(AliasLlave, null) as? SecretKey)?.let { return it }
            val generador = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            generador.init(
                KeyGenParameterSpec.Builder(AliasLlave, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
            return generador.generateKey()
        }
    }
}
