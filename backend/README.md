# Backend de ReciclaKids

API REST en **Kotlin + Ktor** con **PostgreSQL** (ADR-02, ADR-03, ADR-07). Atiende los tres modos
de la app: el niño entra con el código del reto, la docente maneja su grupo y sus retos, y el
acudiente sigue el progreso de su hijo o hija. Los correos a acudientes salen por **Amazon SES**
(ADR-10). El despliegue en Google Cloud está en [docs/despliegue.md](../docs/despliegue.md).

## Correr en local

Con Docker (backend + PostgreSQL):

```bash
docker compose up --build
```

La API queda en `http://localhost:8080` (desde el emulador de Android, `http://10.0.2.2:8080`).
Para correr solo el servidor contra una base propia: `./gradlew run`, con las variables de abajo.

## Pruebas

```bash
./gradlew test
```

Las pruebas de integración levantan PostgreSQL 17 con Testcontainers, así que necesitan Docker
encendido. Cubren el recorrido completo (docente → niño → acudiente), los permisos por rol, el
vencimiento de códigos, la rotación de sesiones, la recuperación de contraseña y los correos.

## Variables de entorno

| Variable | Para qué | Valor por defecto (solo desarrollo) |
|---|---|---|
| `RECICLAKIDS_ENTORNO` | `produccion` exige valores seguros en todo lo demás | `desarrollo` |
| `RECICLAKIDS_URL_PUBLICA` | Dirección pública del backend, para los enlaces de los correos. HTTPS en producción | `http://localhost:8080` |
| `PORT` | Puerto del servidor (Cloud Run lo pone solo) | `8080` |
| `DATABASE_URL` | URL JDBC de PostgreSQL | `jdbc:postgresql://localhost:5432/reciclakids` |
| `DATABASE_USER` / `DATABASE_PASSWORD` | Credenciales de la base | `reciclakids` / `reciclakids` |
| `DATABASE_MAX_POOL` | Conexiones máximas del pool | `5` |
| `JWT_SECRET` | Firma de los tokens. Al menos 32 caracteres en producción | valor de ejemplo |
| `CORREO_PROVEEDOR` | `ses` envía por Amazon SES; `memoria` no envía nada | `memoria` |
| `CORREO_REMITENTE` | Remitente verificado en SES | `ReciclaKids <no-responder@reciclakids.app>` |
| `AWS_REGION`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` | Credenciales de SES | `us-east-1` |
| `TAREAS_CLAVE` | Clave que manda Cloud Scheduler en `X-Tareas-Clave`. Vacía apaga las tareas | vacía |
| `RECICLAKIDS_VISTA_PREVIA_CORREOS` | Vista previa de las plantillas en `/correos/vista-previa/...`. Apagada en producción | `true` |

En producción el servidor **no arranca** si el secreto JWT es el de ejemplo, si la URL pública no
es HTTPS, si la vista previa de correos está encendida o si los correos no salen por SES.

## La API (`/api/v1`)

Todas las respuestas son JSON. Los errores tienen siempre la forma
`{"error": "codigo_estable", "mensaje": "Texto en español"}`; la app decide por `error`.

### Autenticación (adultos)

| Método y ruta | Qué hace |
|---|---|
| `POST /auth/registro/docente` | Crea la cuenta y su grupo. 201, o 409 `correo_en_uso` |
| `POST /auth/registro/acudiente` | Crea la cuenta y la vincula al niño del `codigoVinculacion`. Exige `autorizaDatosMenor: true` (UI-07). 422 `codigo_invalido` |
| `POST /auth/sesion` | Devuelve `tokenAcceso` (1 h), `tokenRefresco` (30 días) y la cuenta con su rol |
| `POST /auth/refresco` | Cambia el token de refresco por uno nuevo. Reusar uno viejo cierra todas las sesiones |
| `POST /auth/cierre` | Cierra la sesión del token de refresco |
| `POST /auth/recuperacion` | Envía el enlace para crear contraseña nueva. Siempre 202 |
| `GET /cuenta` | La cuenta del token: la app la usa para abrir Docente o Padres (ADR-08) |

Las rutas con cuenta llevan `Authorization: Bearer <tokenAcceso>`. Hay límite de intentos por IP en
el acceso (10 por minuto) y en el código del reto (60 por minuto).

### Modo Niño

| Método y ruta | Qué hace |
|---|---|
| `GET /nino/retos/{codigo}` | El reto de hoy y los niños del grupo. 404 `codigo_invalido`, 410 `codigo_vencido` |
| `POST /nino/sesiones` | `{codigo, ninoId}` → token del niño (7 días, para sincronizar lo jugado sin conexión) |
| `POST /nino/partidas` | Lote de partidas con sus intentos. Idempotente por `idLocal`. El servidor vuelve a evaluar cada intento, lleva la racha entre retos y devuelve las insignias nuevas |
| `GET /nino/progreso` | Retos completados, racha, insignias y límite diario de juego |

### Modo Docente (token de docente)

| Método y ruta | Qué hace |
|---|---|
| `GET /docente/tablero` | UI-21 |
| `GET /docente/retos`, `GET /docente/retos/{id}` | Biblioteca (UI-23) |
| `POST /docente/retos` | Publica hoy (con código) o programa para otro día. Con `idExistente` edita un programado |
| `GET /docente/retos/{id}/ingresos` | Cuántos niños entraron (la app la consulta cada pocos segundos en UI-24) |
| `GET /docente/grupo/hoy` | UI-25 |
| `POST /docente/ninos` | Agrega un niño (nombre + inicial del apellido); devuelve su código de vinculación |
| `GET /docente/ninos/{id}`, `DELETE /docente/ninos/{id}` | Detalle (UI-27) y quitar del grupo con todos sus datos |
| `POST /docente/ninos/{id}/codigo-vinculacion` | Código nuevo para ese niño |
| `GET /docente/reportes/semanal`, `POST /docente/reportes/semanal/envio` | UI-26 y envío a acudientes |
| `GET/PUT /docente/perfil`, `PUT /docente/avisos` | UI-28 |
| `DELETE /docente/grupo/datos` | Borra resultados, insignias y correos del grupo |

### Modo Padres (token de acudiente)

| Método y ruta | Qué hace |
|---|---|
| `GET /acudiente/hijos` | Hijos vinculados |
| `POST /acudiente/vinculaciones` | Vincula otro hijo con su código |
| `GET /acudiente/hijos/{id}/inicio` · `/reporte?semana=` · `/logros` · `/correos` | UI-29 a UI-32 |
| `GET/PUT /acudiente/hijos/{id}/control-parental` | UI-33 (lo comparten los acudientes del niño) |
| `GET /acudiente/hijos/{id}/privacidad`, `POST/DELETE .../privacidad/solicitud-eliminacion` | UI-34 |
| `GET/PUT /acudiente/perfil` | UI-35 |

Un acudiente solo ve a sus hijos: pedir otro niño responde 404 sin confirmar que existe.

### Fuera de `/api/v1`

| Ruta | Qué es |
|---|---|
| `GET /salud` | Para Cloud Run |
| `GET/POST /restablecer` | Página del enlace de recuperación de contraseña |
| `GET /correos/baja?token=` | «Dejar de recibir» de los correos, sin iniciar sesión |
| `POST /tareas/reporte-semanal` | La llama Cloud Scheduler los viernes con `X-Tareas-Clave` |
| `/correos/vista-previa/logro`, `/correos/vista-previa/reporte-semanal` | Plantillas UI-36 y UI-37 con datos ficticios (solo desarrollo); `?formato=texto` muestra la versión en texto |

## Estructura

Sigue el documento de arquitectura (C4, sección 2.4.1):

```
src/main/kotlin/com/reciclakids/
  routes/        rutas HTTP por modo
  services/      reglas: cuentas, niño (evaluación), docente, padres, notificaciones
  repositories/  acceso a datos con Exposed
  models/        tablas, catálogo del juego y DTO de la API
  plugins/       seguridad (JWT), serialización, errores, límites, monitoreo
  config/        configuración y conexión a la base
src/main/resources/db/migration/   esquema con Flyway
```

Datos de menores (Ley 1581 de 2012): del niño solo se guarda el nombre con la inicial del apellido,
la edad, el avatar y sus resultados. Los logs no llevan cuerpos, correos ni nombres.
