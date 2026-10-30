#!/bin/bash
# ============================================================
# docker/init-db.sh
# Script de inicialización de la base de datos PostgreSQL.
# Se ejecuta UNA SOLA VEZ al crear el contenedor.
#
# Crea los usuarios de migración y aplicación con las contraseñas
# definidas en las variables de entorno.
#
# Este script se ejecuta como superusuario de PostgreSQL.
# ============================================================

set -e

echo "==> Inicializando usuarios de base de datos..."

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- Usuario de migraciones (Flyway)
    DO \$\$
    BEGIN
        IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = '${DB_MIGRA_USER:-votacion_migra}') THEN
            CREATE ROLE ${DB_MIGRA_USER:-votacion_migra}
                WITH LOGIN
                PASSWORD '${DB_MIGRA_PASSWORD:-CHANGE_ME_MIGRA}'
                NOCREATEDB
                NOCREATEROLE;
        ELSE
            ALTER ROLE ${DB_MIGRA_USER:-votacion_migra}
                WITH PASSWORD '${DB_MIGRA_PASSWORD:-CHANGE_ME_MIGRA}';
        END IF;
    END
    \$\$;

    -- Usuario de aplicación (runtime)
    DO \$\$
    BEGIN
        IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = '${DB_APP_USER:-votacion_app}') THEN
            CREATE ROLE ${DB_APP_USER:-votacion_app}
                WITH LOGIN
                PASSWORD '${DB_APP_PASSWORD:-CHANGE_ME_APP}'
                NOCREATEDB
                NOCREATEROLE;
        ELSE
            ALTER ROLE ${DB_APP_USER:-votacion_app}
                WITH PASSWORD '${DB_APP_PASSWORD:-CHANGE_ME_APP}';
        END IF;
    END
    \$\$;

    -- Otorgar permisos en el esquema public al usuario de migraciones
    GRANT USAGE, CREATE ON SCHEMA public TO ${DB_MIGRA_USER:-votacion_migra};
    GRANT ALL PRIVILEGES ON DATABASE ${POSTGRES_DB:-votacion_segura_db} TO ${DB_MIGRA_USER:-votacion_migra};

    -- El usuario de aplicación solo tiene USAGE en el esquema
    GRANT USAGE ON SCHEMA public TO ${DB_APP_USER:-votacion_app};

    RAISE NOTICE 'Usuarios de base de datos creados correctamente.';
EOSQL

echo "==> Inicialización de BD completada."
