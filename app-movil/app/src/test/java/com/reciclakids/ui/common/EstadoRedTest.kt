package com.reciclakids.ui.common

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Hallazgo 3 de la auditoría: en modo avión la pastilla seguía diciendo «Conectado». */
class EstadoRedTest {

    private val wifi = "wifi"
    private val datos = "datos"

    @Test
    fun alPonerModoAvionYaNoHayConexion() {
        val estado = EstadoRed()
        estado.capacidades(wifi, internet = true)
        assertTrue(estado.hayConexion)

        estado.perdida(wifi)

        assertFalse(estado.hayConexion)
    }

    @Test
    fun alPasarDelWifiALosDatosSigueConectado() {
        val estado = EstadoRed()
        estado.capacidades(wifi, internet = true)

        // El sistema avisa primero la red nueva y después, a veces, que se perdió la vieja.
        estado.capacidades(datos, internet = true)
        estado.perdida(wifi)

        assertTrue(estado.hayConexion)
    }

    @Test
    fun unWifiSinInternetNoCuentaComoConexion() {
        val estado = EstadoRed()
        estado.capacidades(wifi, internet = false)
        assertFalse(estado.hayConexion)

        estado.capacidades(wifi, internet = true)
        assertTrue(estado.hayConexion)
    }

    @Test
    fun sinNingunAvisoNoHayConexion() {
        assertFalse(EstadoRed().hayConexion)
    }
}
