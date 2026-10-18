-- Esquema inicial de ReciclaKids (ADR-03: PostgreSQL).
-- Datos mínimos de menores (Ley 1581 de 2012): del niño solo se guarda el nombre con la inicial
-- del apellido, la edad, el avatar y sus resultados. Nunca documento, dirección, fotos ni ubicación.
-- Los identificadores son UUID en texto, generados por el backend.

-- Adultos: docentes y acudientes. La contraseña se guarda con hash BCrypt (incluye su sal).
CREATE TABLE usuarios (
    id               VARCHAR(36) PRIMARY KEY,
    rol              VARCHAR(16)  NOT NULL CHECK (rol IN ('DOCENTE', 'ACUDIENTE')),
    nombre           VARCHAR(120) NOT NULL,
    correo           VARCHAR(254) NOT NULL,
    hash_contrasena  VARCHAR(100) NOT NULL,
    creado_en        TIMESTAMPTZ  NOT NULL
);
CREATE UNIQUE INDEX usuarios_correo_unico ON usuarios (LOWER(correo));

-- Sesiones de adultos: el token de refresco viaja una sola vez; aquí solo vive su hash.
CREATE TABLE sesiones (
    id            VARCHAR(36) PRIMARY KEY,
    usuario_id    VARCHAR(36) NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    hash_refresco VARCHAR(64) NOT NULL UNIQUE,
    expira_en     TIMESTAMPTZ NOT NULL,
    revocada      BOOLEAN     NOT NULL DEFAULT FALSE,
    creada_en     TIMESTAMPTZ NOT NULL
);
CREATE INDEX sesiones_usuario ON sesiones (usuario_id);

-- Enlaces de recuperación de contraseña: un solo uso, vencen a los 30 minutos.
CREATE TABLE recuperaciones (
    id          VARCHAR(36) PRIMARY KEY,
    usuario_id  VARCHAR(36) NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    hash_token  VARCHAR(64) NOT NULL UNIQUE,
    expira_en   TIMESTAMPTZ NOT NULL,
    usada       BOOLEAN     NOT NULL DEFAULT FALSE,
    creada_en   TIMESTAMPTZ NOT NULL
);

-- Un grupo por docente en el piloto. Las preferencias de avisos son de la docente.
CREATE TABLE grupos (
    id                    VARCHAR(36) PRIMARY KEY,
    docente_id            VARCHAR(36)  NOT NULL UNIQUE REFERENCES usuarios (id) ON DELETE CASCADE,
    jardin                VARCHAR(120) NOT NULL,
    nombre                VARCHAR(80)  NOT NULL,
    aviso_publicacion     BOOLEAN      NOT NULL DEFAULT TRUE,
    aviso_resumen_diario  BOOLEAN      NOT NULL DEFAULT TRUE,
    aviso_ninos_sin_jugar BOOLEAN      NOT NULL DEFAULT FALSE,
    creado_en             TIMESTAMPTZ  NOT NULL
);

-- Niños: los agrega la docente. Cada uno tiene su propio código de vinculación para su acudiente.
CREATE TABLE ninos (
    id                       VARCHAR(36) PRIMARY KEY,
    grupo_id                 VARCHAR(36) NOT NULL REFERENCES grupos (id) ON DELETE CASCADE,
    nombre                   VARCHAR(80) NOT NULL,
    edad                     SMALLINT,
    avatar                   VARCHAR(40) NOT NULL,
    codigo_vinculacion       VARCHAR(7)  NOT NULL UNIQUE,
    -- Residuos seguidos sin error contados entre retos: dan «Racha de 5» y «Coral feliz».
    racha_entre_retos        INTEGER     NOT NULL DEFAULT 0,
    mejor_racha_entre_retos  INTEGER     NOT NULL DEFAULT 0,
    creado_en                TIMESTAMPTZ NOT NULL
);
CREATE INDEX ninos_grupo ON ninos (grupo_id);

-- Acudientes vinculados a un niño. La autorización de datos del menor se registra con fecha.
CREATE TABLE vinculaciones (
    acudiente_id  VARCHAR(36) NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    nino_id       VARCHAR(36) NOT NULL REFERENCES ninos (id) ON DELETE CASCADE,
    vinculado_en  TIMESTAMPTZ NOT NULL,
    autorizada_en TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (acudiente_id, nino_id)
);
CREATE INDEX vinculaciones_nino ON vinculaciones (nino_id);

-- Control parental por niño (UI-33): lo comparten todos sus acudientes.
CREATE TABLE controles_parentales (
    nino_id             VARCHAR(36) PRIMARY KEY REFERENCES ninos (id) ON DELETE CASCADE,
    limite_minutos      SMALLINT    NOT NULL DEFAULT 20 CHECK (limite_minutos BETWEEN 5 AND 45),
    dificultad_sugerida VARCHAR(10) NOT NULL DEFAULT 'MEDIO',
    correos_logro       BOOLEAN     NOT NULL DEFAULT TRUE,
    reporte_semanal     BOOLEAN     NOT NULL DEFAULT TRUE,
    actualizado_en      TIMESTAMPTZ NOT NULL
);

