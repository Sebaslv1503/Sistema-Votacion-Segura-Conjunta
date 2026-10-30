-- ============================================================
-- V1__create_db_roles_and_users.sql
-- Crea los roles de base de datos con mínimo privilegio.
--
-- USUARIOS:
--   votacion_migra : Solo para Flyway (migraciones). Propietario de objetos.
--   votacion_app   : Usuario de la aplicación en runtime. Permisos mínimos.
--
-- NOTA: Las contraseñas se configuran mediante variables de entorno.
--       NO introducir contraseñas reales en este archivo.
--       Este script asume que el superusuario ejecuta las migraciones.
--
-- SEPARACIÓN DE PRIVILEGIOS (OWASP ASVS V14.2):
--   - La aplicación Spring Boot usa votacion_app.
--   - Flyway usa votacion_migra.
--   - Ninguno tiene privilegios de superusuario.
-- ============================================================

-- Crear rol de migración si no existe
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'votacion_migra') THEN
        CREATE ROLE votacion_migra WITH LOGIN PASSWORD 'PLACEHOLDER_MIGRA_PASSWORD';
    END IF;
END
$$;

-- Crear rol de aplicación si no existe
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'votacion_app') THEN
        CREATE ROLE votacion_app WITH LOGIN PASSWORD 'PLACEHOLDER_APP_PASSWORD';
    END IF;
END
$$;

-- Permisos en el esquema public para ambos roles
GRANT USAGE ON SCHEMA public TO votacion_migra;
GRANT USAGE ON SCHEMA public TO votacion_app;

GRANT CREATE ON SCHEMA public TO votacion_migra;
