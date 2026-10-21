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
 * Ilustraciones ya entregadas (res/drawable-nodpi). Donde todavía no hay arte se sigue
 * mostrando el [MarcadorIlustracion]; la lista de lo que falta está en app-movil/ilustraciones/LEEME.md.
 */
object Ilustraciones {
    private val avatares = intArrayOf(
        R.drawable.avatar_1,
        R.drawable.avatar_2,
        R.drawable.avatar_3,
        R.drawable.avatar_4,
        R.drawable.avatar_5,
        R.drawable.avatar_6,
    )

    /**
     * Avatar provisional: el niño todavía no elige avatar (no se guarda), así que se asigna uno
     * estable a partir de su nombre. Mismo nombre, mismo avatar en todas las pantallas.
     */
    @DrawableRes
    fun avatarDe(nombre: String): Int = avatares[nombre.trim().lowercase().hashCode().mod(avatares.size)]

    /** Arte de cada residuo por su id (ver CatalogoResiduos); null si todavía no existe. */
    @DrawableRes
    fun residuo(id: String): Int? = when (id) {
        "botella" -> R.drawable.residuo_botella
        "lata" -> R.drawable.residuo_lata
        "restos" -> R.drawable.residuo_restos
        else -> null
    }

    /** Arte de cada insignia; null si todavía no existe. */
    @DrawableRes
    fun insignia(insignia: Insignia): Int? = when (insignia) {
        Insignia.AmigaTortuga -> R.drawable.amiga_tortuga
        else -> null
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
 * Insignia grande (UI-29). La tortuga viene cortada a la cintura: se apoya abajo del círculo
 * para que la curva del marco haga de corte.
 */
@Composable
fun IlustracionInsignia(insignia: Insignia, modifier: Modifier = Modifier) {
    val arte = Ilustraciones.insignia(insignia)
    if (arte != null) {
        IlustracionImagen(arte, modifier.clip(CircleShape), alineacion = Alignment.BottomCenter)
    } else {
        MarcadorIlustracion("ilustración\n${insignia.nombre.lowercase()}", modifier, estilo = EstiloMarcador.Insignia)
    }
}
