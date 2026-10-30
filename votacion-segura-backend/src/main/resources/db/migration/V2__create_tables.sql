-- ============================================================
-- V2__create_tables.sql
-- Crea todas las tablas del sistema de votación segura.
--
-- ORDEN: Respeta dependencias de claves foráneas.
-- EXTENSIONES: uuid-ossp para generación de UUIDs nativos.
-- ============================================================

-- Extensión para generación de UUIDs en BD
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================
-- TABLA: usuarios
-- Cuentas de acceso al sistema.
-- ============================================================
CREATE TABLE usuarios (
    id                    UUID         NOT NULL DEFAULT gen_random_uuid(),
    username              VARCHAR(50)  NOT NULL,
    password_hash         VARCHAR(255) NOT NULL,
    rol                   VARCHAR(30)  NOT NULL,
    enabled               BOOLEAN      NOT NULL DEFAULT TRUE,
    account_non_locked    BOOLEAN      NOT NULL DEFAULT TRUE,
    failed_login_attempts INTEGER      NOT NULL DEFAULT 0,
    last_login_at         TIMESTAMPTZ,
    locked_until          TIMESTAMPTZ,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_usuarios PRIMARY KEY (id),
    CONSTRAINT uq_usuarios_username UNIQUE (username),
    CONSTRAINT chk_usuarios_rol CHECK (
        rol IN ('SOCIO_VOTANTE','GESTOR_PADRON','ADMINISTRADOR_TECNICO',
                'CUSTODIO_ELECTORAL','AUDITOR_INDEPENDIENTE','SOPORTE')
    )
);

CREATE INDEX idx_usuarios_username ON usuarios(username);
CREATE INDEX idx_usuarios_rol      ON usuarios(rol);

-- ============================================================
-- TABLA: socios
-- Padrón electoral. Sin datos sensibles más allá de nombre/correo.
-- ============================================================
CREATE TABLE socios (
    id                  UUID         NOT NULL DEFAULT gen_random_uuid(),
    numero_socio        VARCHAR(20)  NOT NULL,
    nombres             VARCHAR(100) NOT NULL,
    apellidos           VARCHAR(100) NOT NULL,
    correo_electronico  VARCHAR(150),
    correo_verificado   BOOLEAN      NOT NULL DEFAULT FALSE,
    estado_habilitacion VARCHAR(30)  NOT NULL DEFAULT 'PENDIENTE_VERIFICACION',
    activo              BOOLEAN      NOT NULL DEFAULT TRUE,
    usuario_id          UUID,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_socios PRIMARY KEY (id),
    CONSTRAINT uq_socios_numero_socio UNIQUE (numero_socio),
    CONSTRAINT uq_socios_usuario UNIQUE (usuario_id),
    CONSTRAINT fk_socios_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    CONSTRAINT chk_socios_estado CHECK (
        estado_habilitacion IN ('HABILITADO','INHABILITADO','PENDIENTE_VERIFICACION','BAJA')
    )
);

CREATE INDEX idx_socios_numero_socio ON socios(numero_socio);
CREATE INDEX idx_socios_estado       ON socios(estado_habilitacion);
CREATE INDEX idx_socios_usuario_id   ON socios(usuario_id);

-- ============================================================
-- TABLA: elecciones
-- Ciclo de vida completo de una elección.
-- ============================================================
CREATE TABLE elecciones (
    id                    UUID          NOT NULL DEFAULT gen_random_uuid(),
    nombre                VARCHAR(200)  NOT NULL,
    descripcion           VARCHAR(1000),
    fecha_inicio          TIMESTAMPTZ   NOT NULL,
    fecha_fin             TIMESTAMPTZ   NOT NULL,
    estado                VARCHAR(30)   NOT NULL DEFAULT 'BORRADOR',
    opened_at             TIMESTAMPTZ,
    closed_at             TIMESTAMPTZ,
    solicitante_cierre_id UUID,
    cierre_solicitado_at  TIMESTAMPTZ,
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_elecciones PRIMARY KEY (id),
    CONSTRAINT chk_elecciones_estado CHECK (
        estado IN ('BORRADOR','PROGRAMADA','ABIERTA','PAUSADA',
                   'CIERRE_SOLICITADO','CERRADA','FINALIZADA')
    ),
    CONSTRAINT chk_elecciones_fechas CHECK (fecha_fin > fecha_inicio)
);

CREATE INDEX idx_elecciones_estado      ON elecciones(estado);
CREATE INDEX idx_elecciones_fecha_inicio ON elecciones(fecha_inicio);

-- ============================================================
-- TABLA: candidaturas
-- Candidatos/listas de una elección específica.
-- ============================================================
CREATE TABLE candidaturas (
    id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    eleccion_id UUID         NOT NULL,
    nombre      VARCHAR(200) NOT NULL,
    descripcion VARCHAR(500),
    propuesta   TEXT,
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_candidaturas PRIMARY KEY (id),
    CONSTRAINT fk_candidaturas_eleccion FOREIGN KEY (eleccion_id) REFERENCES elecciones(id)
);

