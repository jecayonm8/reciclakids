-- «Recordatorio si no juega» de UI-33: el acudiente pide un aviso si su hijo lleva tres días
-- seguidos sin jugar. La app ya lo muestra; aquí solo se guarda la preferencia (el correo de ese
-- aviso todavía no se envía). Los controles que ya existían quedan apagados, como en la app.
ALTER TABLE controles_parentales ADD COLUMN recordatorio_sin_jugar BOOLEAN NOT NULL DEFAULT FALSE;
