# Prompts para generar las ilustraciones

Cada bloque gris está **completo**: se copia y se pega tal cual en Claude Design o en una IA de imágenes
(ChatGPT, Gemini u otra). Antes de pegar, **adjunta las imágenes de referencia** que dice la línea «Adjunta».
Están en `app-movil/app/src/main/res/drawable-nodpi/`.

## Qué herramienta usar

Claude Design arma diseños en vector/HTML. Le va bien al logo, el ícono, la mano y los estados vacíos.
Para pez, tortuga, pulpo, insignias, acuario y residuos con el mismo pincel suave que ya existe, una IA de imágenes
con `pez_guia.png` y `amiga_tortuga.png` como referencia suele clavar mejor el estilo.
Ninguna da fondo transparente: por eso todo pide **fondo blanco liso**; después se recorta con
`herramientas/recortar_fondo.py`.

## Orden recomendado

1. **Tortuga**, con `amiga_tortuga.png` de referencia.
2. **Pez**, con `pez_guia.png` de referencia.
3. **Pulpo**: primero el diseño (paso 0), apruébalo y úsalo de referencia para las 4 poses. Todo en la misma conversación.
4. Lo demás, adjuntando siempre las mismas referencias para que los personajes no cambien.

## Reglas

- Nombra cada archivo **exactamente** como el título del bloque (minúsculas, guion bajo, `.png`).
- Descarga el PNG original, no una captura de pantalla.
- Un archivo por ilustración, un personaje por imagen, cuadrado 1024 × 1024.
- Sin marcas reales ni letras: las IAs deforman el texto. Las únicas excepciones están dichas en el bloque.

## Cómo pasarlas al proyecto

Adjúntalas en el chat con Claude (de a ~10 por mensaje) **o** guárdalas en `nuevas-ilustraciones/` en la raíz del
repo en tu PC y avisa. Claude las recorta, las copia a `drawable-nodpi`, las conecta en el código y actualiza el
`LEEME.md`. Esa carpeta no se sube: bórrala antes del commit.

---

## 1. Personajes (12 poses + 1)

Las 4 poses son las mismas para los tres personajes: **flota** (nada tranquilo, de lado en 3/4, mira al frente, sonrisa suave), **saluda**, **celebra** (brazos o aletas arriba, boca abierta de alegría, 3 estrellitas alrededor) y **duerme** (ojos cerrados, sonrisa tranquila; única letra permitida: dos «z» pequeñas).

### Paso 0 · Diseño del pulpo (no se entrega, es la referencia)
Adjunta: `pez_guia.png` y `amiga_tortuga.png`

```text
Ilustración infantil 2D para app de reciclaje (niños de 3 a 7 años). Estilo suave, con degradados sutiles y sombreado limpio, formas redondeadas, sin contorno negro grueso, colores brillantes y amables (turquesa, verde, naranja, amarillo), ojos grandes y brillantes. Mismo estilo y mismos personajes que las imágenes de referencia adjuntas. Diseño de personaje nuevo, de frente, cuerpo completo: pulpo morado, cabeza redonda, ojos grandes, mejillas rosadas, ocho tentáculos cortos y rizados, corbatín negro. Es el personaje de la caneca negra y debe verse amigable y ordenado. Que combine con el pez y la tortuga de las referencias. Fondo blanco liso #FFFFFF, sin sombra en el piso, sin marco, sin texto ni letras salvo lo que se indique, elemento completo y centrado con margen, cuadrado 1024×1024.
```

### `tortuga_flota.png`
Adjunta: `amiga_tortuga.png`

```text
Ilustración infantil 2D para app de reciclaje (niños de 3 a 7 años). Estilo suave, con degradados sutiles y sombreado limpio, formas redondeadas, sin contorno negro grueso, colores brillantes y amables (turquesa, verde, naranja, amarillo), ojos grandes y brillantes. Mismo estilo y mismos personajes que las imágenes de referencia adjuntas. TORTUGA: tortuga turquesa con capa verde, camiseta blanca con símbolo de reciclaje verde y pañuelo verde anudado al cuello (la de amiga_tortuga.png), cuerpo completo con caparazón. TORTUGA nada tranquila, de lado en 3/4, mira al frente, sonrisa suave, cuerpo completo. Fondo blanco liso #FFFFFF, sin sombra en el piso, sin marco, sin texto ni letras salvo lo que se indique, elemento completo y centrado con margen, cuadrado 1024×1024.
```

### `tortuga_saluda.png`
Adjunta: `amiga_tortuga.png`

```text
Ilustración infantil 2D para app de reciclaje (niños de 3 a 7 años). Estilo suave, con degradados sutiles y sombreado limpio, formas redondeadas, sin contorno negro grueso, colores brillantes y amables (turquesa, verde, naranja, amarillo), ojos grandes y brillantes. Mismo estilo y mismos personajes que las imágenes de referencia adjuntas. TORTUGA: tortuga turquesa con capa verde, camiseta blanca con símbolo de reciclaje verde y pañuelo verde anudado al cuello (la de amiga_tortuga.png), cuerpo completo con caparazón. TORTUGA saluda con una mano levantada, mira al frente con sonrisa grande, cuerpo completo. Fondo blanco liso #FFFFFF, sin sombra en el piso, sin marco, sin texto ni letras salvo lo que se indique, elemento completo y centrado con margen, cuadrado 1024×1024.
```

