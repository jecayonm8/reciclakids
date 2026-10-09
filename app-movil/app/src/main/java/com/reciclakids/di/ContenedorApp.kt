package com.reciclakids.di

import android.content.Context
import com.reciclakids.local.BaseDatosReciclaKids
import com.reciclakids.local.RepositorioAjustes
import com.reciclakids.local.RepositorioJuego
import com.reciclakids.local.RepositorioProgreso
import com.reciclakids.local.RepositorioTiempo
import com.reciclakids.model.CuentaAdulto
import com.reciclakids.network.ControlesParentales
import com.reciclakids.network.ServicioAcceso
import com.reciclakids.network.ServicioAccesoEnMemoria
import com.reciclakids.network.ServicioDocente
import com.reciclakids.network.ServicioDocenteEnMemoria
import com.reciclakids.network.ServicioPadres
import com.reciclakids.network.ServicioPadresEnMemoria
import com.reciclakids.network.ServicioResultados
import com.reciclakids.network.ServicioResultadosEnMemoria
import com.reciclakids.network.ServicioRetos
import com.reciclakids.network.ServicioRetosEnMemoria
import com.reciclakids.network.SincronizacionWorker
import com.reciclakids.network.SincronizadorResultados

/**
 * Dependencias de la app con el ciclo de vida del proceso. Inyección manual: con pocas piezas,
 * un contenedor explícito se entiende mejor que un framework. Las pruebas arman las suyas.
 */
class ContenedorApp(
    val servicioAcceso: ServicioAcceso,
    val servicioRetos: ServicioRetos,
    val servicioResultados: ServicioResultados,
    val baseDatos: BaseDatosReciclaKids,
    /** El control parental de cada niño: lo cambia el Modo Padres (UI-33) y lo hace cumplir el Modo Niño (UI-19). */
    val controlesParentales: ControlesParentales = ControlesParentales(),
    /** Pide sincronizar los resultados en segundo plano. Las pruebas no programan nada. */
    val programarSincronizacion: () -> Unit = {},
    /** Servicio del Modo Docente para la cuenta que inicia sesión. */
    val crearServicioDocente: (CuentaAdulto) -> ServicioDocente = { ServicioDocenteEnMemoria(it) },
    /** Servicio del Modo Padres para la cuenta que inicia sesión. */
    val crearServicioPadres: (CuentaAdulto) -> ServicioPadres = { ServicioPadresEnMemoria(it, controlesParentales) },
) {
    val progreso by lazy { RepositorioProgreso(baseDatos.progresoDao()) }
    val juego by lazy { RepositorioJuego(baseDatos) }
    val ajustes by lazy { RepositorioAjustes(baseDatos.juegoDao()) }
    val tiempo by lazy { RepositorioTiempo(baseDatos) }
    val sincronizador by lazy { SincronizadorResultados(baseDatos, servicioResultados) }

    companion object {
        fun produccion(context: Context) = ContenedorApp(
            // PROVISIONALES mientras el backend no exponga autenticación, retos ni resultados.
            servicioAcceso = ServicioAccesoEnMemoria(),
            servicioRetos = ServicioRetosEnMemoria(),
            servicioResultados = ServicioResultadosEnMemoria(),
            baseDatos = BaseDatosReciclaKids.crear(context),
            programarSincronizacion = { SincronizacionWorker.programar(context.applicationContext) },
        )
    }
}
