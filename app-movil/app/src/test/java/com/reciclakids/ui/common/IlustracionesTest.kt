package com.reciclakids.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.reciclakids.model.CatalogoResiduos
import com.reciclakids.model.Insignia
import com.reciclakids.ui.theme.ReciclaKidsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IlustracionesTest {

    @get:Rule
    val compose = createComposeRule()

    private val poses = TipoPersonaje.entries.flatMap { tipo ->
        AnimacionPersonaje.entries.map { animacion -> Ilustraciones.personaje(tipo, animacion) }
    }

    @Test
    fun cadaPersonajeTieneCuatroPosesDistintas() {
        assertEquals(TipoPersonaje.entries.size * 4, poses.toSet().size)
    }

    @Test
    fun cadaInsigniaTieneSuPropiaMedalla() {
        val medallas = Insignia.entries.map { Ilustraciones.insignia(it) }
        assertEquals(Insignia.entries.size, medallas.toSet().size)
    }

    @Test
    fun cadaResiduoDelCatalogoTieneArte() {
        CatalogoResiduos.todos.forEach { assertNotNull(it.id, Ilustraciones.residuo(it.id)) }
        assertNull(Ilustraciones.residuo("no-existe"))
    }

    @Test
    fun elAcuarioTieneUnaImagenPorNivelYAcotaLosExtremos() {
        val niveles = (1..4).map { Ilustraciones.acuario(it) }
        assertEquals(4, niveles.toSet().size)
        assertEquals(Ilustraciones.acuario(4), Ilustraciones.acuario(9))
    }

    @Test
    fun losDosAvataresSonDistintosYElNombreDecideSiempreElMismo() {
        assertEquals(2, setOf(Ilustraciones.avatarNino, Ilustraciones.avatarNina).size)
        assertEquals(Ilustraciones.avatarDe("Salomé M."), Ilustraciones.avatarDe("  salomé m. "))
    }

    @Test
    fun todasLasIlustracionesSeDecodificanYSeDibujan() {
        val todas = poses +
            Insignia.entries.map { Ilustraciones.insignia(it) } +
            CatalogoResiduos.todos.mapNotNull { Ilustraciones.residuo(it.id) } +
            (1..4).map { Ilustraciones.acuario(it) } +
            listOf(Ilustraciones.avatarNino, Ilustraciones.avatarNina)

        compose.setContent {
            ReciclaKidsTheme {
                Column { todas.forEach { IlustracionImagen(it, Modifier.size(24.dp)) } }
            }
        }
        compose.waitForIdle()
    }
}