### `tortuga_celebra.png`
Adjunta: `amiga_tortuga.png`

```text
Ilustración infantil 2D para app de reciclaje (niños de 3 a 7 años). Estilo suave, con degradados sutiles y sombreado limpio, formas redondeadas, sin contorno negro grueso, colores brillantes y amables (turquesa, verde, naranja, amarillo), ojos grandes y brillantes. Mismo estilo y mismos personajes que las imágenes de referencia adjuntas. TORTUGA: tortuga turquesa con capa verde, camiseta blanca con símbolo de reciclaje verde y pañuelo verde anudado al cuello (la de amiga_tortuga.png), cuerpo completo con caparazón. TORTUGA celebra con los brazos arriba, boca abierta de alegría, 3 estrellitas alrededor, cuerpo completo. Fondo blanco liso #FFFFFF, sin sombra en el piso, sin marco, sin texto ni letras salvo lo que se indique, elemento completo y centrado con margen, cuadrado 1024×1024.
```

### `tortuga_duerme.png`
Adjunta: `amiga_tortuga.png`

```text
Ilustración infantil 2D para app de reciclaje (niños de 3 a 7 años). Estilo suave, con degradados sutiles y sombreado limpio, formas redondeadas, sin contorno negro grueso, colores brillantes y amables (turquesa, verde, naranja, amarillo), ojos grandes y brillantes. Mismo estilo y mismos personajes que las imágenes de referencia adjuntas. TORTUGA: tortuga turquesa con capa verde, camiseta blanca con símbolo de reciclaje verde y pañuelo verde anudado al cuello (la de amiga_tortuga.png), cuerpo completo con caparazón. TORTUGA duerme con los ojos cerrados y sonrisa tranquila, dos «z» pequeñas flotando (única letra permitida), cuerpo completo. Fondo blanco liso #FFFFFF, sin sombra en el piso, sin marco, sin texto ni letras salvo lo que se indique, elemento completo y centrado con margen, cuadrado 1024×1024.
```

### `pez_flota.png`
Adjunta: `pez_guia.png`

```text
Ilustración infantil 2D para app de reciclaje (niños de 3 a 7 años). Estilo suave, con degradados sutiles y sombreado limpio, formas redondeadas, sin contorno negro grueso, colores brillantes y amables (turquesa, verde, naranja, amarillo), ojos grandes y brillantes. Mismo estilo y mismos personajes que las imágenes de referencia adjuntas. PEZ: pez dorado-naranja con birrete azul de graduación con borla amarilla y medalla con brújula (el de pez_guia.png). Sin círculo de fondo. PEZ nada tranquilo, de lado en 3/4, mira al frente, sonrisa suave, cuerpo completo. Fondo blanco liso #FFFFFF, sin sombra en el piso, sin marco, sin texto ni letras salvo lo que se indique, elemento completo y centrado con margen, cuadrado 1024×1024.
```

### `pez_saluda.png`
Adjunta: `pez_guia.png`

```text
Ilustración infantil 2D para app de reciclaje (niños de 3 a 7 años). Estilo suave, con degradados sutiles y sombreado limpio, formas redondeadas, sin contorno negro grueso, colores brillantes y amables (turquesa, verde, naranja, amarillo), ojos grandes y brillantes. Mismo estilo y mismos personajes que las imágenes de referencia adjuntas. PEZ: pez dorado-naranja con birrete azul de graduación con borla amarilla y medalla con brújula (el de pez_guia.png). Sin círculo de fondo. PEZ saluda con una aleta levantada, mira al frente con sonrisa grande, cuerpo completo. Fondo blanco liso #FFFFFF, sin sombra en el piso, sin marco, sin texto ni letras salvo lo que se indique, elemento completo y centrado con margen, cuadrado 1024×1024.
```

### `pez_celebra.png`
Adjunta: `pez_guia.png`

```text
Ilustración infantil 2D para app de reciclaje (niños de 3 a 7 años). Estilo suave, con degradados sutiles y sombreado limpio, formas redondeadas, sin contorno negro grueso, colores brillantes y amables (turquesa, verde, naranja, amarillo), ojos grandes y brillantes. Mismo estilo y mismos personajes que las imágenes de referencia adjuntas. PEZ: pez dorado-naranja con birrete azul de graduación con borla amarilla y medalla con brújula (el de pez_guia.png). Sin círculo de fondo. PEZ celebra con las aletas arriba, boca abierta de alegría, 3 estrellitas alrededor, cuerpo completo. Fondo blanco liso #FFFFFF, sin sombra en el piso, sin marco, sin texto ni letras salvo lo que se indique, elemento completo y centrado con margen, cuadrado 1024×1024.
```

### `pez_duerme.png`
Adjunta: `pez_guia.png`

