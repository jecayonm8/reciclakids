# Ilustraciones de ReciclaKids (parte 1 del frontend)

Solo ilustraciones. Animaciones, audio y pantallas/backend no se tocan aquí.

## Agregar una ilustración nueva (3 pasos)

1. **Recórtala** si trae fondo blanco:
   `python herramientas/recortar_fondo.py entrada.png salida.png` (objetos: `--objeto`; con sombra gris: `--tol 24`).
2. **Cópiala** a `app/src/main/res/drawable-nodpi/` con nombre en minúsculas y guion bajo (`pulpo_flota.png`).
3. **Conéctala** donde hoy hay un marcador rayado (`MarcadorIlustracion`). Casi siempre es una línea en
   `app/src/main/java/com/reciclakids/ui/common/Ilustraciones.kt` (residuos, insignias, avatares) o en
   `PersonajeMarino.kt` (pez, tortuga, pulpo).

> Al generar con IA pide: **fondo blanco liso, sin sombra en el piso, personaje completo, 1024 × 1024**.
> Así el recorte sale limpio. Mismo estilo que el pez cartero y la tortuga con capa.

## Ya conectadas (se ven en la app)

| Archivo en `drawable-nodpi` | Origen | Dónde sale |
|---|---|---|
| `amiga_tortuga.png` | 0.2 UI (UI-29) | Insignia «Amiga tortuga»: UI-29 y colección UI-30 |
| `pez_cartero.png` | 0.2 UI (UI-08) | Recuperar contraseña |
| `pez_guia.png` | 0.2 UI (UI-09) | Ingreso del código del reto |
| `peces_dormidos.png` | 0.2 UI (UI-32) | Tiempo de juego terminado |
| `tortuga_mascota.png` | 0.2 UI (UI-29): la tortuga con capa, con la cintura difuminada | Selector de modo (personaje Tortuga) |
| `caneca_blanca / negra / verde.png` | 0.2 UI (UI-12) | Juego, tutorial (traen su rótulo dibujado) |
| `residuo_botella.png` | 0.2 UI | Juego, tutorial, vista previa del reto |
| `residuo_lata.png`, `residuo_restos.png` | Canva | Juego y vista previa (**baja resolución**) |
| `avatar_nino.png` (pez guía) y `avatar_nina.png` (tortuga con capa) | 0.2 UI | Selección «¿Quién eres?», menú, listas de docente y padres |

**Tortuga:** la oficial es la de capa y camiseta de reciclaje (UI-29). La de gorra y botella del Canva se descartó.

**Avatares:** solo hay dos, un animal marino por cada uno: **niño = pez guía**, **niña = tortuga con capa**.
Todavía es **provisional quién recibe cuál**: el backend guarda un `avatar` por niño, pero no llega al modelo
de la app, así que hoy se reparte por el nombre (`Ilustraciones.avatarDe`: mismo nombre, mismo avatar).
Pendiente de «Pantallas y backend»: que la docente elija niño o niña al agregarlo y que el backend guarde
solo esas dos opciones (hoy `DocenteServicio.Avatares` trae 8 nombres). Al hacerlo, solo cambia `avatarDe`.

## Faltan (generar con Claude Design u otra IA)

| # | Qué | Dónde se conecta | Nota |
|---|---|---|---|
| 1 | Logo «ReciclaKids» (200 dp) | `SplashScreen` | Hoy marcador «logo» |
| 2 | **Pez** solo, sin marco (4 poses: flota, saluda, celebra, duerme) | `PersonajeMarino` (Pez) | El de `pez_guia` y `pez_cartero` va dentro de un círculo |
| 3 | **Pulpo** (4 poses) + versión pequeña 52 dp para «¡Casi!» | `PersonajeMarino` (Pulpo) y `MensajeCasi` en `JuegoScreen` | No existe en ningún documento |
| 4 | **Tortuga con capa**, cuerpo completo (4 poses: flota, saluda, celebra, duerme) | `PersonajeMarino` (Tortuga) | Hoy sale el busto de UI-29, cortado a la cintura |
| 5 | Acuario por nivel: 1 agua turbia sin vida · 2 aparece el pez · 3 pez y tortuga · 4 pez, tortuga y pulpo | `MenuAcuarioScreen.personajesDelNivel` | 150 dp |
| 6 | Mano con gesto de arrastre | `TutorialArrastreScreen` | 92 dp |
| 7 | Residuos: papel, cartón, cáscara de banano, servilleta usada, empaque metalizado | `Ilustraciones.residuo` (ids `papel`, `carton`, `cascara`, `servilleta`, `empaque`) | El «papel» de Canva es un ícono de documento, no sirve |
| 8 | Insignias (7): Rápido como pez, Racha de 5, 5 retos diarios, Pulpo ordenado, Agua cristalina, Coral feliz, Guardián del mar | `Ilustraciones.insignia` | Hoy se ve el marcador. En `por-conectar/` hay 4 hexágonos genéricos de Canva por si se quiere algo provisional |
| 9 | Estados vacíos: grupo vacío, acuario vacío (tablero), biblioteca vacía, acuario nuevo (inicio de padres) | `EstadoVacioDocente` (150 dp) | Hoy marcadores de texto |
| 10 | Ícono de la app (launcher) | `res/mipmap-*` y `drawable/ic_launcher_*` | Sigue el robot verde de Android |

## Calidad: lo que conviene regenerar aunque ya se vea

- `amiga_tortuga.png` y `tortuga_mascota.png` vienen **cortadas a la cintura** en el diseño. La insignia se apoya abajo del círculo para que el marco haga de corte; la mascota tiene la cintura difuminada. Falta la versión de cuerpo completo (ver item 4).
- `residuo_lata.png` (≈60 × 99 px) y `residuo_restos.png` (≈80 × 99) son de baja resolución: se ven un poco blandos en pantallas grandes.
- `avatar_nino.png` y `avatar_nina.png` (384 px) salen del pez guía y del busto de la tortuga; si se genera arte nuevo de los personajes, rehacerlos.

## `por-conectar/`

Recortes listos del Canva que todavía no tienen sitio en las pantallas: corales, alga, arrecife, burbuja, estrella,
flecha punteada (guía del tutorial), sol, 4 hexágonos de insignia y la tortuga señalando. No están en `res/`
para no engordar el APK; cópialos a `drawable-nodpi/` cuando los uses.

## No se usó

Delfín y tortuga con caneca (prototipo de Canva descartado), la tortuga con gorra y botella del Canva y las seis caras
de niños (los avatares son ahora solo el pez y la tortuga), `por-conectar/tortuga_senala.png` (otra tortuga que no es la
de capa), canecas azul y amarilla (el código colombiano es blanca, negra y verde), fondos de océano (la app ya dibuja el suyo
con `FondoSubmarino`) y las capturas de pantalla.
