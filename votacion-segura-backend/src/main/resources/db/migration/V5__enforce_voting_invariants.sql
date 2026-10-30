-- Additional database-level defenses for one pending credential and append-only records.
WITH duplicate_pending AS (
    SELECT id, row_number() OVER (PARTITION BY participacion_ref_id ORDER BY created_at DESC, id DESC) AS position
    FROM credenciales_voto
    WHERE estado = 'PENDIENTE'
)
UPDATE credenciales_voto c
SET estado = 'INVALIDADA'
FROM duplicate_pending d
WHERE c.id = d.id AND d.position > 1;

WITH duplicate_pending AS (
    SELECT id, row_number() OVER (PARTITION BY participacion_ref_id ORDER BY created_at DESC, id DESC) AS position
    FROM credenciales_voto
    WHERE estado = 'PENDIENTE'
)
UPDATE credenciales_voto c
SET estado = 'INVALIDADA'
FROM duplicate_pending d
WHERE c.id = d.id AND d.position > 1;

CREATE UNIQUE INDEX uq_credencial_pending_participation
    ON credenciales_voto (participacion_ref_id)
    WHERE estado = 'PENDIENTE';

CREATE OR REPLACE FUNCTION reject_immutable_row_change()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Records in % are append-only', TG_TABLE_NAME;
END;
$$;

CREATE TRIGGER trg_votos_append_only
    BEFORE UPDATE OR DELETE ON votos
    FOR EACH ROW EXECUTE FUNCTION reject_immutable_row_change();

CREATE TRIGGER trg_auditoria_append_only
    BEFORE UPDATE OR DELETE ON eventos_auditoria
    FOR EACH ROW EXECUTE FUNCTION reject_immutable_row_change();