```text
Ilustración infantil 2D para app de reciclaje (niños de 3 a 7 años). Estilo suave, con degradados sutiles y sombreado limpio, formas redondeadas, sin contorno negro grueso, colores brillantes y amables (turquesa, verde, naranja, amarillo), ojos grandes y brillantes. Mismo estilo y mismos personajes que las imágenes de referencia adjuntas. PEZ: pez dorado-naranja con birrete azul de graduación con borla amarilla y medalla con brújula (el de pez_guia.png). Sin círculo de fondo. PEZ duerme con los ojos cerrados y sonrisa tranquila, dos «z» pequeñas flotando (única letra permitida), cuerpo completo. Fondo blanco liso #FFFFFF, sin sombra en el piso, sin marco, sin texto ni letras salvo lo que se indique, elemento completo y centrado con margen, cuadrado 1024×1024.
```

### `pulpo_flota.png`
Adjunta: la imagen aprobada del paso 0 y `pez_guia.png`

```text
Ilustración infantil 2D para app de reciclaje (niños de 3 a 7 años). Estilo suave, con degradados sutiles y sombreado limpio, formas redondeadas, sin contorno negro grueso, colores brillantes y amables (turquesa, verde, naranja, amarillo), ojos grandes y brillantes. Mismo estilo y mismos personajes que las imágenes de referencia adjuntas. PULPO: pulpo morado, cabeza redonda, ojos grandes, mejillas rosadas, ocho tentáculos cortos y rizados, corbatín negro (el diseño aprobado, adjunto). PULPO nada tranquilo, de lado en 3/4, mira al frente, sonrisa suave, cuerpo completo. Fondo blanco liso #FFFFFF, sin sombra en el piso, sin marco, sin texto ni letras salvo lo que se indique, elemento completo y centrado con margen, cuadrado 1024×1024.
```

### `pulpo_saluda.png`
Adjunta: la imagen aprobada del paso 0 y `pez_guia.png`

```text
Ilustración infantil 2D para app de reciclaje (niños de 3 a 7 años). Estilo suave, con degradados sutiles y sombreado limpio, formas redondeadas, sin contorno negro grueso, colores brillantes y amables (turquesa, verde, naranja, amarillo), ojos grandes y brillantes. Mismo estilo y mismos personajes que las imágenes de referencia adjuntas. PULPO: pulpo morado, cabeza redonda, ojos grandes, mejillas rosadas, ocho tentáculos cortos y rizados, corbatín negro (el diseño aprobado, adjunto). PULPO saluda con un tentáculo levantado, mira al frente con sonrisa grande, cuerpo completo. Fondo blanco liso #FFFFFF, sin sombra en el piso, sin marco, sin texto ni letras salvo lo que se indique, elemento completo y centrado con margen, cuadrado 1024×1024.
```

### `pulpo_celebra.png`
Adjunta: la imagen aprobada del paso 0 y `pez_guia.png`

```text
Ilustración infantil 2D para app de reciclaje (niños de 3 a 7 años). Estilo suave, con degradados sutiles y sombreado limpio, formas redondeadas, sin contorno negro grueso, colores brillantes y amables (turquesa, verde, naranja, amarillo), ojos grandes y brillantes. Mismo estilo y mismos personajes que las imágenes de referencia adjuntas. PULPO: pulpo morado, cabeza redonda, ojos grandes, mejillas rosadas, ocho tentáculos cortos y rizados, corbatín negro (el diseño aprobado, adjunto). PULPO celebra con los tentáculos arriba, boca abierta de alegría, 3 estrellitas alrededor, cuerpo completo. Fondo blanco liso #FFFFFF, sin sombra en el piso, sin marco, sin texto ni letras salvo lo que se indique, elemento completo y centrado con margen, cuadrado 1024×1024.
```

### `pulpo_duerme.png`
Adjunta: la imagen aprobada del paso 0 y `pez_guia.png`

```text
Ilustración infantil 2D para app de reciclaje (niños de 3 a 7 años). Estilo suave, con degradados sutiles y sombreado limpio, formas redondeadas, sin contorno negro grueso, colores brillantes y amables (turquesa, verde, naranja, amarillo), ojos grandes y brillantes. Mismo estilo y mismos personajes que las imágenes de referencia adjuntas. PULPO: pulpo morado, cabeza redonda, ojos grandes, mejillas rosadas, ocho tentáculos cortos y rizados, corbatín negro (el diseño aprobado, adjunto). PULPO duerme con los ojos cerrados y sonrisa tranquila, dos «z» pequeñas flotando (única letra permitida), cuerpo completo. Fondo blanco liso #FFFFFF, sin sombra en el piso, sin marco, sin texto ni letras salvo lo que se indique, elemento completo y centrado con margen, cuadrado 1024×1024.
```

### `pulpo_casi.png` · se ve a 52 dp
Adjunta: la imagen aprobada del paso 0 y `pez_guia.png`

```text
Ilustración infantil 2D para app de reciclaje (niños de 3 a 7 años). Estilo suave, con degradados sutiles y sombreado limpio, formas redondeadas, sin contorno negro grueso, colores brillantes y amables (turquesa, verde, naranja, amarillo), ojos grandes y brillantes. Mismo estilo y mismos personajes que las imágenes de referencia adjuntas. PULPO: pulpo morado, cabeza redonda, ojos grandes, mejillas rosadas, ocho tentáculos cortos y rizados, corbatín negro (el diseño aprobado, adjunto). PULPO de medio cuerpo con cara esperanzada de «¡casi, inténtalo otra vez!»: ojos grandes, cabeza ladeada y un tentáculo con el pulgar arriba. Se verá muy pequeño (52 dp): formas grandes y simples. Fondo blanco liso #FFFFFF, sin sombra en el piso, sin marco, sin texto ni letras salvo lo que se indique, elemento completo y centrado con margen, cuadrado 1024×1024.
```

