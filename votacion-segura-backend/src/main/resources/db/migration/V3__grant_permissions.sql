-- ============================================================
-- V3__grant_permissions.sql
-- Otorga permisos mínimos al usuario de aplicación (votacion_app).
--
-- PRINCIPIO: mínimo privilegio (OWASP ASVS V14.2 / CWE-272)
--
-- votacion_app puede:
--   - SELECT, INSERT, UPDATE en la mayoría de tablas
--   - Solo INSERT en votos (NO UPDATE, NO DELETE)
--   - Solo INSERT en eventos_auditoria (NO UPDATE, NO DELETE)
--   - UPDATE limitado en credenciales_voto (solo estado y used_at)
--
-- votacion_app NO puede:
--   - DELETE en ninguna tabla
--   - UPDATE en votos (nunca)
--   - UPDATE en eventos_auditoria (nunca)
--   - ALTER, DROP, CREATE TABLE
--   - Acceder a pg_catalog con privilegios elevados
--
-- Nota: La restricción a nivel de columna se implementa adicionalmente
-- en la capa de aplicación (entidades sin @Setter, servicios sin métodos de modificación).
-- ============================================================

-- ──────────────────────────────────────────────────────────────
-- Revocar cualquier permiso previo innecesario
-- ──────────────────────────────────────────────────────────────
REVOKE ALL ON ALL TABLES IN SCHEMA public FROM votacion_app;

-- ──────────────────────────────────────────────────────────────
-- usuarios: SELECT e INSERT (UPDATE solo de campos no-críticos)
-- ──────────────────────────────────────────────────────────────
GRANT SELECT, INSERT ON TABLE usuarios TO votacion_app;
GRANT UPDATE (
    enabled,
    account_non_locked,
    failed_login_attempts,
    last_login_at,
    locked_until,
    updated_at
) ON TABLE usuarios TO votacion_app;

-- ──────────────────────────────────────────────────────────────
-- socios: SELECT, INSERT, UPDATE (no DELETE)
-- ──────────────────────────────────────────────────────────────
GRANT SELECT, INSERT ON TABLE socios TO votacion_app;
GRANT UPDATE (
    nombres,
    apellidos,
    correo_electronico,
    correo_verificado,
    estado_habilitacion,
    activo,
    usuario_id,
    updated_at
) ON TABLE socios TO votacion_app;

-- ──────────────────────────────────────────────────────────────
-- elecciones: SELECT, INSERT, UPDATE (no DELETE)
-- ──────────────────────────────────────────────────────────────
GRANT SELECT, INSERT ON TABLE elecciones TO votacion_app;
GRANT UPDATE (
    nombre,
    descripcion,
    fecha_inicio,
    fecha_fin,
    estado,
    opened_at,
    closed_at,
    solicitante_cierre_id,
    cierre_solicitado_at,
    updated_at
) ON TABLE elecciones TO votacion_app;

-- ──────────────────────────────────────────────────────────────
-- candidaturas: SELECT, INSERT, UPDATE (no DELETE)
-- ──────────────────────────────────────────────────────────────
GRANT SELECT, INSERT ON TABLE candidaturas TO votacion_app;
GRANT UPDATE (
    nombre,
    descripcion,
    propuesta,
    activo,
    updated_at
) ON TABLE candidaturas TO votacion_app;

-- ──────────────────────────────────────────────────────────────
-- participaciones: SELECT, INSERT, UPDATE (solo estado y fecha_operativa)
-- No DELETE (la participación es registro permanente)
-- ──────────────────────────────────────────────────────────────
GRANT SELECT, INSERT ON TABLE participaciones TO votacion_app;
GRANT UPDATE (
    estado_participacion,
    fecha_operativa
) ON TABLE participaciones TO votacion_app;

-- ──────────────────────────────────────────────────────────────
-- credenciales_voto: SELECT, INSERT
-- UPDATE solo en estado y used_at (consumo atómico controlado)
-- NO DELETE, NO UPDATE de token_hash ni de eleccion_id
-- ──────────────────────────────────────────────────────────────
GRANT SELECT, INSERT ON TABLE credenciales_voto TO votacion_app;
GRANT UPDATE (
    estado,
    used_at
) ON TABLE credenciales_voto TO votacion_app;

-- ──────────────────────────────────────────────────────────────
-- votos: SELECT, INSERT ÚNICAMENTE
-- NO UPDATE (ningún campo), NO DELETE
-- La inmutabilidad se refuerza tanto aquí como en la capa de aplicación
-- ──────────────────────────────────────────────────────────────
GRANT SELECT, INSERT ON TABLE votos TO votacion_app;
-- NO GRANT UPDATE ON votos
-- NO GRANT DELETE ON votos

-- ──────────────────────────────────────────────────────────────
-- refresh_tokens: SELECT, INSERT, UPDATE (solo revocado)
-- ──────────────────────────────────────────────────────────────
GRANT SELECT, INSERT ON TABLE refresh_tokens TO votacion_app;
GRANT UPDATE (
    revocado
) ON TABLE refresh_tokens TO votacion_app;

-- ──────────────────────────────────────────────────────────────
-- codigos_verificacion: SELECT, INSERT, UPDATE (solo estado de uso)
-- ──────────────────────────────────────────────────────────────
GRANT SELECT, INSERT ON TABLE codigos_verificacion TO votacion_app;
GRANT UPDATE (
    intentos_fallidos,
    usado
) ON TABLE codigos_verificacion TO votacion_app;

-- ──────────────────────────────────────────────────────────────
-- eventos_auditoria: INSERT, SELECT (lectura restringida por API/roles)
-- NO UPDATE, NO DELETE (append-only desde la aplicación)
-- La seguridad adicional se implementa en el endpoint /api/v1/auditoria
-- (solo AUDITOR_INDEPENDIENTE y ADMINISTRADOR_TECNICO pueden consultar)
-- ──────────────────────────────────────────────────────────────
GRANT SELECT, INSERT ON TABLE eventos_auditoria TO votacion_app;
-- NO GRANT UPDATE ON eventos_auditoria
-- NO GRANT DELETE ON eventos_auditoria

-- ──────────────────────────────────────────────────────────────
-- actas_resultado: SELECT, INSERT, UPDATE (solo estado y aprobadores)
-- ──────────────────────────────────────────────────────────────
GRANT SELECT, INSERT ON TABLE actas_resultado TO votacion_app;
GRANT UPDATE (
    estado,
    aprobador1_id,
    aprobador2_id,
    fecha_aprobacion
) ON TABLE actas_resultado TO votacion_app;

-- ──────────────────────────────────────────────────────────────
-- Secuencias (si se usan SERIAL; con UUID no son necesarias,
-- pero se incluyen por compatibilidad)
-- ──────────────────────────────────────────────────────────────
GRANT USAGE ON ALL SEQUENCES IN SCHEMA public TO votacion_app;

-- ──────────────────────────────────────────────────────────────
-- Asegurar que permisos futuros también se apliquen
-- ──────────────────────────────────────────────────────────────
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT ON TABLES TO votacion_app;
