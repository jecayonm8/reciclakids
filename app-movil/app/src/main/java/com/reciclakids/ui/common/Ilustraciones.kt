package com.reciclakids.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.reciclakids.R
import com.reciclakids.model.Insignia

/**
 * Ilustraciones entregadas (res/drawable-nodpi). Lo que todavía falta (y se sigue viendo como marcador rayado)
 * está en app-movil/ilustraciones/LEEME.md.
 */
object Ilustraciones {
    /** Catálogo de avatares: solo dos. El pez guía representa al niño y la tortuga con capa a la niña. */
    val avatarNino: Int get() = R.drawable.avatar_nino

    val avatarNina: Int get() = R.drawable.avatar_nina

    /**
     * Avatar provisional: la app todavía no sabe si es niño o niña (el campo `avatar` que guarda el
     * backend no llega al modelo de la app), así que se asigna uno estable a partir del nombre.
     * Mismo nombre, mismo avatar en todas las pantallas. Cuando exista la elección, solo cambia esta función.
     */
    @DrawableRes
    fun avatarDe(nombre: String): Int =
        if (nombre.trim().lowercase().hashCode().mod(2) == 0) avatarNino else avatarNina

    /** Arte de cada residuo por su id (ver CatalogoResiduos); null si todavía no existe. */
    @DrawableRes
    fun residuo(id: String): Int? = when (id) {
        "botella" -> R.drawable.residuo_botella
        "lata" -> R.drawable.residuo_lata
        "papel" -> R.drawable.residuo_papel
        "carton" -> R.drawable.residuo_carton
        "cascara" -> R.drawable.residuo_cascara
        "restos" -> R.drawable.residuo_restos
        "servilleta" -> R.drawable.residuo_servilleta
        "empaque" -> R.drawable.residuo_empaque
        else -> null
    }

    /** Arte de cada insignia (medalla redonda; la de la tortuga es el busto de UI-29). */
    @DrawableRes
    fun insignia(insignia: Insignia): Int = when (insignia) {
        Insignia.AmigaTortuga -> R.drawable.amiga_tortuga
        Insignia.RapidoComoPez -> R.drawable.insignia_rapido_como_pez
        Insignia.RachaDeCinco -> R.drawable.insignia_racha_de_cinco
        Insignia.CincoRetosDiarios -> R.drawable.insignia_cinco_retos_diarios
        Insignia.PulpoOrdenado -> R.drawable.insignia_pulpo_ordenado
        Insignia.AguaCristalina -> R.drawable.insignia_agua_cristalina
        Insignia.CoralFeliz -> R.drawable.insignia_coral_feliz
        Insignia.GuardianDelMar -> R.drawable.insignia_guardian_del_mar
    }

    /** Cada personaje en sus cuatro poses: flota, saluda, celebra y duerme. */
    @DrawableRes
    fun personaje(tipo: TipoPersonaje, animacion: AnimacionPersonaje): Int = when (tipo) {
        TipoPersonaje.Pez -> when (animacion) {
            AnimacionPersonaje.Flotar -> R.drawable.pez_flota
            AnimacionPersonaje.Animar -> R.drawable.pez_saluda
            AnimacionPersonaje.Celebrar -> R.drawable.pez_celebra
            AnimacionPersonaje.Dormir -> R.drawable.pez_duerme
        }
        TipoPersonaje.Tortuga -> when (animacion) {
            AnimacionPersonaje.Flotar -> R.drawable.tortuga_flota
            AnimacionPersonaje.Animar -> R.drawable.tortuga_saluda
            AnimacionPersonaje.Celebrar -> R.drawable.tortuga_celebra
            AnimacionPersonaje.Dormir -> R.drawable.tortuga_duerme
        }
        TipoPersonaje.Pulpo -> when (animacion) {
            AnimacionPersonaje.Flotar -> R.drawable.pulpo_flota
            AnimacionPersonaje.Animar -> R.drawable.pulpo_saluda
            AnimacionPersonaje.Celebrar -> R.drawable.pulpo_celebra
            AnimacionPersonaje.Dormir -> R.drawable.pulpo_duerme
        }
    }

    /** El acuario se limpia con el progreso: 1 agua turbia · 2 aparece el pez · 3 pez y tortuga · 4 pez, tortuga y pulpo. */
    @DrawableRes
    fun acuario(nivel: Int): Int = when (nivel) {
        1 -> R.drawable.acuario_nivel_1
        2 -> R.drawable.acuario_nivel_2
        3 -> R.drawable.acuario_nivel_3
        else -> R.drawable.acuario_nivel_4
    }
}

/** Ilustración decorativa: no tiene descripción porque el texto de la pantalla ya dice lo mismo. */
@Composable
fun IlustracionImagen(
    @DrawableRes recurso: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    alineacion: Alignment = Alignment.Center,
) {
    Image(
        painter = painterResource(recurso),
        contentDescription = null,
        modifier = modifier,
        alignment = alineacion,
        contentScale = contentScale,
    )
}

@Composable
fun AvatarIlustrado(nombre: String, modifier: Modifier = Modifier) {
    IlustracionImagen(Ilustraciones.avatarDe(nombre), modifier)
}

/** UI-32: los peces dormidos, con la esquina redondeada de la escena original. */
@Composable
fun IlustracionPecesDormidos(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.peces_dormidos),
        contentDescription = null,
        modifier = modifier.width(240.dp).aspectRatio(419f / 343f).clip(RoundedCornerShape(28.dp)),
    )
}

/**
 * Insignia grande (UI-29). La de la tortuga viene cortada a la cintura: se apoya abajo del círculo
 * para que la curva del marco haga de corte. Las demás ya son medallas redondas completas.
 */
@Composable
fun IlustracionInsignia(insignia: Insignia, modifier: Modifier = Modifier) {
    val alineacion = if (insignia == Insignia.AmigaTortuga) Alignment.BottomCenter else Alignment.Center
    IlustracionImagen(Ilustraciones.insignia(insignia), modifier.clip(CircleShape), alineacion = alineacion)
}