---

## 2. Insignias (medalla circular)

Referencia: `amiga_tortuga.png` es la insignia que ya existe (UI-29).

### `insignia_rapido_como_pez.png`
Adjunta: `pez_guia.png`

```text
Ilustración infantil 2D para app de reciclaje (niños de 3 a 7 años). Estilo suave, con degradados sutiles y sombreado limpio, formas redondeadas, sin contorno negro grueso, colores brillantes y amables (turquesa, verde, naranja, amarillo), ojos grandes y brillantes. Mismo estilo y mismos personajes que las imágenes de referencia adjuntas. Insignia «Rápido como pez»: PEZ: pez dorado-naranja con birrete azul de graduación con borla amarilla y medalla con brújula (el de pez_guia.png). PEZ nadando muy rápido, con líneas de velocidad y una estela de burbujas. Medalla circular completa que ocupa todo el cuadro. Dentro del círculo, agua azul-turquesa con degradado suave y burbujas, como en pez_guia.png. Fuera del círculo, blanco liso #FFFFFF. Sin texto ni letras salvo lo que se indique. Cuadrado 1024×1024.
```

### `insignia_racha_de_cinco.png`
Adjunta: `pez_guia.png`

```text
Ilustración infantil 2D para app de reciclaje (niños de 3 a 7 años). Estilo suave, con degradados sutiles y sombreado limpio, formas redondeadas, sin contorno negro grueso, colores brillantes y amables (turquesa, verde, naranja, amarillo), ojos grandes y brillantes. Mismo estilo y mismos personajes que las imágenes de referencia adjuntas. Insignia «Racha de 5»: PEZ: pez dorado-naranja con birrete azul de graduación con borla amarilla y medalla con brújula (el de pez_guia.png). PEZ saltando a través de 5 burbujas en fila, con un número 5 grande y dorado (único texto permitido: el número 5). Medalla circular completa que ocupa todo el cuadro. Dentro del círculo, agua azul-turquesa con degradado suave y burbujas, como en pez_guia.png. Fuera del círculo, blanco liso #FFFFFF. Sin texto ni letras salvo lo que se indique. Cuadrado 1024×1024.
```

### `insignia_cinco_retos_diarios.png`
Adjunta: `pez_guia.png`

```text
Ilustración infantil 2D para app de reciclaje (niños de 3 a 7 años). Estilo suave, con degradados sutiles y sombreado limpio, formas redondeadas, sin contorno negro grueso, colores brillantes y amables (turquesa, verde, naranja, amarillo), ojos grandes y brillantes. Mismo estilo y mismos personajes que las imágenes de referencia adjuntas. Insignia «5 retos diarios»: calendario con 5 casillas marcadas con visto verde, un sol sonriente detrás y el PEZ asomando por un lado. PEZ: pez dorado-naranja con birrete azul de graduación con borla amarilla y medalla con brújula (el de pez_guia.png). Medalla circular completa que ocupa todo el cuadro. Dentro del círculo, agua azul-turquesa con degradado suave y burbujas, como en pez_guia.png. Fuera del círculo, blanco liso #FFFFFF. Sin texto ni letras salvo lo que se indique. Cuadrado 1024×1024.
```

### `insignia_pulpo_ordenado.png`
Adjunta: la imagen aprobada del pulpo y `pez_guia.png`

```text
Ilustración infantil 2D para app de reciclaje (niños de 3 a 7 años). Estilo suave, con degradados sutiles y sombreado limpio, formas redondeadas, sin contorno negro grueso, colores brillantes y amables (turquesa, verde, naranja, amarillo), ojos grandes y brillantes. Mismo estilo y mismos personajes que las imágenes de referencia adjuntas. Insignia «Pulpo ordenado»: PULPO: pulpo morado, cabeza redonda, ojos grandes, mejillas rosadas, ocho tentáculos cortos y rizados, corbatín negro (el diseño aprobado, adjunto). PULPO poniendo residuos en 3 canecas alineadas, una blanca, una negra y una verde. Medalla circular completa que ocupa todo el cuadro. Dentro del círculo, agua azul-turquesa con degradado suave y burbujas, como en pez_guia.png. Fuera del círculo, blanco liso #FFFFFF. Sin texto ni letras salvo lo que se indique. Cuadrado 1024×1024.
```

### `insignia_agua_cristalina.png`
Adjunta: `pez_guia.png`

```text
Ilustración infantil 2D para app de reciclaje (niños de 3 a 7 años). Estilo suave, con degradados sutiles y sombreado limpio, formas redondeadas, sin contorno negro grueso, colores brillantes y amables (turquesa, verde, naranja, amarillo), ojos grandes y brillantes. Mismo estilo y mismos personajes que las imágenes de referencia adjuntas. Insignia «Agua cristalina»: una gota de agua muy clara, con destellos, y un PEZ nadando dentro. PEZ: pez dorado-naranja con birrete azul de graduación con borla amarilla y medalla con brújula (el de pez_guia.png). Medalla circular completa que ocupa todo el cuadro. Dentro del círculo, agua azul-turquesa con degradado suave y burbujas, como en pez_guia.png. Fuera del círculo, blanco liso #FFFFFF. Sin texto ni letras salvo lo que se indique. Cuadrado 1024×1024.
```

