package com.reciclakids.di

import android.content.Context
import com.reciclakids.BuildConfig
import com.reciclakids.local.AlmacenCifrado
import com.reciclakids.local.BaseDatosReciclaKids
import com.reciclakids.local.LimitesJuego
import com.reciclakids.local.LimitesJuegoLocales
import com.reciclakids.local.RepositorioAjustes
import com.reciclakids.local.RepositorioJuego
import com.reciclakids.local.RepositorioProgreso
import com.reciclakids.local.RepositorioTiempo
import com.reciclakids.model.CuentaAdulto
import com.reciclakids.network.ControlesParentales
import com.reciclakids.network.ServicioAcceso
import com.reciclakids.network.ServicioDocente
import com.reciclakids.network.ServicioDocenteEnMemoria
import com.reciclakids.network.ServicioPadres
import com.reciclakids.network.ServicioPadresEnMemoria
import com.reciclakids.network.ServicioResultados
import com.reciclakids.network.ServicioRetos
import com.reciclakids.network.SincronizacionWorker
import com.reciclakids.network.SincronizadorResultados
import com.reciclakids.network.api.ClienteApi
import com.reciclakids.network.api.ServicioAccesoApi
import com.reciclakids.network.api.ServicioDocenteApi
import com.reciclakids.network.api.ServicioPadresApi
import com.reciclakids.network.api.ServicioResultadosApi
import com.reciclakids.network.api.ServicioRetosApi
import com.reciclakids.network.api.SesionAdultoGuardada
import com.reciclakids.network.api.SesionesNino
import com.reciclakids.network.api.TokensNino
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Dependencias de la app con el ciclo de vida del proceso. Inyección manual: con pocas piezas,
 * un contenedor explícito se entiende mejor que un framework. Las pruebas arman las suyas con los
 * servicios en memoria; la app usa los del backend ([produccion]).
 */
class ContenedorApp(
    val servicioAcceso: ServicioAcceso,
    val servicioRetos: ServicioRetos,
    val servicioResultados: ServicioResultados,
    val baseDatos: BaseDatosReciclaKids,
    /** El control parental en memoria que comparten el Modo Padres de demostración y el Modo Niño. */
    val controlesParentales: ControlesParentales = ControlesParentales(),
    /** El límite diario que hace cumplir el Modo Niño (UI-19). Con el backend sale de la sesión del niño. */
    val limitesJuego: LimitesJuego = controlesParentales,
    /** Pide sincronizar los resultados en segundo plano. Las pruebas no programan nada. */
    val programarSincronizacion: () -> Unit = {},
    /** Servicio del Modo Docente para la cuenta que inicia sesión. */
    val crearServicioDocente: (CuentaAdulto) -> ServicioDocente = { ServicioDocenteEnMemoria(it) },
    /** Servicio del Modo Padres para la cuenta que inicia sesión. */
    val crearServicioPadres: (CuentaAdulto) -> ServicioPadres = { ServicioPadresEnMemoria(it, controlesParentales) },
    /** Emite cuando la sesión del adulto venció y no se pudo refrescar: la app vuelve al inicio de sesión. */
    val sesionVencida: Flow<Unit> = emptyFlow(),
    /** Para lo que no debe cancelarse al salir de una pantalla, como abrir la sesión del niño. */
    val alcance: CoroutineScope = MainScope(),
) {
    val progreso by lazy { RepositorioProgreso(baseDatos.progresoDao()) }
    val juego by lazy { RepositorioJuego(baseDatos) }
    val ajustes by lazy { RepositorioAjustes(baseDatos.juegoDao()) }
    val tiempo by lazy { RepositorioTiempo(baseDatos) }
    val sincronizador by lazy { SincronizadorResultados(baseDatos, servicioResultados) }

    companion object {
        /** La app contra el backend de BuildConfig.API_URL (ADR-07). */
        fun produccion(context: Context): ContenedorApp {
            val baseDatos = BaseDatosReciclaKids.crear(context)
            val almacen = AlmacenCifrado.keystore(context)
            val sesion = SesionAdultoGuardada(almacen)
            val cliente = ClienteApi(BuildConfig.API_URL, OkHttp.create(), sesion)
            val limites = LimitesJuegoLocales(baseDatos.juegoDao())
            val sesionesNino = SesionesNino(cliente, TokensNino(almacen), limites::guardar)
            val alcance = MainScope()
            return ContenedorApp(
                servicioAcceso = ServicioAccesoApi(cliente, sesion, alcance),
                servicioRetos = ServicioRetosApi(cliente, sesionesNino),
                servicioResultados = ServicioResultadosApi(cliente, sesionesNino),
                baseDatos = baseDatos,
                limitesJuego = limites,
                programarSincronizacion = { SincronizacionWorker.programar(context.applicationContext) },
                crearServicioDocente = { ServicioDocenteApi(cliente, sesion) },
                crearServicioPadres = { ServicioPadresApi(cliente, sesion, alGuardarLimite = limites::guardar) },
                sesionVencida = cliente.sesionVencida,
                alcance = alcance,
            )
        }
    }
}
