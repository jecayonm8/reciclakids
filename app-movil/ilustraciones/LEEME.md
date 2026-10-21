# Ilustraciones de ReciclaKids (parte 1 del frontend)

Solo ilustraciones. Animaciones, audio y pantallas/backend no se tocan aquí.
Los prompts para generar lo que falte están en [PROMPTS.md](PROMPTS.md), listos para copiar y pegar.

## Agregar una ilustración nueva (3 pasos)

1. **Recórtala** si trae fondo blanco (`herramientas/recortar_fondo.py`):
   - Personaje de IA (1024 px, con estrellitas o «z»): `python recortar_fondo.py entrada.png salida.png --ampliar 1 --recorte 0 --motas 0.0008 --maximo 640 --paleta`
   - Objeto: `... --objeto --ampliar 1 --recorte 0 --maximo 448 --paleta`
   - Medalla redonda (insignias, acuario, logo): `... --circulo --maximo 640` (sin `--paleta`: los degradados se escalonan)
2. **Cópiala** a `app/src/main/res/drawable-nodpi/` con nombre en minúsculas y guion bajo (`pulpo_flota.png`).
3. **Conéctala** donde hoy hay un marcador rayado (`MarcadorIlustracion`). Casi siempre es una línea en
   `app/src/main/java/com/reciclakids/ui/common/Ilustraciones.kt` (personajes, residuos, insignias, acuario, avatares).

> Al generar con IA pide: **fondo blanco liso, sin sombra en el piso, personaje completo, 1024 × 1024**.
> Mismo estilo que el pez guía y la tortuga con capa.

## Ya conectadas (se ven en la app)

| Archivo(s) en `drawable-nodpi` | Dónde sale |
|---|---|
| `logo_reciclakids` | Bienvenida (Splash) |
| `ic_launcher_fondo` + `ic_launcher_frente` | Ícono de la app (adaptable; el frente es el emblema del logo, provisional) |
| `pez_*`, `tortuga_*`, `pulpo_*` (flota, saluda, celebra, duerme) | `PersonajeMarino`. Hoy solo se ve la tortuga que saluda (selector de modo): el pez y el pulpo se verán cuando el personaje de cada caneca se anime (parte 2) |
| `pulpo_casi` | Mensaje «¡Casi!» del juego (52 dp) |
| `acuario_nivel_1` … `4` | Menú del acuario (UI-10), según el progreso |
| `insignia_*` (7) + `amiga_tortuga` | Colección de insignias (UI-30) e insignia obtenida (UI-29) |
| `residuo_papel`, `carton`, `cascara`, `servilleta`, `empaque`, `lata` (+ `botella`, `restos` anteriores) | Juego, vista previa del reto |
| `mano_arrastre` | Tutorial de arrastre (UI-11) |
| `vacio_grupo` | Mi grupo vacío |
| `vacio_acuario` | Tablero del docente vacío y código no disponible |
| `vacio_acuario_nuevo` | Inicio de padres sin datos |
| `avatar_nino` (pez guía) y `avatar_nina` (tortuga con capa) | «¿Quién eres?», menú, listas de docente y padres |
| `pez_cartero`, `pez_guia`, `peces_dormidos`, `caneca_blanca / negra / verde` | Recuperar contraseña, código del reto, tiempo terminado, juego (0.2 UI) |

**Tortuga:** la oficial es la de capa y camiseta de reciclaje (UI-29). La de gorra y botella del Canva se descartó.

**Avatares:** solo hay dos, un animal marino por cada uno: **niño = pez guía**, **niña = tortuga con capa**.
Todavía es **provisional quién recibe cuál**: el backend guarda un `avatar` por niño, pero no llega al modelo
de la app, así que hoy se reparte por el nombre (`Ilustraciones.avatarDe`: mismo nombre, mismo avatar).
Pendiente de «Pantallas y backend»: que la docente elija niño o niña al agregarlo y que el backend guarde
solo esas dos opciones (hoy `DocenteServicio.Avatares` trae 8 nombres). Al hacerlo, solo cambia `avatarDe`.

## Faltan o conviene regenerar

| Qué | Dónde | Nota |
|---|---|---|
| `vacio_biblioteca`: caja o estante vacío con la tortuga asomando | Biblioteca de retos vacía (`BibliotecaRetosScreen`) | Hoy marcador de texto. El archivo que llegó con ese nombre era la pecera vacía, por eso es `vacio_acuario` |
| `ic_launcher_frente` definitivo (cara del pez o emblema) | Ícono | El actual sale del logo; el fondo (`ic_launcher_fondo`) llegó con el nombre `ic_launcher_frente` |
| `tortuga_saluda` y `tortuga_flota` **con capa verde** | `PersonajeMarino` | Llegaron sin capa (se ve el caparazón); `celebra` y `duerme` sí la traen |
| `residuo_restos` en alta resolución | Juego | Sigue siendo el del Canva (≈ 80 × 99 px) |

## Calidad

- `insignia_guardian_del_mar` trae el texto «Guardián del mar» dibujado dentro de la imagen (no se traduce ni lo lee el lector de pantalla; la tarjeta ya dice el nombre aparte).
- `amiga_tortuga.png` viene **cortada a la cintura** del diseño UI-29: se apoya abajo del círculo para que el marco haga de corte.
- `vacio_grupo`: el respaldo de las sillitas quedó blanco; se ve bien sobre fondo claro (las pantallas del docente), no sobre oscuro.
- Peso: personajes, objetos y vacíos van como PNG de 256 colores (≈ 50 KB); las medallas, acuario y logo en color completo
  (≈ 400 KB). En total ≈ 6 MB.

## `por-conectar/`

Recortes listos del Canva que todavía no tienen sitio en las pantallas: corales, alga, arrecife, burbuja, estrella,
flecha punteada (guía del tutorial), sol, 4 hexágonos de insignia y la tortuga señalando. No están en `res/`
para no engordar el APK; cópialos a `drawable-nodpi/` cuando los uses.

## No se usó

Delfín y tortuga con caneca (prototipo de Canva descartado), la tortuga con gorra y botella del Canva y las seis caras
de niños (los avatares son ahora solo el pez y la tortuga), `por-conectar/tortuga_senala.png` (otra tortuga que no es la
de capa), canecas azul y amarilla (el código colombiano es blanca, negra y verde), fondos de océano (la app ya dibuja el suyo
con `FondoSubmarino`), la lata y los restos viejos del Canva (reemplazados) y las capturas de pantalla.