### `insignia_coral_feliz.png`
Adjunta: `pez_guia.png`

```text
Ilustración infantil 2D para app de reciclaje (niños de 3 a 7 años). Estilo suave, con degradados sutiles y sombreado limpio, formas redondeadas, sin contorno negro grueso, colores brillantes y amables (turquesa, verde, naranja, amarillo), ojos grandes y brillantes. Mismo estilo y mismos personajes que las imágenes de referencia adjuntas. Insignia «Coral feliz»: coral naranja y rosa con carita sonriente (ojos y mejillas) y dos pececitos alrededor. Medalla circular completa que ocupa todo el cuadro. Dentro del círculo, agua azul-turquesa con degradado suave y burbujas, como en pez_guia.png. Fuera del círculo, blanco liso #FFFFFF. Sin texto ni letras salvo lo que se indique. Cuadrado 1024×1024.
```

### `insignia_guardian_del_mar.png`
Adjunta: `pez_guia.png`, `amiga_tortuga.png` y la imagen aprobada del pulpo

```text
Ilustración infantil 2D para app de reciclaje (niños de 3 a 7 años). Estilo suave, con degradados sutiles y sombreado limpio, formas redondeadas, sin contorno negro grueso, colores brillantes y amables (turquesa, verde, naranja, amarillo), ojos grandes y brillantes. Mismo estilo y mismos personajes que las imágenes de referencia adjuntas. Insignia «Guardián del mar», la más especial: PEZ: pez dorado-naranja con birrete azul de graduación con borla amarilla y medalla con brújula (el de pez_guia.png). TORTUGA: tortuga turquesa con capa verde, camiseta blanca con símbolo de reciclaje verde y pañuelo verde anudado al cuello (la de amiga_tortuga.png), cuerpo completo con caparazón. PULPO: pulpo morado, cabeza redonda, ojos grandes, mejillas rosadas, ocho tentáculos cortos y rizados, corbatín negro (el diseño aprobado, adjunto). Los tres juntos detrás de un escudo dorado con una ola y una estrella. Medalla circular completa que ocupa todo el cuadro. Dentro del círculo, agua azul-turquesa con degradado suave y burbujas, como en pez_guia.png. Fuera del círculo, blanco liso #FFFFFF. Sin texto ni letras salvo lo que se indique. Cuadrado 1024×1024.
```

---

## 3. Acuario por nivel (medalla circular, se ve a 150 dp)

### `acuario_nivel_1.png`
Adjunta: `pez_guia.png`

```text
Ilustración infantil 2D para app de reciclaje (niños de 3 a 7 años). Estilo suave, con degradados sutiles y sombreado limpio, formas redondeadas, sin contorno negro grueso, colores brillantes y amables (turquesa, verde, naranja, amarillo), ojos grandes y brillantes. Mismo estilo y mismos personajes que las imágenes de referencia adjuntas. Acuario nivel 1: agua turbia café-verdosa con basura flotando (una botella, una bolsa), arena oscura y ningún animal. Medalla circular completa que ocupa todo el cuadro. Dentro del círculo, agua azul-turquesa con degradado suave y burbujas, como en pez_guia.png. Fuera del círculo, blanco liso #FFFFFF. Sin texto ni letras salvo lo que se indique. Cuadrado 1024×1024.
```

### `acuario_nivel_2.png`
Adjunta: `pez_guia.png`

```text
Ilustración infantil 2D para app de reciclaje (niños de 3 a 7 años). Estilo suave, con degradados sutiles y sombreado limpio, formas redondeadas, sin contorno negro grueso, colores brillantes y amables (turquesa, verde, naranja, amarillo), ojos grandes y brillantes. Mismo estilo y mismos personajes que las imágenes de referencia adjuntas. Acuario nivel 2: agua algo más clara, un PEZ pequeño asomando entre algas, poca basura. PEZ: pez dorado-naranja con birrete azul de graduación con borla amarilla y medalla con brújula (el de pez_guia.png). Medalla circular completa que ocupa todo el cuadro. Dentro del círculo, agua azul-turquesa con degradado suave y burbujas, como en pez_guia.png. Fuera del círculo, blanco liso #FFFFFF. Sin texto ni letras salvo lo que se indique. Cuadrado 1024×1024.
```

### `acuario_nivel_3.png`
Adjunta: `pez_guia.png` y `amiga_tortuga.png`

```text
Ilustración infantil 2D para app de reciclaje (niños de 3 a 7 años). Estilo suave, con degradados sutiles y sombreado limpio, formas redondeadas, sin contorno negro grueso, colores brillantes y amables (turquesa, verde, naranja, amarillo), ojos grandes y brillantes. Mismo estilo y mismos personajes que las imágenes de referencia adjuntas. Acuario nivel 3: agua clara y azul, un PEZ y una TORTUGA, corales, sin basura. PEZ: pez dorado-naranja con birrete azul de graduación con borla amarilla y medalla con brújula (el de pez_guia.png). TORTUGA: tortuga turquesa con capa verde, camiseta blanca con símbolo de reciclaje verde y pañuelo verde anudado al cuello (la de amiga_tortuga.png), cuerpo completo con caparazón. Medalla circular completa que ocupa todo el cuadro. Dentro del círculo, agua azul-turquesa con degradado suave y burbujas, como en pez_guia.png. Fuera del círculo, blanco liso #FFFFFF. Sin texto ni letras salvo lo que se indique. Cuadrado 1024×1024.
```

