package com.reciclakids.ui.padres

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ExportacionDatosTest {

    @Test
    fun elArchivoSeLlamaConElNinoYLaFechaSinTildesNiEspacios() {
        val fecha = LocalDate.of(2026, 11, 5)
        assertEquals("reciclakids-datos-salome-m-2026-11-05.json", nombreArchivoExportacion("Salomé M.", fecha))
        assertEquals("reciclakids-datos-maria-jose-n-2026-11-05.json", nombreArchivoExportacion("María José Ñ.", fecha))
        assertEquals("reciclakids-datos-nino-2026-11-05.json", nombreArchivoExportacion("  ", fecha))
    }
}
