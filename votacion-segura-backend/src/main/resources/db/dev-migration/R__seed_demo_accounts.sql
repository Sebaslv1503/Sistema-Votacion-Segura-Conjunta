-- Development-only accounts with reproducible credentials.
-- Password for every account below: CambiarEsto1234!

INSERT INTO usuarios (username, password_hash, rol, enabled, account_non_locked, failed_login_attempts)
VALUES
    ('demo.admin', '$2a$12$4PJIKesvgykwImGBDVQ3/udcgJeZVae9qtKzkIUEy6xrVVRrnGkRu', 'ADMINISTRADOR_TECNICO', TRUE, TRUE, 0),
    ('demo.auditor', '$2a$12$4PJIKesvgykwImGBDVQ3/udcgJeZVae9qtKzkIUEy6xrVVRrnGkRu', 'AUDITOR_INDEPENDIENTE', TRUE, TRUE, 0),
    ('demo.custodio1', '$2a$12$4PJIKesvgykwImGBDVQ3/udcgJeZVae9qtKzkIUEy6xrVVRrnGkRu', 'CUSTODIO_ELECTORAL', TRUE, TRUE, 0),
    ('demo.custodio2', '$2a$12$4PJIKesvgykwImGBDVQ3/udcgJeZVae9qtKzkIUEy6xrVVRrnGkRu', 'CUSTODIO_ELECTORAL', TRUE, TRUE, 0),
    ('demo.gestor', '$2a$12$4PJIKesvgykwImGBDVQ3/udcgJeZVae9qtKzkIUEy6xrVVRrnGkRu', 'GESTOR_PADRON', TRUE, TRUE, 0),
    ('demo.soporte', '$2a$12$4PJIKesvgykwImGBDVQ3/udcgJeZVae9qtKzkIUEy6xrVVRrnGkRu', 'SOPORTE', TRUE, TRUE, 0),
    ('demo.votante01', '$2a$12$4PJIKesvgykwImGBDVQ3/udcgJeZVae9qtKzkIUEy6xrVVRrnGkRu', 'SOCIO_VOTANTE', TRUE, TRUE, 0),
    ('demo.votante02', '$2a$12$4PJIKesvgykwImGBDVQ3/udcgJeZVae9qtKzkIUEy6xrVVRrnGkRu', 'SOCIO_VOTANTE', TRUE, TRUE, 0),
    ('demo.votante03', '$2a$12$4PJIKesvgykwImGBDVQ3/udcgJeZVae9qtKzkIUEy6xrVVRrnGkRu', 'SOCIO_VOTANTE', TRUE, TRUE, 0),
    ('demo.votante04', '$2a$12$4PJIKesvgykwImGBDVQ3/udcgJeZVae9qtKzkIUEy6xrVVRrnGkRu', 'SOCIO_VOTANTE', TRUE, TRUE, 0),
    ('demo.votante05', '$2a$12$4PJIKesvgykwImGBDVQ3/udcgJeZVae9qtKzkIUEy6xrVVRrnGkRu', 'SOCIO_VOTANTE', TRUE, TRUE, 0)
ON CONFLICT (username) DO UPDATE SET
    password_hash = EXCLUDED.password_hash,
    enabled = TRUE,
    account_non_locked = TRUE,
    failed_login_attempts = 0,
    locked_until = NULL;

INSERT INTO socios (numero_socio, nombres, apellidos, estado_habilitacion, activo, usuario_id)
SELECT v.numero_socio, v.nombres, v.apellidos, v.estado, TRUE, u.id
FROM (VALUES
    ('DEMO-T01-01', 'Votante Demo 01', 'Prueba', 'HABILITADO', 'demo.votante01'),
    ('DEMO-T02-02', 'Votante Demo 02', 'Prueba', 'HABILITADO', 'demo.votante02'),
    ('DEMO-T17-03', 'Votante Demo 03', 'Prueba', 'HABILITADO', 'demo.votante03'),
    ('DEMO-T18-04', 'Votante Demo 04', 'Prueba', 'HABILITADO', 'demo.votante04'),
    ('DEMO-NO-HABILITADO', 'Votante Demo 05', 'No Habilitado', 'INHABILITADO', 'demo.votante05')
) AS v(numero_socio, nombres, apellidos, estado, username)
JOIN usuarios u ON u.username = v.username
ON CONFLICT (numero_socio) DO UPDATE SET
    usuario_id = EXCLUDED.usuario_id,
    nombres = EXCLUDED.nombres,
    apellidos = EXCLUDED.apellidos,
    estado_habilitacion = EXCLUDED.estado_habilitacion,
    activo = EXCLUDED.activo;