### `acuario_nivel_4.png`
Adjunta: `pez_guia.png`, `amiga_tortuga.png` y la imagen aprobada del pulpo

```text
Ilustración infantil 2D para app de reciclaje (niños de 3 a 7 años). Estilo suave, con degradados sutiles y sombreado limpio, formas redondeadas, sin contorno negro grueso, colores brillantes y amables (turquesa, verde, naranja, amarillo), ojos grandes y brillantes. Mismo estilo y mismos personajes que las imágenes de referencia adjuntas. Acuario nivel 4: agua cristalina turquesa brillante, un PEZ, una TORTUGA y un PULPO, corales coloridos, rayos de luz desde arriba. PEZ: pez dorado-naranja con birrete azul de graduación con borla amarilla y medalla con brújula (el de pez_guia.png). TORTUGA: tortuga turquesa con capa verde, camiseta blanca con símbolo de reciclaje verde y pañuelo verde anudado al cuello (la de amiga_tortuga.png), cuerpo completo con caparazón. PULPO: pulpo morado, cabeza redonda, ojos grandes, mejillas rosadas, ocho tentáculos cortos y rizados, corbatín negro (el diseño aprobado, adjunto). Medalla circular completa que ocupa todo el cuadro. Dentro del círculo, agua azul-turquesa con degradado suave y burbujas, como en pez_guia.png. Fuera del círculo, blanco liso #FFFFFF. Sin texto ni letras salvo lo que se indique. Cuadrado 1024×1024.
```

---

## 4. Residuos (objeto solo, fondo blanco)

Referencia en todos: `residuo_botella.png`. Sin personajes, sin marcas ni letras.

### `residuo_papel.png`
Adjunta: `residuo_botella.png`

```text
Ilustración infantil 2D para app de reciclaje (niños de 3 a 7 años). Estilo suave, con degradados sutiles y sombreado limpio, formas redondeadas, sin contorno negro grueso, colores brillantes y amables (turquesa, verde, naranja, amarillo), ojos grandes y brillantes. Mismo estilo y mismos personajes que las imágenes de referencia adjuntas. Objeto de reciclaje para un juego infantil: una hoja de cuaderno arrugada en bola. Un solo objeto, grande, de 3/4, sin personajes. Fondo blanco liso #FFFFFF, sin sombra en el piso, sin marco, sin texto ni letras salvo lo que se indique, elemento completo y centrado con margen, cuadrado 1024×1024.
```

### `residuo_carton.png`
Adjunta: `residuo_botella.png`

```text
Ilustración infantil 2D para app de reciclaje (niños de 3 a 7 años). Estilo suave, con degradados sutiles y sombreado limpio, formas redondeadas, sin contorno negro grueso, colores brillantes y amables (turquesa, verde, naranja, amarillo), ojos grandes y brillantes. Mismo estilo y mismos personajes que las imágenes de referencia adjuntas. Objeto de reciclaje para un juego infantil: una caja de cartón aplastada con un pedazo de cinta. Un solo objeto, grande, de 3/4, sin personajes. Fondo blanco liso #FFFFFF, sin sombra en el piso, sin marco, sin texto ni letras salvo lo que se indique, elemento completo y centrado con margen, cuadrado 1024×1024.
```

### `residuo_cascara.png`
Adjunta: `residuo_botella.png`

```text
Ilustración infantil 2D para app de reciclaje (niños de 3 a 7 años). Estilo suave, con degradados sutiles y sombreado limpio, formas redondeadas, sin contorno negro grueso, colores brillantes y amables (turquesa, verde, naranja, amarillo), ojos grandes y brillantes. Mismo estilo y mismos personajes que las imágenes de referencia adjuntas. Objeto de reciclaje para un juego infantil: una cáscara de banano abierta. Un solo objeto, grande, de 3/4, sin personajes. Fondo blanco liso #FFFFFF, sin sombra en el piso, sin marco, sin texto ni letras salvo lo que se indique, elemento completo y centrado con margen, cuadrado 1024×1024.
```

### `residuo_servilleta.png`
Adjunta: `residuo_botella.png`

```text
Ilustración infantil 2D para app de reciclaje (niños de 3 a 7 años). Estilo suave, con degradados sutiles y sombreado limpio, formas redondeadas, sin contorno negro grueso, colores brillantes y amables (turquesa, verde, naranja, amarillo), ojos grandes y brillantes. Mismo estilo y mismos personajes que las imágenes de referencia adjuntas. Objeto de reciclaje para un juego infantil: una servilleta de papel usada, arrugada, con una mancha de salsa. Un solo objeto, grande, de 3/4, sin personajes. Fondo blanco liso #FFFFFF, sin sombra en el piso, sin marco, sin texto ni letras salvo lo que se indique, elemento completo y centrado con margen, cuadrado 1024×1024.
```

### `residuo_empaque.png`
Adjunta: `residuo_botella.png`

