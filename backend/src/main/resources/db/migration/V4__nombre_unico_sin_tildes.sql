-- El servicio ya trata «Salomé M.» y «Salome M.» como el mismo niño, pero el índice de V3 solo
-- ignoraba las mayúsculas: dos altas al mismo tiempo, una con tilde y otra sin ella, pasaban.
-- Ahora el índice compara como Validacion.claveNombre: sin mayúsculas ni tildes (los espacios
-- ya llegan normalizados). translate y lower son inmutables, así que sirven en un índice.
-- Los repetidos que aparezcan con esta regla se numeran antes, igual que en V3.
WITH repetidos AS (
    SELECT id, ROW_NUMBER() OVER (
        PARTITION BY grupo_id, translate(lower(nombre),
            'áàâäãéèêëíìîïóòôöõúùûüñçÁÀÂÄÃÉÈÊËÍÌÎÏÓÒÔÖÕÚÙÛÜÑÇ',
            'aaaaaeeeeiiiiooooouuuuncaaaaaeeeeiiiiooooouuuunc')
        ORDER BY creado_en, id
    ) AS orden
    FROM ninos
)
UPDATE ninos
SET nombre = ninos.nombre || ' ' || repetidos.orden
FROM repetidos
WHERE ninos.id = repetidos.id AND repetidos.orden > 1;

DROP INDEX ninos_nombre_unico_en_grupo;

CREATE UNIQUE INDEX ninos_nombre_unico_en_grupo ON ninos (
    grupo_id,
    translate(lower(nombre),
        'áàâäãéèêëíìîïóòôöõúùûüñçÁÀÂÄÃÉÈÊËÍÌÎÏÓÒÔÖÕÚÙÛÜÑÇ',
        'aaaaaeeeeiiiiooooouuuuncaaaaaeeeeiiiiooooouuuunc')
);
