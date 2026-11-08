# Cómo trabajamos en ReciclaKids

## Ramas
- `main` está protegida. Nadie hace push directo.
- Por cada tarea de Jira: `feature/RK-<numero>-descripcion-corta`
  Ejemplo: `feature/RK-23-pantalla-login-nino`

## Commits
- Mensaje claro y en español, referenciando la tarjeta de Jira:
  `RK-23: agrega validación de avatar en login del niño`

## Pull Requests
- Se abre PR de tu rama hacia `main` apenas termines la tarea (no esperar a tener "todo listo").
- Mínimo 1 aprobación de otro integrante antes de mergear.
- El PR debe pasar el pipeline de GitHub Actions (build + tests) antes de poder mergear.
- Al mergear, borrar la rama.

## Uso de inteligencia artificial
- Si un asistente de IA participó en un commit, el commit lo dice con la línea `Co-Authored-By` del asistente.
- Lo que escribe la IA pasa por la misma revisión que el código escrito a mano: lo lee y lo prueba quien hace el commit, y luego va por PR con aprobación y pipeline.
- Nunca se le pasan datos reales de niños, acudientes o docentes, ni secretos.
- Detalle en [docs/uso-de-ia.md](docs/uso-de-ia.md).

## Definición de terminado
Una historia no está "Done" en Jira hasta que:
- Tiene pruebas (unitarias mínimo).
- Pasó revisión de otro integrante.
- Está mergeada a `main` sin romper el pipeline.