```text
Ilustración infantil 2D para app de reciclaje (niños de 3 a 7 años). Estilo suave, con degradados sutiles y sombreado limpio, formas redondeadas, sin contorno negro grueso, colores brillantes y amables (turquesa, verde, naranja, amarillo), ojos grandes y brillantes. Mismo estilo y mismos personajes que las imágenes de referencia adjuntas. Objeto de reciclaje para un juego infantil: un empaque metalizado de snack, arrugado, plateado con colores, sin marca ni texto. Un solo objeto, grande, de 3/4, sin personajes. Fondo blanco liso #FFFFFF, sin sombra en el piso, sin marco, sin texto ni letras salvo lo que se indique, elemento completo y centrado con margen, cuadrado 1024×1024.
```

### `residuo_lata.png` · opcional, la actual es de baja resolución
Adjunta: `residuo_botella.png`

```text
Ilustración infantil 2D para app de reciclaje (niños de 3 a 7 años). Estilo suave, con degradados sutiles y sombreado limpio, formas redondeadas, sin contorno negro grueso, colores brillantes y amables (turquesa, verde, naranja, amarillo), ojos grandes y brillantes. Mismo estilo y mismos personajes que las imágenes de referencia adjuntas. Objeto de reciclaje para un juego infantil: una lata de jugo, nueva y brillante, sin marca ni texto. Un solo objeto, grande, de 3/4, sin personajes. Fondo blanco liso #FFFFFF, sin sombra en el piso, sin marco, sin texto ni letras salvo lo que se indique, elemento completo y centrado con margen, cuadrado 1024×1024.
```

---

## 5. Lo demás

### `logo_reciclakids.png` · se ve a 200 dp, la app escribe el nombre debajo
Adjunta: `pez_guia.png`

```text
Ilustración infantil 2D para app de reciclaje (niños de 3 a 7 años). Estilo suave, con degradados sutiles y sombreado limpio, formas redondeadas, sin contorno negro grueso, colores brillantes y amables (turquesa, verde, naranja, amarillo), ojos grandes y brillantes. Mismo estilo y mismos personajes que las imágenes de referencia adjuntas. Emblema redondo sin texto: tres flechas de reciclaje formando una ola, con un pececito dorado saltando en el centro. Colores turquesa, verde y naranja. Se ve a 200 dp. Fondo blanco liso #FFFFFF, sin sombra en el piso, sin marco, sin texto ni letras salvo lo que se indique, elemento completo y centrado con margen, cuadrado 1024×1024.
```

### `ic_launcher_fondo.png`
Adjunta: `pez_guia.png`

```text
Fondo para ícono de app: degradado suave turquesa a azul con burbujas translúcidas, sin personajes, sin texto, de borde a borde, sin marco ni esquinas redondeadas, cuadrado 1024×1024.
```

Este es el único que **no** lleva fondo blanco.


### `ic_launcher_frente.png`
Adjunta: `pez_guia.png`

```text
Ilustración infantil 2D para app de reciclaje (niños de 3 a 7 años). Estilo suave, con degradados sutiles y sombreado limpio, formas redondeadas, sin contorno negro grueso, colores brillantes y amables (turquesa, verde, naranja, amarillo), ojos grandes y brillantes. Mismo estilo y mismos personajes que las imágenes de referencia adjuntas. Primer plano de ícono de app: la cara del PEZ guía (o el emblema del logo), simple y grande, centrado y ocupando como máximo el 60 % del centro del cuadro. PEZ: pez dorado-naranja con birrete azul de graduación con borla amarilla y medalla con brújula (el de pez_guia.png). Sin círculo de fondo. Fondo blanco liso #FFFFFF, sin sombra en el piso, sin marco, sin texto ni letras salvo lo que se indique, elemento completo y centrado con margen, cuadrado 1024×1024.
```

### `mano_arrastre.png` · se ve a 92 dp
Adjunta: `pez_guia.png`

```text
Ilustración infantil 2D para app de reciclaje (niños de 3 a 7 años). Estilo suave, con degradados sutiles y sombreado limpio, formas redondeadas, sin contorno negro grueso, colores brillantes y amables (turquesa, verde, naranja, amarillo), ojos grandes y brillantes. Mismo estilo y mismos personajes que las imágenes de referencia adjuntas. Una mano de niño de caricatura haciendo el gesto de arrastrar, con el pulgar y el índice pellizcando algo invisible. Solo la mano y la muñeca, sin brazo. Se ve a 92 dp. Fondo blanco liso #FFFFFF, sin sombra en el piso, sin marco, sin texto ni letras salvo lo que se indique, elemento completo y centrado con margen, cuadrado 1024×1024.
```

### `vacio_acuario.png` · se ve a 150 dp
Adjunta: `pez_guia.png`

```text
Ilustración infantil 2D para app de reciclaje (niños de 3 a 7 años). Estilo suave, con degradados sutiles y sombreado limpio, formas redondeadas, sin contorno negro grueso, colores brillantes y amables (turquesa, verde, naranja, amarillo), ojos grandes y brillantes. Mismo estilo y mismos personajes que las imágenes de referencia adjuntas. Estado vacío: una pecera de vidrio vacía con agua limpia y dos burbujas, sin peces. Más simple y plana que los personajes, amable, se ve a 150 dp. Fondo blanco liso #FFFFFF, sin sombra en el piso, sin marco, sin texto ni letras salvo lo que se indique, elemento completo y centrado con margen, cuadrado 1024×1024.
```

