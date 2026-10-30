-- Java maps SHA-256 hexadecimal values as String/VARCHAR(64). Keep all hash
-- columns consistent with that mapping while preserving existing values.
ALTER TABLE credenciales_voto
    ALTER COLUMN token_hash TYPE VARCHAR(64);

ALTER TABLE votos
    ALTER COLUMN hash_integridad TYPE VARCHAR(64);

ALTER TABLE refresh_tokens
    ALTER COLUMN token_hash TYPE VARCHAR(64);

ALTER TABLE eventos_auditoria
    ALTER COLUMN hash_evento TYPE VARCHAR(64);
