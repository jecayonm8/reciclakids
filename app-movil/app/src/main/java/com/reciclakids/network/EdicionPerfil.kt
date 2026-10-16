package com.reciclakids.network

import com.reciclakids.model.PerfilAcudiente
import com.reciclakids.model.PerfilDocente

/** Lo que la docente puede cambiar de su perfil desde UI-28. El correo es también su usuario. */
data class DatosPerfilDocente(
    val nombre: String,
    val correo: String,
    val jardin: String,
    val grupo: String,
)

/** Lo que el acudiente puede cambiar de su perfil desde UI-35. El correo es también su usuario. */
data class DatosPerfilAcudiente(
    val nombre: String,
    val correo: String,
)

/** [CorreoEnUso]: otra cuenta ya tiene ese correo y no se guardó nada. */
enum class ResultadoEdicionPerfil { Guardado, CorreoEnUso, SinConexion }

val PerfilDocente.datosEditables: DatosPerfilDocente
    get() = DatosPerfilDocente(nombre, correo, jardin, grupo)

val PerfilAcudiente.datosEditables: DatosPerfilAcudiente
    get() = DatosPerfilAcudiente(nombre, correo)

fun PerfilDocente.con(datos: DatosPerfilDocente): PerfilDocente =
    copy(nombre = datos.nombre, correo = datos.correo, jardin = datos.jardin, grupo = datos.grupo)

fun PerfilAcudiente.con(datos: DatosPerfilAcudiente): PerfilAcudiente = copy(nombre = datos.nombre, correo = datos.correo)
