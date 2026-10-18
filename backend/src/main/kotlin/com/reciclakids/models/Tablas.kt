package com.reciclakids.models

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

// Tablas de Exposed. El esquema lo crea y lo cambia Flyway (src/main/resources/db/migration):
// estas definiciones solo describen las columnas para consultar.

object Usuarios : Table("usuarios") {
    val id = varchar("id", 36)
    val rol = varchar("rol", 16)
    val nombre = varchar("nombre", 120)
    val correo = varchar("correo", 254)
    val hashContrasena = varchar("hash_contrasena", 100)
    val creadoEn = timestampWithTimeZone("creado_en")
    override val primaryKey = PrimaryKey(id)
}

object Sesiones : Table("sesiones") {
    val id = varchar("id", 36)
    val usuarioId = varchar("usuario_id", 36)
    val hashRefresco = varchar("hash_refresco", 64)
    val expiraEn = timestampWithTimeZone("expira_en")
    val revocada = bool("revocada")
    val creadaEn = timestampWithTimeZone("creada_en")
    override val primaryKey = PrimaryKey(id)
}

object Recuperaciones : Table("recuperaciones") {
    val id = varchar("id", 36)
    val usuarioId = varchar("usuario_id", 36)
    val hashToken = varchar("hash_token", 64)
    val expiraEn = timestampWithTimeZone("expira_en")
    val usada = bool("usada")
    val creadaEn = timestampWithTimeZone("creada_en")
    override val primaryKey = PrimaryKey(id)
}

object Grupos : Table("grupos") {
    val id = varchar("id", 36)
    val docenteId = varchar("docente_id", 36)
    val jardin = varchar("jardin", 120)
    val nombre = varchar("nombre", 80)
    val avisoPublicacion = bool("aviso_publicacion")
    val avisoResumenDiario = bool("aviso_resumen_diario")
    val avisoNinosSinJugar = bool("aviso_ninos_sin_jugar")
    val creadoEn = timestampWithTimeZone("creado_en")
    override val primaryKey = PrimaryKey(id)
}

object Ninos : Table("ninos") {
    val id = varchar("id", 36)
    val grupoId = varchar("grupo_id", 36)
    val nombre = varchar("nombre", 80)
    val edad = short("edad").nullable()
    val avatar = varchar("avatar", 40)
    val codigoVinculacion = varchar("codigo_vinculacion", 7)
    val rachaEntreRetos = integer("racha_entre_retos")
    val mejorRachaEntreRetos = integer("mejor_racha_entre_retos")
    val creadoEn = timestampWithTimeZone("creado_en")
    override val primaryKey = PrimaryKey(id)
}

object Vinculaciones : Table("vinculaciones") {
    val acudienteId = varchar("acudiente_id", 36)
    val ninoId = varchar("nino_id", 36)
    val vinculadoEn = timestampWithTimeZone("vinculado_en")
    val autorizadaEn = timestampWithTimeZone("autorizada_en")
    override val primaryKey = PrimaryKey(acudienteId, ninoId)
}

object ControlesParentales : Table("controles_parentales") {
    val ninoId = varchar("nino_id", 36)
    val limiteMinutos = short("limite_minutos")
    val dificultadSugerida = varchar("dificultad_sugerida", 10)
    val correosLogro = bool("correos_logro")
    val reporteSemanal = bool("reporte_semanal")
    val recordatorioSinJugar = bool("recordatorio_sin_jugar")
    val actualizadoEn = timestampWithTimeZone("actualizado_en")
    override val primaryKey = PrimaryKey(ninoId)
}

object Retos : Table("retos") {
    val id = varchar("id", 36)
    val grupoId = varchar("grupo_id", 36)
    val nombre = varchar("nombre", 120)
    val categorias = varchar("categorias", 60)
    val dificultad = varchar("dificultad", 10)
    val fecha = date("fecha").nullable()
    val estado = varchar("estado", 12)
    val codigo = char("codigo", 4).nullable()
    val reutilizadoDe = varchar("reutilizado_de", 36).nullable()
    val creadoEn = timestampWithTimeZone("creado_en")
    override val primaryKey = PrimaryKey(id)
}

object RetoResiduos : Table("reto_residuos") {
    val retoId = varchar("reto_id", 36)
    val orden = short("orden")
    val residuoId = varchar("residuo_id", 20)
    override val primaryKey = PrimaryKey(retoId, orden)
}

object Ingresos : Table("ingresos") {
    val ninoId = varchar("nino_id", 36)
    val retoId = varchar("reto_id", 36)
    val entroEn = timestampWithTimeZone("entro_en")
    override val primaryKey = PrimaryKey(ninoId, retoId)
}

object Partidas : Table("partidas") {
    val id = varchar("id", 36)
    val ninoId = varchar("nino_id", 36)
    val retoId = varchar("reto_id", 36)
    val idLocal = varchar("id_local", 64)
    val fecha = date("fecha")
    val terminada = bool("terminada")
    val aciertos = short("aciertos")
    val errores = short("errores")
    val rachaMaxima = short("racha_maxima")
    val tiempoMs = long("tiempo_ms")
    val recibidaEn = timestampWithTimeZone("recibida_en")
    override val primaryKey = PrimaryKey(id)
}

object Intentos : Table("intentos") {
    val partidaId = varchar("partida_id", 36)
    val momentoMs = long("momento_ms")
    val residuoId = varchar("residuo_id", 20)
    val categoriaElegida = varchar("categoria_elegida", 20)
    val correcto = bool("correcto")
    override val primaryKey = PrimaryKey(partidaId, momentoMs, residuoId)
}

object InsigniasGanadas : Table("insignias_ganadas") {
    val ninoId = varchar("nino_id", 36)
    val insignia = varchar("insignia", 30)
    val ganadaEl = date("ganada_el")
    override val primaryKey = PrimaryKey(ninoId, insignia)
}

object CorreosEnviados : Table("correos_enviados") {
    val id = varchar("id", 36)
    val acudienteId = varchar("acudiente_id", 36)
    val ninoId = varchar("nino_id", 36)
    val tipo = varchar("tipo", 20)
    val destinatario = varchar("destinatario", 254)
    val insignia = varchar("insignia", 30).nullable()
    val retosCompletados = short("retos_completados")
    val retosPublicados = short("retos_publicados")
    val aciertos = short("aciertos").nullable()
    val enviadoEn = timestampWithTimeZone("enviado_en")
    override val primaryKey = PrimaryKey(id)
}

object SolicitudesEliminacion : Table("solicitudes_eliminacion") {
    val ninoId = varchar("nino_id", 36)
    val solicitadaPor = varchar("solicitada_por", 36)
    val solicitadaEl = date("solicitada_el")
    override val primaryKey = PrimaryKey(ninoId)
}
