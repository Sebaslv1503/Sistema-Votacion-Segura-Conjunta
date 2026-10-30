-- ============================================================
-- V4__insert_admin_user.sql
-- Inserta el usuario administrador técnico inicial del sistema.
--
-- SEGURIDAD:
--   - La contraseña en este script es un hash BCrypt de un valor temporal.
--   - Debe cambiarse INMEDIATAMENTE en el primer inicio de sesión.
--   - La contraseña real nunca debe commitearse en el repositorio.
--   - En producción, usar el mecanismo de reset de contraseña.
--
-- Hash BCrypt incluido: corresponde a "CambiarEsto1234!" (SOLO PARA DESARROLLO)
-- Factor de costo: 12
-- ============================================================

INSERT INTO usuarios (
    id,
    username,
    password_hash,
    rol,
    enabled,
    account_non_locked,
    failed_login_attempts,
    created_at,
    updated_at
) VALUES (
    gen_random_uuid(),
    'admin.tecnico',
    -- BCrypt hash de 'CambiarEsto1234!' con cost=12
    -- CAMBIAR EN PRODUCCIÓN mediante /api/v1/auth/cambiar-password
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQyCi0sONIXLBEWaOFgDHZ8Ku',
    'ADMINISTRADOR_TECNICO',
    true,
    true,
    0,
    NOW(),
    NOW()
)
ON CONFLICT (username) DO NOTHING;

-- ============================================================
-- Usuario custodio electoral 1 (para pruebas de doble firma)
-- ============================================================
INSERT INTO usuarios (
    id,
    username,
    password_hash,
    rol,
    enabled,
    account_non_locked,
    failed_login_attempts,
    created_at,
    updated_at
) VALUES (
    gen_random_uuid(),
    'custodio.1',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQyCi0sONIXLBEWaOFgDHZ8Ku',
    'CUSTODIO_ELECTORAL',
    true,
    true,
    0,
    NOW(),
    NOW()
)
ON CONFLICT (username) DO NOTHING;

INSERT INTO usuarios (
    id,
    username,
    password_hash,
    rol,
    enabled,
    account_non_locked,
    failed_login_attempts,
    created_at,
    updated_at
) VALUES (
    gen_random_uuid(),
    'custodio.2',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQyCi0sONIXLBEWaOFgDHZ8Ku',
    'CUSTODIO_ELECTORAL',
    true,
    true,
    0,
    NOW(),
    NOW()
)
ON CONFLICT (username) DO NOTHING;

-- ============================================================
-- Usuario auditor (solo lectura)
-- ============================================================
INSERT INTO usuarios (
    id,
    username,
    password_hash,
    rol,
    enabled,
    account_non_locked,
    failed_login_attempts,
    created_at,
    updated_at
) VALUES (
    gen_random_uuid(),
    'auditor.independiente',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQyCi0sONIXLBEWaOFgDHZ8Ku',
    'AUDITOR_INDEPENDIENTE',
    true,
    true,
    0,
    NOW(),
    NOW()
)
ON CONFLICT (username) DO NOTHING;

-- ============================================================
-- Usuario gestor del padrón
-- ============================================================
INSERT INTO usuarios (
    id,
    username,
    password_hash,
    rol,
    enabled,
    account_non_locked,
    failed_login_attempts,
    created_at,
    updated_at
) VALUES (
    gen_random_uuid(),
    'gestor.padron',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQyCi0sONIXLBEWaOFgDHZ8Ku',
    'GESTOR_PADRON',
    true,
    true,
    0,
    NOW(),
    NOW()
)
ON CONFLICT (username) DO NOTHING;
