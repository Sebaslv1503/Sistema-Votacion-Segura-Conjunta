# Colección de pruebas de seguridad local

## Alcance verificado

La colección usa únicamente `http://localhost:8080` y endpoints encontrados en los
controladores del backend. No contiene contraseñas, JWT, refresh tokens ni datos
reales. La variable `local_password` queda vacía y debe establecerse manualmente
en el entorno local de Postman.

Endpoints identificados:

- Autenticación: `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`,
  `POST /api/v1/auth/logout`, `POST /api/v1/auth/change-password`.
- Elecciones: `GET /api/v1/elecciones`, `POST /api/v1/elecciones`,
  `PATCH /api/v1/elecciones/{id}/estado`, `POST /api/v1/elecciones/{id}/solicitar-cierre`,
  `POST /api/v1/elecciones/{id}/aprobar-cierre`.
- Candidaturas: `GET /api/v1/candidaturas/elecciones/{id}`,
  `POST /api/v1/candidaturas`, `PUT /api/v1/candidaturas/{id}`.
- Votación: `POST /api/v1/votacion/elecciones/{id}/credencial`,
  `POST /api/v1/votacion/votos`,
  `GET /api/v1/participaciones/elecciones/{id}/mi-estado`.
- Resultados: `POST /api/v1/resultados/elecciones/{id}/escrutinio`,
  `POST /api/v1/resultados/elecciones/{id}/aprobacion`,
  `POST /api/v1/resultados/elecciones/{id}/publicar`,
  `GET /api/v1/resultados/elecciones/{id}`.
- Padrón: `POST /api/v1/socios`, `PUT /api/v1/socios/{id}`,
  `POST /api/v1/socios/importar`, `GET /api/v1/socios`,
  `PATCH /api/v1/socios/{id}/habilitacion`.
- Auditoría: `GET /api/v1/auditoria`.
- Diagnóstico público: `GET /actuator/health`.

Roles semilla de desarrollo: `demo.admin`, `demo.auditor`, `demo.custodio1`,
`demo.custodio2`, `demo.gestor`, `demo.soporte` y `demo.votante01` a
`demo.votante05`. La contraseña no está incluida; configúrela manualmente.

## Importación y preparación

1. Inicie PostgreSQL y el backend en modo local:
   `http://localhost:8080`.
2. Importe `Votacion_Local.postman_environment.json` y selecciónelo.
3. Configure `local_password` con la contraseña ficticia de desarrollo.
4. Importe `Votacion_Seguridad.postman_collection.json`.
5. Ejecute primero la carpeta **00 - Autenticación**. Las pruebas de login
   guardan los tokens en variables separadas por rol.
6. Configure `socio_id`, `otro_socio_id`, `eleccion_id`, `otro_eleccion_id` y
   `candidatura_id` con UUID ficticios existentes en su base local.

Las solicitudes mutantes están marcadas como controladas o deshabilitadas cuando
pueden cambiar el estado electoral. No ejecute pruebas de voto, cierre,
aprobación, publicación o importación sobre datos que necesite conservar.

## Interpretación

Cada solicitud contiene objetivo, payload, resultado esperado y una prueba
`pm.test()`. Un HTTP 200 no se considera automáticamente una vulnerabilidad:
revise también el cuerpo y el efecto esperado. `NO IMPLEMENTADO` significa que
el código no expone un endpoint para esa capacidad, no que la prueba haya
fallado.

Estados utilizados:

- **PASS**: la respuesta y el efecto coinciden con la protección esperada.
- **FAIL**: la respuesta o el efecto contradicen la protección esperada.
- **NO IMPLEMENTADO**: no existe una superficie verificable en el backend.
- **MANUAL**: requiere dos cuentas, un estado electoral concreto o inspección
  funcional adicional.

## Cobertura por categoría

1. SQL Injection: entradas de búsqueda y DTO; repositorios Spring Data
   parametrizados.
2. Bypass de autenticación: endpoints protegidos sin Bearer.
3. JWT: token alterado o malformado.
4. Escalamiento: roles cruzados.
5. IDOR: UUID de otro recurso con rol autorizado.
6. Fuerza bruta: máximo 11 intentos controlados; el filtro local limita IP y el
   servicio bloquea la cuenta tras fallos configurados.
7. Refresh reutilizado: rotación y replay del token anterior.
8. CORS: preflight permitido y origen no permitido.
9. XSS almacenado: texto inocuo con etiquetas en un DTO; validar que se trate
   como texto y que el frontend lo escape.
10. Voto duplicado: segunda solicitud con la misma credencial.
11. Credencial ajena: credencial de votante A con JWT de B.
12. Resultados anticipados: consulta antes de publicación.
13. Doble aprobación: aprobación por el mismo custodio y por custodios
   distintos; requiere estado de cierre.
14. Comandos/Path Traversal: no existe ejecución de comandos ni ruta de archivo
   controlada; solo se incluye importación CSV acotada para revisión manual.
15. CSRF: la API usa Bearer JWT y no cookies; sin JWT debe responder 401.
16. Manipulación administrativa de votos: no existen PUT/DELETE de votos;
   se prueba el método no soportado.
17. Replay/idempotencia: replay de voto y refresh; no se afirma idempotencia
   general donde no está implementada.
18. Información sensible: health público y revisión manual de respuestas.
19. Endpoints no autorizados: acceso sin token y rol incorrecto.
20. Conteo: generación, aprobación, publicación y consistencia del acta.
21. Carga: no está en esta colección; usar una herramienta separada y solo
   `GET /actuator/health` a baja tasa, nunca DDoS.

## Restricciones

No usar SQLMap destructivo, `--dump`, `--os-shell`, extracción de archivos,
producción, datos reales, eliminación de votos ni cargas masivas. La configuración
por defecto de la aplicación usa perfil de desarrollo; para una revisión de
despliegue debe verificarse explícitamente que producción no cargue las cuentas
demo ni logs de diagnóstico.
