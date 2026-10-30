-- Development only. The original V4 hash did not match its documented
-- temporary password. Update only untouched seed accounts with that hash.
UPDATE usuarios
SET password_hash = '$2a$12$4PJIKesvgykwImGBDVQ3/udcgJeZVae9qtKzkIUEy6xrVVRrnGkRu',
    failed_login_attempts = 0,
    account_non_locked = TRUE,
    locked_until = NULL
WHERE username IN (
    'admin.tecnico',
    'custodio.1',
    'custodio.2',
    'auditor.independiente',
    'gestor.padron'
)
AND password_hash = '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQyCi0sONIXLBEWaOFgDHZ8Ku';