-- Retos de la biblioteca de la docente. El código de 4 dígitos solo existe al publicar y vale
-- únicamente el día del reto (RNF-05); no se repite entre retos del mismo día.
CREATE TABLE retos (
    id             VARCHAR(36) PRIMARY KEY,
    grupo_id       VARCHAR(36)  NOT NULL REFERENCES grupos (id) ON DELETE CASCADE,
    nombre         VARCHAR(120) NOT NULL,
    categorias     VARCHAR(60)  NOT NULL,
    dificultad     VARCHAR(10)  NOT NULL CHECK (dificultad IN ('FACIL', 'MEDIO', 'DIFICIL')),
    fecha          DATE,
    estado         VARCHAR(12)  NOT NULL CHECK (estado IN ('PUBLICADO', 'PROGRAMADO', 'BORRADOR')),
    codigo         CHAR(4),
    reutilizado_de VARCHAR(36)  REFERENCES retos (id) ON DELETE SET NULL,
    creado_en      TIMESTAMPTZ  NOT NULL
);
CREATE INDEX retos_grupo_fecha ON retos (grupo_id, fecha);
CREATE UNIQUE INDEX retos_codigo_por_dia ON retos (fecha, codigo) WHERE codigo IS NOT NULL;

-- Residuos del reto en el orden en que salen en el juego. El catálogo vive en el código.
CREATE TABLE reto_residuos (
    reto_id    VARCHAR(36) NOT NULL REFERENCES retos (id) ON DELETE CASCADE,
    orden      SMALLINT    NOT NULL,
    residuo_id VARCHAR(20) NOT NULL,
    PRIMARY KEY (reto_id, orden)
);

-- Niños que entraron al reto con el código (el contador en vivo de UI-24).
CREATE TABLE ingresos (
    nino_id    VARCHAR(36) NOT NULL REFERENCES ninos (id) ON DELETE CASCADE,
    reto_id    VARCHAR(36) NOT NULL REFERENCES retos (id) ON DELETE CASCADE,
    entro_en   TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (nino_id, reto_id)
);

-- Partidas que llegan desde el teléfono. [id_local] lo genera la app: si el lote se reenvía, no se
-- duplica (0 % de pérdida y sin dobles conteos ante reintentos).
CREATE TABLE partidas (
    id            VARCHAR(36) PRIMARY KEY,
    nino_id       VARCHAR(36) NOT NULL REFERENCES ninos (id) ON DELETE CASCADE,
    reto_id       VARCHAR(36) NOT NULL REFERENCES retos (id) ON DELETE CASCADE,
    id_local      VARCHAR(64) NOT NULL,
    fecha         DATE        NOT NULL,
    terminada     BOOLEAN     NOT NULL DEFAULT FALSE,
    aciertos      SMALLINT    NOT NULL DEFAULT 0,
    errores       SMALLINT    NOT NULL DEFAULT 0,
    racha_maxima  SMALLINT    NOT NULL DEFAULT 0,
    tiempo_ms     BIGINT      NOT NULL DEFAULT 0,
    recibida_en   TIMESTAMPTZ NOT NULL,
    UNIQUE (nino_id, id_local)
);
CREATE INDEX partidas_nino_fecha ON partidas (nino_id, fecha);
CREATE INDEX partidas_reto ON partidas (reto_id);

CREATE TABLE intentos (
    partida_id        VARCHAR(36) NOT NULL REFERENCES partidas (id) ON DELETE CASCADE,
    momento_ms        BIGINT      NOT NULL,
    residuo_id        VARCHAR(20) NOT NULL,
    categoria_elegida VARCHAR(20) NOT NULL,
    correcto          BOOLEAN     NOT NULL,
    PRIMARY KEY (partida_id, momento_ms, residuo_id)
);

-- Insignias ganadas: cada una una sola vez por niño.
CREATE TABLE insignias_ganadas (
    nino_id   VARCHAR(36) NOT NULL REFERENCES ninos (id) ON DELETE CASCADE,
    insignia  VARCHAR(30) NOT NULL,
    ganada_el DATE        NOT NULL,
    PRIMARY KEY (nino_id, insignia)
);

-- Correos enviados a acudientes (historial de UI-32).
CREATE TABLE correos_enviados (
    id                 VARCHAR(36) PRIMARY KEY,
    acudiente_id       VARCHAR(36)  NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    nino_id            VARCHAR(36)  NOT NULL REFERENCES ninos (id) ON DELETE CASCADE,
    tipo               VARCHAR(20)  NOT NULL CHECK (tipo IN ('LOGRO', 'REPORTE_SEMANAL')),
    destinatario       VARCHAR(254) NOT NULL,
    insignia           VARCHAR(30),
    retos_completados  SMALLINT     NOT NULL DEFAULT 0,
    retos_publicados   SMALLINT     NOT NULL DEFAULT 0,
    aciertos           SMALLINT,
    enviado_en         TIMESTAMPTZ  NOT NULL
);
CREATE INDEX correos_nino ON correos_enviados (nino_id, enviado_en);

-- Solicitudes de supresión de datos del menor (UI-34). La docente o el equipo las atiende.
CREATE TABLE solicitudes_eliminacion (
    nino_id        VARCHAR(36) PRIMARY KEY REFERENCES ninos (id) ON DELETE CASCADE,
    solicitada_por VARCHAR(36) NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    solicitada_el  DATE        NOT NULL
);
