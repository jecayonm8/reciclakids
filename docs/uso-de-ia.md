# Declaración de uso de inteligencia artificial

El equipo de ReciclaKids usó un asistente de inteligencia artificial durante el desarrollo. Este documento dice cuál, para qué se usó, cómo se controló y qué no se le entregó.

## Herramienta

**Claude Code** (Anthropic): un asistente de programación que trabaja sobre el repositorio desde la terminal o la aplicación de escritorio.

## Cómo identificar lo que hizo

Cada commit en el que participó el asistente termina con una línea de coautoría:

```
Co-Authored-By: Claude <modelo> <noreply@anthropic.com>
```

Para listarlos:

```bash
git log --grep "Co-Authored-By: Claude"
```

Los commits sin esa línea los escribió el equipo sin el asistente.

## Para qué se usó

- Escribir y reorganizar código de la app (Kotlin y Jetpack Compose) y del backend (Ktor y PostgreSQL), a partir del diseño y de las tareas que definió el equipo.
- Escribir pruebas unitarias y de integración.
- Redactar documentación técnica (guías de despliegue, de servicios y secretos, y este documento) y descripciones de pull requests.
- Revisar hallazgos de pruebas y auditorías, ubicar la causa en el código y proponer un plan de corrección.

## Para qué no se usó

- **Decisiones de producto y pedagógicas.** El alcance, las reglas del juego, las insignias, el manejo de los datos del menor y las prioridades las decidió el equipo. El asistente proponía y el equipo elegía.
- **Dentro de la app.** ReciclaKids no usa inteligencia artificial: ni el juego, ni los reportes, ni los correos llaman a ningún modelo.

## Control humano

Nada de lo que produjo el asistente llegó a `main` sin pasar por lo mismo que el código escrito a mano:

1. Un integrante del equipo lo pidió, lo leyó y lo probó antes de hacer el commit.
2. Entró por pull request con al menos una aprobación de otro integrante.
3. Pasó el pipeline de GitHub Actions: compilación, pruebas y cobertura mínima del 70 % en la lógica de negocio del backend.

El equipo responde por todo el contenido del repositorio, lo haya escrito con o sin el asistente.

## Datos que no se compartieron

- Ningún dato real de niños, acudientes ni docentes. Se trabajó con datos de demostración (nombres ficticios y códigos de prueba).
- Ninguna clave, contraseña ni secreto de producción. Los secretos viven en Secret Manager y en GitHub Actions (ver [guía de servicios y secretos](guia-servicios-y-secretos.md)), no en el repositorio ni en las conversaciones con el asistente.
