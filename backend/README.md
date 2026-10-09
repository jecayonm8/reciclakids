# ktor-sample

This project was created using the [Ktor Project Generator](https://start.ktor.io).

Here are some useful links to get you started:
 * [Ktor Documentation](https://ktor.io/docs/home.html)
 * [Ktor GitHub page](https://github.com/ktorio/ktor)
 * [Ktor Slack chat](https://app.slack.com/client/T09229ZC6/C0A974TJ9). [Request an invite](https://surveys.jetbrains.com/s3/kotlin-slack-sign-up).


## Features
Here's a list of features included in this project:

| Name | Description |
|------|-------------|

## Building & Running
To build or run the project, use one of the following tasks:


| Task | Description |
|------|-------------|
| `./gradlew test`    | Run the tests     |
| `./gradlew build`   | Build the project |
| `./gradlew run`     | Run the server    |

If the server starts successfully, you'll see the following output:
```
2024-12-04 14:32:45.584 [main] INFO  Application - Application started in 0.303 seconds.
2024-12-04 14:32:45.682 [main] INFO  Application - Responding at http://0.0.0.0:8080
```

## Vista previa de los correos (UI-36 y UI-37)
Las plantillas viven en `com.reciclakids.services.notificaciones.correos`. Con `./gradlew run`
se ven en el navegador con datos ficticios:

| Ruta | Correo |
|------|--------|
| `/correos/vista-previa/logro` | UI-36 · Correo de logro |
| `/correos/vista-previa/reporte-semanal` | UI-37 · Correo de reporte semanal |

Agrega `?formato=texto` para ver el asunto y la alternativa en texto plano. La vista previa se
apaga con la variable de entorno `RECICLAKIDS_VISTA_PREVIA_CORREOS=false` (hazlo en producción).