CREATE INDEX idx_candidaturas_eleccion ON candidaturas(eleccion_id);
CREATE INDEX idx_candidaturas_activo   ON candidaturas(eleccion_id, activo);

-- ============================================================
-- TABLA: participaciones
-- Registra QUIÉN participó en una elección, NUNCA POR QUIÉN votó.
--
-- RESTRICCIÓN CRÍTICA: UNIQUE(eleccion_id, socio_id)
-- Garantiza a nivel de BD que un socio no puede participar dos veces.
-- ============================================================
CREATE TABLE participaciones (
    id                   UUID        NOT NULL DEFAULT gen_random_uuid(),
    eleccion_id          UUID        NOT NULL,
    socio_id             UUID        NOT NULL,
    estado_participacion VARCHAR(20) NOT NULL DEFAULT 'SIN_CREDENCIAL',
    -- fecha_operativa truncada al minuto para reducir correlación temporal
    fecha_operativa      TIMESTAMPTZ,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_participaciones PRIMARY KEY (id),
    -- RESTRICCIÓN CRÍTICA: garantiza voto único por socio y elección
    CONSTRAINT uq_participacion_eleccion_socio UNIQUE (eleccion_id, socio_id),
    CONSTRAINT fk_participaciones_eleccion FOREIGN KEY (eleccion_id) REFERENCES elecciones(id),
    CONSTRAINT fk_participaciones_socio    FOREIGN KEY (socio_id)    REFERENCES socios(id),
    CONSTRAINT chk_participacion_estado CHECK (
        estado_participacion IN ('SIN_CREDENCIAL','PENDIENTE','VOTADO')
    )
    -- NOTA: Esta tabla NO tiene candidatura_id ni voto_id. Intencionalmente.
);

CREATE INDEX idx_participaciones_eleccion_socio ON participaciones(eleccion_id, socio_id);
CREATE INDEX idx_participaciones_socio          ON participaciones(socio_id);
CREATE INDEX idx_participaciones_estado         ON participaciones(eleccion_id, estado_participacion);

-- ============================================================
-- TABLA: credenciales_voto
-- Token de un solo uso para emitir el voto.
--
-- SEGURIDAD:
--   - token_hash: SHA-256 del token aleatorio (nunca el token original)
--   - NO contiene socio_id ni candidatura_id
--   - participacion_ref_id permite invalidar credenciales previas
--     y actualizar el estado de participación, sin vincular al voto
-- ============================================================
CREATE TABLE credenciales_voto (
    id                  UUID        NOT NULL DEFAULT gen_random_uuid(),
    token_hash          CHAR(64)    NOT NULL,  -- SHA-256 hex = 64 chars
    eleccion_id         UUID        NOT NULL,
    participacion_ref_id UUID       NOT NULL,
    fecha_expiracion    TIMESTAMPTZ NOT NULL,
    estado              VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    used_at             TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_credenciales PRIMARY KEY (id),
    CONSTRAINT uq_credenciales_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_credenciales_eleccion      FOREIGN KEY (eleccion_id)          REFERENCES elecciones(id),
    CONSTRAINT fk_credenciales_participacion FOREIGN KEY (participacion_ref_id) REFERENCES participaciones(id),
    CONSTRAINT chk_credenciales_estado CHECK (
        estado IN ('PENDIENTE','USADA','VENCIDA','INVALIDADA')
    )
);

CREATE INDEX idx_credencial_token_hash     ON credenciales_voto(token_hash);
CREATE INDEX idx_credencial_eleccion_estado ON credenciales_voto(eleccion_id, estado);
CREATE INDEX idx_credencial_participacion  ON credenciales_voto(participacion_ref_id);
CREATE INDEX idx_credencial_expiracion     ON credenciales_voto(fecha_expiracion);

-- ============================================================
-- TABLA: votos
-- La urna electrónica. Completamente separada de la identidad.
--
-- INVARIANTES CRÍTICOS:
--   - NO contiene socio_id, username, cedula, correo, ip, session_id
--   - NO tiene FK hacia socios, participaciones ni credenciales_voto
--   - NO permite UPDATE ni DELETE (controlado también en permisos de BD)
--   - fecha_registro truncada al minuto para reducir correlación temporal
-- ============================================================
CREATE TABLE votos (
    id               UUID        NOT NULL DEFAULT gen_random_uuid(),
    eleccion_id      UUID        NOT NULL,
    candidatura_id   UUID        NOT NULL,
    -- Truncado al minuto: date_trunc('minute', NOW())
    -- Reduce riesgo de correlación temporal exacta con credenciales
    fecha_registro   TIMESTAMPTZ NOT NULL,
    hash_integridad  CHAR(64)    NOT NULL,  -- SHA-256 del contenido del voto

    CONSTRAINT pk_votos PRIMARY KEY (id),
    CONSTRAINT fk_votos_eleccion    FOREIGN KEY (eleccion_id)    REFERENCES elecciones(id),
    CONSTRAINT fk_votos_candidatura FOREIGN KEY (candidatura_id) REFERENCES candidaturas(id)
    -- NO HAY FK hacia socios, participaciones ni credenciales_voto (diseño intencional)
);

