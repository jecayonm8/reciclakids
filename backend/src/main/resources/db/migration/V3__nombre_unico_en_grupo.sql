-- Dos niños con el mismo nombre en un grupo no se distinguen en el aula (hallazgo 2 de la
-- auditoría). El servicio ya lo impide; el índice cubre dos altas al mismo tiempo. Los repetidos
-- que ya existan se numeran antes de crear el índice: «Salomé M.» y «Salomé M. 2».
WITH repetidos AS (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY grupo_id, LOWER(nombre) ORDER BY creado_en, id) AS orden
    FROM ninos
)
UPDATE ninos
SET nombre = ninos.nombre || ' ' || repetidos.orden
FROM repetidos
WHERE ninos.id = repetidos.id AND repetidos.orden > 1;

CREATE UNIQUE INDEX ninos_nombre_unico_en_grupo ON ninos (grupo_id, LOWER(nombre));
