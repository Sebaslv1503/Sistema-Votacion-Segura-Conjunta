-- Hibernate maps String as VARCHAR; align the fixed-length hash column with
-- the entity without rewriting an already-applied migration.
ALTER TABLE actas_resultado
    ALTER COLUMN hash_manifiesto TYPE VARCHAR(64);
