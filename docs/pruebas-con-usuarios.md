# Protocolo de pruebas con usuarios reales

Prueba de uso de ReciclaKids con las tres personas para las que existe: un niño o niña, su acudiente y una docente. Se hace como mínimo con una persona de cada rol, sobre la versión de `main` instalada en un teléfono y con el backend desplegado.

## Objetivo

Comprobar que cada rol puede hacer sus tareas principales sin ayuda, y encontrar lo que confunde antes de la entrega. No se evalúa a las personas: se evalúa la app.

## Participantes

| Rol | Perfil | Duración |
|---|---|---|
| Niño o niña | Entre 4 y 6 años, siempre acompañado por su acudiente | 15 min como máximo |
| Acudiente | Adulto responsable del niño que participa | 20 min |
| Docente | Docente de preescolar o de transición | 25 min |

## Consentimiento y datos del menor (Ley 1581 de 2012)

- Antes de empezar, el acudiente firma una autorización por escrito para que el niño participe. Puede detener la sesión cuando quiera.
- No se graba la cara ni la voz del niño. Si se toma una foto, es de la pantalla del teléfono.
- En la app solo se escribe el nombre y la inicial del apellido del niño, como en el uso normal.
- Al terminar, la docente quita al niño del grupo de prueba para que se borren sus datos.

## Preparación

- [ ] Teléfono con la app de `main` instalada y con conexión.
- [ ] Backend desplegado y funcionando (ver [despliegue](despliegue.md)).
- [ ] Grupo de prueba de la docente creado y un reto publicado para el día de la prueba.
- [ ] Una copia impresa de la hoja de registro por participante.
- [ ] Una persona del equipo guía las tareas y otra toma notas. Quien guía no ayuda a menos que la persona se quede atascada más de un minuto.

## Tareas

La columna «Verifica» enlaza cada tarea con el hallazgo de la auditoría que comprueba, cuando aplica.

### Docente

| # | Tarea | Se cumple si… | Verifica |
|---|---|---|---|
| D1 | Crear su cuenta | Llega al tablero | |
| D2 | Tocar «Guardar» en «Agregar niño» sin escribir nada | Ve qué campos faltan | #8 |
| D3 | Agregar al niño participante | Ve el código de vinculación | |
| D4 | Agregar otra vez un niño con el mismo nombre | La app avisa que ya está en el grupo | #2 |
| D5 | Crear un reto programado para otro día y buscarlo en la biblioteca | Lee la fecha con el mes | #10 |
| D6 | Editar su perfil y volver con el botón atrás del teléfono | Regresa a la cuenta | #5 |
| D7 | Cerrar sesión | La app pide confirmación | #4 |

### Acudiente

| # | Tarea | Se cumple si… | Verifica |
|---|---|---|---|
| A1 | Crear su cuenta y vincularse con el código del niño | Ve el inicio con el nombre del niño | |
| A2 | Configurar 15 minutos, dificultad difícil y correos de logros desactivados | Ve los tres ajustes guardados | |
| A3 | Cambiar solo el reporte semanal y volver a entrar al control parental | Los ajustes de A2 siguen igual | #11 |
| A4 | Descargar los datos del niño | Recibe un archivo con las partidas y las insignias | #6 |
| A5 | Cambiar la contraseña desde la cuenta | Lo hace con la contraseña actual, sin correo | #7 |

### Niño o niña (con su acudiente al lado)

| # | Tarea | Se cumple si… | Verifica |
|---|---|---|---|
| N1 | Entrar con el código del reto y elegir avatar | Llega al acuario | |
| N2 | Jugar el reto del día hasta el final | Ve la pantalla de resultado | |
| N3 | Ver la insignia ganada, si sale | La abre y vuelve al acuario | |
| N4 | Volver a tocar el reto ya terminado | La app avisa que ya lo completó y ofrece practicar | #9 |
| N5 | Poner el teléfono en modo avión y mirar el acuario | La pastilla dice «Guardado aquí» | #3 |

## Qué se registra

Por cada tarea:

- **Resultado:** sin ayuda, con ayuda o no la completó.
- **Tiempo** aproximado.
- **Errores y dudas:** dónde tocó mal o qué preguntó.
- **Comentarios** textuales que valga la pena guardar.

Al final de la sesión:

- **Adultos:** el cuestionario SUS (System Usability Scale): diez afirmaciones que se responden de 1 (muy en desacuerdo) a 5 (muy de acuerdo). Un puntaje de 68 o más se considera aceptable.
- **Niño:** una escala de cinco caritas, de muy triste a muy feliz, con la pregunta «¿Te gustó jugar?».

## Hoja de registro

| Rol | Tarea | Resultado | Tiempo | Errores y dudas | Comentarios |
|---|---|---|---|---|---|
| | | | | | |

## Después de la sesión

1. Pasar las hojas a un resumen en `docs/` con la fecha y el rol (sin nombres de los participantes).
2. Cada problema encontrado se vuelve un hallazgo con pasos para reproducirlo y su prioridad.
3. Marcar en TestRail los casos de prueba que cubrió la sesión.