### `vacio_biblioteca.png` · se ve a 150 dp
Adjunta: `amiga_tortuga.png`

```text
Ilustración infantil 2D para app de reciclaje (niños de 3 a 7 años). Estilo suave, con degradados sutiles y sombreado limpio, formas redondeadas, sin contorno negro grueso, colores brillantes y amables (turquesa, verde, naranja, amarillo), ojos grandes y brillantes. Mismo estilo y mismos personajes que las imágenes de referencia adjuntas. Estado vacío: una caja de archivo abierta y vacía, con la TORTUGA asomando curiosa. TORTUGA: tortuga turquesa con capa verde, camiseta blanca con símbolo de reciclaje verde y pañuelo verde anudado al cuello (la de amiga_tortuga.png), cuerpo completo con caparazón. Más simple y plana que los personajes, amable, se ve a 150 dp. Fondo blanco liso #FFFFFF, sin sombra en el piso, sin marco, sin texto ni letras salvo lo que se indique, elemento completo y centrado con margen, cuadrado 1024×1024.
```

### `vacio_grupo.png` · se ve a 150 dp
Adjunta: `pez_guia.png`

```text
Ilustración infantil 2D para app de reciclaje (niños de 3 a 7 años). Estilo suave, con degradados sutiles y sombreado limpio, formas redondeadas, sin contorno negro grueso, colores brillantes y amables (turquesa, verde, naranja, amarillo), ojos grandes y brillantes. Mismo estilo y mismos personajes que las imágenes de referencia adjuntas. Estado vacío: tres sillitas de colegio vacías y el PEZ mirando alrededor. PEZ: pez dorado-naranja con birrete azul de graduación con borla amarilla y medalla con brújula (el de pez_guia.png). Sin círculo de fondo. Más simple y plana que los personajes, amable, se ve a 150 dp. Fondo blanco liso #FFFFFF, sin sombra en el piso, sin marco, sin texto ni letras salvo lo que se indique, elemento completo y centrado con margen, cuadrado 1024×1024.
```

### `vacio_acuario_nuevo.png` · se ve a 150 dp
Adjunta: `pez_guia.png`

```text
Ilustración infantil 2D para app de reciclaje (niños de 3 a 7 años). Estilo suave, con degradados sutiles y sombreado limpio, formas redondeadas, sin contorno negro grueso, colores brillantes y amables (turquesa, verde, naranja, amarillo), ojos grandes y brillantes. Mismo estilo y mismos personajes que las imágenes de referencia adjuntas. Estado vacío: una pecera pequeña recién estrenada, con un huevito de coral y un pececito recién nacido. Más simple y plana que los personajes, amable, se ve a 150 dp. Fondo blanco liso #FFFFFF, sin sombra en el piso, sin marco, sin texto ni letras salvo lo que se indique, elemento completo y centrado con margen, cuadrado 1024×1024.
```

---

## Estado (21 oct)

Marcadas = ya están en el proyecto y conectadas. Las demás se pueden generar con el prompt de arriba.

- [ ] `tortuga_flota.png`: entregada SIN capa: regenerar con «capa verde» (el prompt ya la pide)
- [ ] `tortuga_saluda.png`: entregada SIN capa: regenerar con «capa verde» (el prompt ya la pide)
- [x] `tortuga_celebra.png`
- [x] `tortuga_duerme.png`
- [x] `pez_flota.png`
- [x] `pez_saluda.png`
- [x] `pez_celebra.png`
- [x] `pez_duerme.png`
- [x] `pulpo_flota.png`
- [x] `pulpo_saluda.png`
- [x] `pulpo_celebra.png`
- [x] `pulpo_duerme.png`
- [x] `pulpo_casi.png`
- [x] `insignia_rapido_como_pez.png`
- [x] `insignia_racha_de_cinco.png`
- [x] `insignia_cinco_retos_diarios.png`
- [x] `insignia_pulpo_ordenado.png`
- [x] `insignia_agua_cristalina.png`
- [x] `insignia_coral_feliz.png`
- [x] `insignia_guardian_del_mar.png`
- [x] `acuario_nivel_1.png`
- [x] `acuario_nivel_2.png`
- [x] `acuario_nivel_3.png`
- [x] `acuario_nivel_4.png`
- [x] `residuo_papel.png`
- [x] `residuo_carton.png`
- [x] `residuo_cascara.png`
- [x] `residuo_servilleta.png`
- [x] `residuo_empaque.png`
- [x] `residuo_lata.png`
- [x] `logo_reciclakids.png`
- [ ] `ic_launcher_frente.png`: provisional: hoy es el emblema del logo; la que llegó con este nombre era el fondo
- [x] `mano_arrastre.png`
- [ ] `vacio_biblioteca.png`: falta: la que llegó con este nombre era la pecera vacía y se usa como vacio_acuario
- [x] `vacio_grupo.png`
- [x] `vacio_acuario_nuevo.png`
- [x] `ic_launcher_fondo.png` y `vacio_acuario.png` (llegaron con otro nombre y se renombraron)
