package com.reciclakids.integracion

import com.reciclakids.models.CatalogoResiduos
import com.reciclakids.models.CategoriaResiduo
import com.reciclakids.models.Dificultad
import com.reciclakids.models.EstadoReto
import com.reciclakids.models.Insignia
import com.reciclakids.models.dto.AgregarNinoSolicitud
import com.reciclakids.models.dto.CodigoRetoDto
import com.reciclakids.models.dto.ControlParentalDto
import com.reciclakids.models.dto.CuentaDto
import com.reciclakids.models.dto.DetalleNinoDto
import com.reciclakids.models.dto.GrupoHoyDto
import com.reciclakids.models.dto.HijoDto
import com.reciclakids.models.dto.HistorialCorreosDto
import com.reciclakids.models.dto.IngresosDto
import com.reciclakids.models.dto.InicioPadresDto
import com.reciclakids.models.dto.InicioSesionSolicitud
import com.reciclakids.models.dto.IntentoDto
import com.reciclakids.models.dto.LotePartidasDto
import com.reciclakids.models.dto.LotePartidasSolicitud
import com.reciclakids.models.dto.LogrosHijoDto
import com.reciclakids.models.dto.NinoAgregadoDto
import com.reciclakids.models.dto.PartidaDto
import com.reciclakids.models.dto.PrivacidadHijoDto
import com.reciclakids.models.dto.ProgresoNinoDto
import com.reciclakids.models.dto.RecuperacionSolicitud
import com.reciclakids.models.dto.RefrescoSolicitud
import com.reciclakids.models.dto.RegistroAcudienteSolicitud
import com.reciclakids.models.dto.RegistroDocenteSolicitud
import com.reciclakids.models.dto.ReporteSemanalDto
import com.reciclakids.models.dto.RetoBorradorSolicitud
import com.reciclakids.models.dto.RetoDocenteDto
import com.reciclakids.models.dto.RetoNinoDto
import com.reciclakids.models.dto.RolAdulto
import com.reciclakids.models.dto.SesionDto
import com.reciclakids.models.dto.SesionNinoDto
import com.reciclakids.models.dto.SesionNinoSolicitud
import com.reciclakids.models.dto.TableroDto
import com.reciclakids.models.dto.TipoEstadoNino
import com.reciclakids.models.dto.VinculacionDto
import com.reciclakids.models.dto.VincularSolicitud
import com.reciclakids.plugins.CuerpoError
import com.reciclakids.routes.ResultadoTareaDto
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.parameters
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ApiTest {

    // --- Ayudas para armar cada escenario ---

    private suspend fun ApiDePrueba.registrarDocente(correo: String = "laura@jardin.co"): String {
        val respuesta = post("/api/v1/auth/registro/docente", RegistroDocenteSolicitud("Laura Gómez", correo, "Jardín Gotitas", "Transición A", "Clave1234"))
        assertEquals(HttpStatusCode.Created, respuesta.status)
        return iniciarSesion(correo).tokenAcceso
    }

    private suspend fun ApiDePrueba.iniciarSesion(correo: String, contrasena: String = "Clave1234"): SesionDto {
        val respuesta = post("/api/v1/auth/sesion", InicioSesionSolicitud(correo, contrasena))
        assertEquals(HttpStatusCode.OK, respuesta.status, respuesta.bodyAsText())
        return respuesta.body()
    }

    private suspend fun ApiDePrueba.agregarNino(docente: String, nombre: String, inicial: String): NinoAgregadoDto {
        val respuesta = post("/api/v1/docente/ninos", AgregarNinoSolicitud(nombre, inicial, edad = 5), docente)
        assertEquals(HttpStatusCode.Created, respuesta.status, respuesta.bodyAsText())
        return respuesta.body()
    }

    private suspend fun ApiDePrueba.publicar(
        docente: String,
        dificultad: Dificultad = Dificultad.Facil,
        fecha: String = reloj.hoy().toString(),
        idExistente: String? = null,
    ): RetoDocenteDto {
        val borrador = RetoBorradorSolicitud(listOf(CategoriaResiduo.Aprovechable, CategoriaResiduo.Organico), dificultad, fecha, idExistente)
        val respuesta = post("/api/v1/docente/retos", borrador, docente)
        assertEquals(HttpStatusCode.Created, respuesta.status, respuesta.bodyAsText())
        return respuesta.body()
    }

    private suspend fun ApiDePrueba.entrarComoNino(codigo: String, ninoId: String): SesionNinoDto {
        val respuesta = post("/api/v1/nino/sesiones", SesionNinoSolicitud(codigo, ninoId))
        assertEquals(HttpStatusCode.OK, respuesta.status, respuesta.bodyAsText())
        return respuesta.body()
    }

    private suspend fun ApiDePrueba.registrarAcudiente(codigo: String, correo: String = "marta@casa.co"): String {
        val respuesta = post("/api/v1/auth/registro/acudiente", RegistroAcudienteSolicitud("Marta Ruiz", correo, "Clave1234", codigo, autorizaDatosMenor = true))
        assertEquals(HttpStatusCode.Created, respuesta.status, respuesta.bodyAsText())
        return iniciarSesion(correo).tokenAcceso
    }

    /** Una partida terminada: los residuos del reto en orden, con [errores] fallos en el primero. */
    private fun partida(reto: RetoNinoDto, idLocal: String, errores: Int, inicioMs: Long, tiempoMs: Long = 60_000): PartidaDto {
        var momento = inicioMs
        val intentos = buildList {
            reto.residuos.forEachIndexed { indice, residuoId ->
                val correcta = CatalogoResiduos.porId(residuoId)!!.categoria
                if (indice == 0) repeat(errores) {
                    val equivocada = CategoriaResiduo.entries.first { it != correcta }
                    add(IntentoDto(residuoId, equivocada, correcto = false, momentoMs = momento++))
                }
                add(IntentoDto(residuoId, correcta, correcto = true, momentoMs = momento++))
            }
        }
        return PartidaDto(idLocal, reto.id, terminada = true, aciertos = reto.residuos.size, errores = errores, rachaMaxima = reto.residuos.size, tiempoMs = tiempoMs, intentos = intentos)
    }

    private suspend fun ApiDePrueba.enviarPartidas(tokenNino: String, vararg partidas: PartidaDto): LotePartidasDto {
        val respuesta = post("/api/v1/nino/partidas", LotePartidasSolicitud(partidas.toList()), tokenNino)
        assertEquals(HttpStatusCode.OK, respuesta.status, respuesta.bodyAsText())
        return respuesta.body()
    }

    // --- Escenarios ---

    @Test
    fun salud() = probarApi {
        assertEquals(HttpStatusCode.OK, get("/salud").status)
        // La vista previa de correos está apagada en esta configuración.
        assertEquals(HttpStatusCode.NotFound, get("/correos/vista-previa/logro").status)
    }

    @Test
    fun recorridoCompletoDocenteNinoYAcudiente() = probarApi {
        val docente = registrarDocente()
        val cuenta = get("/api/v1/cuenta", docente).body<CuentaDto>()
        assertEquals(RolAdulto.Docente, cuenta.rol)
        assertEquals("Transición A", cuenta.grupo)

        val salome = agregarNino(docente, "salomé", "m")
        assertEquals("Salomé M.", salome.nombre)
        val juan = agregarNino(docente, "Juan", "T")

        val reto = publicar(docente)
        assertEquals(EstadoReto.Publicado, reto.estado)
        val codigo = assertNotNull(reto.codigo)

        // El niño escribe el código, ve a su grupo y elige su nombre.
        val conCodigo = get("/api/v1/nino/retos/$codigo").body<CodigoRetoDto>()
        assertEquals(4, conCodigo.reto.residuos.size)
        assertEquals(listOf("Juan T.", "Salomé M."), conCodigo.ninos.map { it.nombre })
        val sesionNino = entrarComoNino(codigo, salome.id)
        assertEquals(20, sesionNino.limiteMinutosDiarios)
        assertEquals(1, get("/api/v1/docente/retos/${reto.id}/ingresos", docente).body<IngresosDto>().ninosQueEntraron)

        // Primera partida con un error: no hay insignias todavía.
        val primera = partida(sesionNino.reto, "local-1", errores = 1, inicioMs = 1_000)
        assertEquals(emptyList(), enviarPartidas(sesionNino.token, primera).insigniasNuevas)
        // Reenviar el mismo lote no duplica nada.
        enviarPartidas(sesionNino.token, primera)

        // La mamá se registra con el código de Salomé antes de la segunda partida.
        val acudiente = registrarAcudiente(salome.codigoVinculacion)
        val hijos = get("/api/v1/acudiente/hijos", acudiente).body<List<HijoDto>>()
        assertEquals(listOf("Salomé M."), hijos.map { it.nombre })

        // Segunda partida sin errores: 4 + 4 aciertos seguidos entre retos dan la racha de 5.
        val segunda = partida(sesionNino.reto, "local-2", errores = 0, inicioMs = 10_000)
        val lote = enviarPartidas(sesionNino.token, segunda)
        assertEquals(listOf(Insignia.AmigaTortuga, Insignia.RachaDeCinco), lote.insigniasNuevas)
        val progreso = get("/api/v1/nino/progreso", sesionNino.token).body<ProgresoNinoDto>()
        assertEquals(1, progreso.retosCompletados)
        assertEquals(8, progreso.mejorRachaEntreRetos)

        // Un solo correo de logro al día, aunque haya dos insignias.
        assertEquals(1, correos.enviados.size)
        assertEquals("marta@casa.co", correos.enviados.single().destinatario)
        assertTrue("Salomé ganó la insignia «Racha de 5»" in correos.enviados.single().correo.asunto)

        // Lo que ve la docente.
        val tablero = get("/api/v1/docente/tablero", docente).body<TableroDto>()
        assertEquals(1, tablero.completaron)
        assertEquals(2, tablero.totalNinos)
        assertEquals(8 * 100 / 9, tablero.aciertosPromedio)
        val grupo = get("/api/v1/docente/grupo/hoy", docente).body<GrupoHoyDto>()
        assertEquals(TipoEstadoNino.Completo, grupo.ninos.first { it.id == salome.id }.estado.tipo)
        assertEquals(TipoEstadoNino.SinEmpezar, grupo.ninos.first { it.id == juan.id }.estado.tipo)
        val detalle = get("/api/v1/docente/ninos/${salome.id}", docente).body<DetalleNinoDto>()
        assertTrue(detalle.acudienteVinculado)
        assertEquals(2, detalle.insignias.count { it.ganada })

        // Lo que ve la mamá.
        val inicio = get("/api/v1/acudiente/hijos/${salome.id}/inicio", acudiente).body<InicioPadresDto>()
        assertEquals(2, inicio.insigniasGanadas)
        assertEquals(1, inicio.semana.dias.size)
        assertTrue(inicio.semana.dias.single().completo)
        val logros = get("/api/v1/acudiente/hijos/${salome.id}/logros", acudiente).body<LogrosHijoDto>()
        assertEquals(1, logros.retosCompletados)
        val historial = get("/api/v1/acudiente/hijos/${salome.id}/correos", acudiente).body<HistorialCorreosDto>()
        assertEquals(1, historial.correos.size)
        val privacidad = get("/api/v1/acudiente/hijos/${salome.id}/privacidad", acudiente).body<PrivacidadHijoDto>()
        assertEquals(5 + 4, privacidad.registros)
        assertEquals("Marta Ruiz", privacidad.autorizadaPor)

        // El control parental llega al niño.
        assertEquals(HttpStatusCode.NoContent, put("/api/v1/acudiente/hijos/${salome.id}/control-parental", ControlParentalDto(limiteMinutosDiarios = 25), acudiente).status)
        assertEquals(25, get("/api/v1/nino/progreso", sesionNino.token).body<ProgresoNinoDto>().limiteMinutosDiarios)
        assertEquals(HttpStatusCode.UnprocessableEntity, put("/api/v1/acudiente/hijos/${salome.id}/control-parental", ControlParentalDto(limiteMinutosDiarios = 7), acudiente).status)
    }

    @Test
    fun cadaRolSoloVeLoSuyo() = probarApi {
        val docente = registrarDocente()
        val salome = agregarNino(docente, "Salomé", "M")
        val juan = agregarNino(docente, "Juan", "T")
        val acudiente = registrarAcudiente(salome.codigoVinculacion)

        // Un acudiente no ve a un niño que no es suyo: 404, sin confirmar que existe.
        assertEquals(HttpStatusCode.NotFound, get("/api/v1/acudiente/hijos/${juan.id}/inicio", acudiente).status)
        // Los tokens no sirven en el modo equivocado.
        assertEquals(HttpStatusCode.Unauthorized, get("/api/v1/docente/tablero", acudiente).status)
        assertEquals(HttpStatusCode.Unauthorized, get("/api/v1/acudiente/hijos", docente).status)
        assertEquals(HttpStatusCode.Unauthorized, get("/api/v1/docente/tablero").status)

        // Otra docente no ve los niños del primer grupo.
        val otra = registrarDocente("otra@jardin.co")
        assertEquals(HttpStatusCode.NotFound, get("/api/v1/docente/ninos/${salome.id}", otra).status)
        // Y el token de un niño no sirve para retos de otro grupo.
        val retoOtra = publicar(otra)
        val retoPrimera = publicar(docente)
        val sesion = entrarComoNino(retoPrimera.codigo!!, salome.id)
        val ajena = PartidaDto("x", retoOtra.id, true, 1, 0, 1, 1000, emptyList())
        assertEquals(HttpStatusCode.Forbidden, post("/api/v1/nino/partidas", LotePartidasSolicitud(listOf(ajena)), sesion.token).status)
        // Ni para entrar al reto de otro grupo con su propio id.
        assertEquals(HttpStatusCode.NotFound, post("/api/v1/nino/sesiones", SesionNinoSolicitud(retoOtra.codigo!!, salome.id)).status)
    }

    @Test
    fun elCodigoSoloValeElDiaDelReto() = probarApi {
        val docente = registrarDocente()
        agregarNino(docente, "Salomé", "M")
        val hoy = publicar(docente)
        val manana = publicar(docente, fecha = reloj.hoy().plusDays(1).toString())
        assertEquals(EstadoReto.Programado, manana.estado)
        assertNotEquals(hoy.codigo, manana.codigo)

        assertEquals(HttpStatusCode.OK, get("/api/v1/nino/retos/${hoy.codigo}").status)
        // El código de mañana todavía no existe para los niños.
        assertEquals(HttpStatusCode.NotFound, get("/api/v1/nino/retos/${manana.codigo}").status)
        assertEquals(HttpStatusCode.NotFound, get("/api/v1/nino/retos/abcd").status)

        // Un programado se puede editar; lo publicado de hoy no.
        val editado = publicar(docente, Dificultad.Dificil, manana.fecha!!, idExistente = manana.id)
        assertEquals(Dificultad.Dificil, editado.dificultad)
        assertEquals(manana.codigo, editado.codigo)
        val editarHoy = post(
            "/api/v1/docente/retos",
            RetoBorradorSolicitud(listOf(CategoriaResiduo.Organico), Dificultad.Facil, hoy.fecha!!, hoy.id),
            docente,
        )
        assertEquals("reto_publicado", editarHoy.body<CuerpoError>().error)

        // A medianoche el de hoy vence y el de mañana se activa.
        reloj.avanzarDias(1)
        val vencido = get("/api/v1/nino/retos/${hoy.codigo}")
        assertEquals(HttpStatusCode.Gone, vencido.status)
        assertEquals("codigo_vencido", vencido.body<CuerpoError>().error)
        assertEquals(8, get("/api/v1/nino/retos/${manana.codigo}").body<CodigoRetoDto>().reto.residuos.size)
        // El token de acceso duró una hora: al otro día hay que volver a entrar.
        assertEquals(HttpStatusCode.Unauthorized, get("/api/v1/docente/tablero", docente).status)
        val docenteManana = iniciarSesion("laura@jardin.co").tokenAcceso
        // No se publica para un día que ya pasó.
        val pasado = post("/api/v1/docente/retos", RetoBorradorSolicitud(listOf(CategoriaResiduo.Organico), Dificultad.Facil, hoy.fecha!!), docenteManana)
        assertEquals("fecha_pasada", pasado.body<CuerpoError>().error)
    }

    @Test
    fun registroYValidaciones() = probarApi {
        registrarDocente()
        val repetido = post("/api/v1/auth/registro/docente", RegistroDocenteSolicitud("Otra", "LAURA@jardin.co", "J", "G", "Clave1234"))
        assertEquals(HttpStatusCode.Conflict, repetido.status)
        val corta = post("/api/v1/auth/registro/docente", RegistroDocenteSolicitud("Otra", "otra@jardin.co", "J", "G", "123"))
        assertEquals("contrasena_corta", corta.body<CuerpoError>().error)
        val sinCodigo = post("/api/v1/auth/registro/acudiente", RegistroAcudienteSolicitud("Marta", "m@c.co", "Clave1234", "ZZ-9999", true))
        assertEquals("codigo_invalido", sinCodigo.body<CuerpoError>().error)
        val sinAutorizacion = post("/api/v1/auth/registro/acudiente", RegistroAcudienteSolicitud("Marta", "m@c.co", "Clave1234", "ZZ-9999", false))
        assertEquals("autorizacion_requerida", sinAutorizacion.body<CuerpoError>().error)
        val malaClave = post("/api/v1/auth/sesion", InicioSesionSolicitud("laura@jardin.co", "otra-clave"))
        assertEquals(HttpStatusCode.Unauthorized, malaClave.status)
        assertEquals("credenciales_invalidas", malaClave.body<CuerpoError>().error)
    }

    @Test
    fun elTokenDeRefrescoRotaYDetectaCopias() = probarApi {
        registrarDocente()
        val sesion = iniciarSesion("laura@jardin.co")
        val nueva = post("/api/v1/auth/refresco", RefrescoSolicitud(sesion.tokenRefresco)).body<SesionDto>()
        assertNotEquals(sesion.tokenRefresco, nueva.tokenRefresco)
        // Usar otra vez el viejo es señal de copia: se cierran todas las sesiones.
        assertEquals(HttpStatusCode.Unauthorized, post("/api/v1/auth/refresco", RefrescoSolicitud(sesion.tokenRefresco)).status)
        assertEquals(HttpStatusCode.Unauthorized, post("/api/v1/auth/refresco", RefrescoSolicitud(nueva.tokenRefresco)).status)
    }

    @Test
    fun recuperarLaContrasena() = probarApi {
        registrarDocente()
        // La respuesta es igual exista o no el correo.
        assertEquals(HttpStatusCode.Accepted, post("/api/v1/auth/recuperacion", RecuperacionSolicitud("nadie@x.co")).status)
        assertEquals(0, correos.enviados.size)
        assertEquals(HttpStatusCode.Accepted, post("/api/v1/auth/recuperacion", RecuperacionSolicitud("laura@jardin.co")).status)
        val enlace = Regex("https://reciclakids\\.test/restablecer\\?token=([A-Za-z0-9_-]+)").find(correos.enviados.single().correo.textoPlano)!!
        val token = enlace.groupValues[1]

        assertEquals(HttpStatusCode.OK, get("/restablecer?token=$token").status)
        val cambio = http.submitForm("/restablecer", parameters {
            append("token", token)
            append("contrasena", "NuevaClave99")
            append("confirmacion", "NuevaClave99")
        })
        assertEquals(HttpStatusCode.OK, cambio.status)
        iniciarSesion("laura@jardin.co", "NuevaClave99")
        // El enlace sirve una sola vez.
        val otraVez = http.submitForm("/restablecer", parameters {
            append("token", token)
            append("contrasena", "OtraClave99")
            append("confirmacion", "OtraClave99")
        })
        assertEquals(HttpStatusCode.UnprocessableEntity, otraVez.status)
    }

    @Test
    fun elLimiteDeIntentosFrenaLaFuerzaBruta() = probarApi {
        val atacante = clienteDesde("203.0.113.9")
        val estados = (1..12).map {
            atacante.post("/api/v1/auth/sesion") {
                setBody(InicioSesionSolicitud("laura@jardin.co", "intento-$it"))
            }.status
        }
        assertEquals(HttpStatusCode.TooManyRequests, estados.last())
    }

    @Test
    fun reporteSemanalYBajaDeCorreos() = probarApi {
        val docente = registrarDocente()
        val salome = agregarNino(docente, "Salomé", "M")
        val reto = publicar(docente)
        val sesionNino = entrarComoNino(reto.codigo!!, salome.id)
        registrarAcudiente(salome.codigoVinculacion)
        enviarPartidas(sesionNino.token, partida(sesionNino.reto, "a", errores = 1, inicioMs = 1))

        // Cloud Scheduler sin la clave no entra.
        assertEquals(HttpStatusCode.Unauthorized, post("/tareas/reporte-semanal").status)
        val tarea = http.post("/tareas/reporte-semanal") { header("X-Tareas-Clave", "clave-de-tareas-de-prueba-larga") }
        assertEquals(1, tarea.body<ResultadoTareaDto>().correosEnviados)
        val reporte = correos.enviados.last().correo
        assertTrue(reporte.asunto.startsWith("El resumen de la semana de Salomé"))

        // El enlace «Dejar de recibir» apaga los dos correos sin iniciar sesión.
        val baja = Regex("https://reciclakids\\.test(/correos/baja\\?token=[A-Za-z0-9._-]+)").find(reporte.textoPlano)!!.groupValues[1]
        assertEquals(HttpStatusCode.OK, get(baja).status)
        val otraVez = http.post("/tareas/reporte-semanal") { header("X-Tareas-Clave", "clave-de-tareas-de-prueba-larga") }
        assertEquals(0, otraVez.body<ResultadoTareaDto>().correosEnviados)

        // El reporte de la docente es de la semana anterior: aún vacío.
        val semanal = get("/api/v1/docente/reportes/semanal", docente).body<ReporteSemanalDto>()
        assertEquals("2026-10-05", semanal.inicio)
        assertTrue(semanal.completadosPorNino.isEmpty())
    }

    @Test
    fun vincularOtroHijoYQuitarNinos() = probarApi {
        val docente = registrarDocente()
        val salome = agregarNino(docente, "Salomé", "M")
        val martin = agregarNino(docente, "Martín", "M")
        val acudiente = registrarAcudiente(salome.codigoVinculacion)

        val vinculacion = post("/api/v1/acudiente/vinculaciones", VincularSolicitud(martin.codigoVinculacion.lowercase()), acudiente).body<VinculacionDto>()
        assertEquals("Martín M.", vinculacion.hijo.nombre)
        assertTrue(!vinculacion.yaEstaba)
        assertTrue(post("/api/v1/acudiente/vinculaciones", VincularSolicitud(martin.codigoVinculacion), acudiente).body<VinculacionDto>().yaEstaba)

        // Regenerar el código no desvincula a quien ya estaba.
        val nuevo = post("/api/v1/docente/ninos/${martin.id}/codigo-vinculacion", token = docente).body<com.reciclakids.models.dto.CodigoVinculacionDto>()
        assertNotEquals(martin.codigoVinculacion, nuevo.codigoVinculacion)
        assertEquals(2, get("/api/v1/acudiente/hijos", acudiente).body<List<HijoDto>>().size)
        assertEquals("codigo_invalido", post("/api/v1/acudiente/vinculaciones", VincularSolicitud(martin.codigoVinculacion), acudiente).body<CuerpoError>().error)

        // Quitar al niño borra todo lo suyo, también la vinculación.
        assertEquals(HttpStatusCode.NoContent, delete("/api/v1/docente/ninos/${martin.id}", docente).status)
        assertEquals(listOf("Salomé M."), get("/api/v1/acudiente/hijos", acudiente).body<List<HijoDto>>().map { it.nombre })

        // Solicitud de supresión de datos del menor (UI-34).
        assertEquals(HttpStatusCode.Created, post("/api/v1/acudiente/hijos/${salome.id}/privacidad/solicitud-eliminacion", token = acudiente).status)
        assertEquals("2026-10-13", get("/api/v1/acudiente/hijos/${salome.id}/privacidad", acudiente).body<PrivacidadHijoDto>().solicitudEliminacion)
        assertEquals(HttpStatusCode.NoContent, delete("/api/v1/acudiente/hijos/${salome.id}/privacidad/solicitud-eliminacion", acudiente).status)
        assertNull(get("/api/v1/acudiente/hijos/${salome.id}/privacidad", acudiente).body<PrivacidadHijoDto>().solicitudEliminacion)
    }

    @Test
    fun eliminarLosDatosDelGrupo() = probarApi {
        val docente = registrarDocente()
        val salome = agregarNino(docente, "Salomé", "M")
        val reto = publicar(docente)
        val sesion = entrarComoNino(reto.codigo!!, salome.id)
        enviarPartidas(sesion.token, partida(sesion.reto, "a", errores = 0, inicioMs = 1))
        assertEquals(1, get("/api/v1/docente/tablero", docente).body<TableroDto>().completaron)

        assertEquals(HttpStatusCode.NoContent, delete("/api/v1/docente/grupo/datos", docente).status)
        val tablero = get("/api/v1/docente/tablero", docente).body<TableroDto>()
        assertEquals(0, tablero.completaron)
        assertEquals(1, tablero.totalNinos)
        assertTrue(get("/api/v1/docente/ninos/${salome.id}", docente).body<DetalleNinoDto>().insignias.none { it.ganada })
    }
}