CREATE INDEX idx_votos_eleccion            ON votos(eleccion_id);
CREATE INDEX idx_votos_eleccion_candidatura ON votos(eleccion_id, candidatura_id);
CREATE INDEX idx_votos_fecha               ON votos(fecha_registro);

-- ============================================================
-- TABLA: refresh_tokens
-- Tokens de renovación de sesión JWT.
-- ============================================================
CREATE TABLE refresh_tokens (
    id               UUID        NOT NULL DEFAULT gen_random_uuid(),
    token_hash       CHAR(64)    NOT NULL,
    usuario_id       UUID        NOT NULL,
    fecha_expiracion TIMESTAMPTZ NOT NULL,
    revocado         BOOLEAN     NOT NULL DEFAULT FALSE,
    ip_origen        VARCHAR(45),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT uq_refresh_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

CREATE INDEX idx_refresh_token_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_usuario    ON refresh_tokens(usuario_id);
CREATE INDEX idx_refresh_expiracion ON refresh_tokens(fecha_expiracion);

-- ============================================================
-- TABLA: codigos_verificacion
-- Códigos OTP de un solo uso.
-- ============================================================
CREATE TABLE codigos_verificacion (
    id               UUID        NOT NULL DEFAULT gen_random_uuid(),
    usuario_id       UUID        NOT NULL,
    codigo_hash      VARCHAR(255) NOT NULL,  -- BCrypt del código de 6 dígitos
    tipo             VARCHAR(30) NOT NULL,
    fecha_expiracion TIMESTAMPTZ NOT NULL,
    intentos_fallidos INTEGER    NOT NULL DEFAULT 0,
    usado            BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_codigos PRIMARY KEY (id),
    CONSTRAINT fk_codigos_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    CONSTRAINT chk_codigos_tipo CHECK (
        tipo IN ('MFA_LOGIN','VERIFICACION_VOTO','OPERACION_PRIVILEGIADA','RESET_PASSWORD')
    )
);

CREATE INDEX idx_codigo_usuario_tipo ON codigos_verificacion(usuario_id, tipo, usado);
CREATE INDEX idx_codigo_expiracion   ON codigos_verificacion(fecha_expiracion);

-- ============================================================
-- TABLA: eventos_auditoria
-- Log de auditoría append-only.
-- ============================================================
CREATE TABLE eventos_auditoria (
    id             UUID         NOT NULL DEFAULT gen_random_uuid(),
    actor_id       UUID,
    rol_actor      VARCHAR(30),
    tipo_evento    VARCHAR(50)  NOT NULL,
    accion         VARCHAR(300) NOT NULL,
    resultado      VARCHAR(20)  NOT NULL,
    fecha_evento   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    correlation_id VARCHAR(36),
    detalles       TEXT,
    hash_evento    CHAR(64),

    CONSTRAINT pk_auditoria PRIMARY KEY (id),
    CONSTRAINT chk_auditoria_resultado CHECK (resultado IN ('SUCCESS','FAILURE','UNAUTHORIZED'))
);

CREATE INDEX idx_auditoria_fecha       ON eventos_auditoria(fecha_evento);
CREATE INDEX idx_auditoria_actor       ON eventos_auditoria(actor_id);
CREATE INDEX idx_auditoria_tipo        ON eventos_auditoria(tipo_evento);
CREATE INDEX idx_auditoria_correlation ON eventos_auditoria(correlation_id);

-- ============================================================
-- TABLA: actas_resultado
-- Resultados agregados de una elección. Generados al cierre.
-- ============================================================
CREATE TABLE actas_resultado (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    eleccion_id     UUID        NOT NULL,
    total_votos     BIGINT      NOT NULL DEFAULT 0,
    fecha_cierre    TIMESTAMPTZ NOT NULL,
    conteos_json    JSONB       NOT NULL DEFAULT '[]'::jsonb,
    hash_manifiesto CHAR(64)    NOT NULL,
    estado          VARCHAR(30) NOT NULL DEFAULT 'BORRADOR',
    aprobador1_id   UUID,
    aprobador2_id   UUID,
    fecha_aprobacion TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_actas PRIMARY KEY (id),
    CONSTRAINT uq_acta_eleccion UNIQUE (eleccion_id),
    CONSTRAINT fk_acta_eleccion    FOREIGN KEY (eleccion_id)  REFERENCES elecciones(id),
    CONSTRAINT fk_acta_aprobador1  FOREIGN KEY (aprobador1_id) REFERENCES usuarios(id),
    CONSTRAINT fk_acta_aprobador2  FOREIGN KEY (aprobador2_id) REFERENCES usuarios(id),
    CONSTRAINT chk_acta_estado CHECK (
        estado IN ('BORRADOR','PENDIENTE_APROBACION','APROBADA','PUBLICADA')
    ),
    -- Los dos aprobadores deben ser diferentes
    CONSTRAINT chk_acta_aprobadores_distintos CHECK (
        aprobador1_id IS NULL OR aprobador2_id IS NULL OR aprobador1_id <> aprobador2_id
    )
);

CREATE INDEX idx_acta_eleccion ON actas_resultado(eleccion_id);
