# Sistema de Votación Electrónica Segura

Backend Java 17 / Spring Boot 4.1.2 con PostgreSQL 16, Flyway, JWT y OpenAPI.

## Configuración

La configuración del repositorio está en `application.properties`, `application-dev.properties` y `application-prod.properties`; no hay archivos YAML. Las pruebas activan explícitamente el perfil `test` y usan una base H2 en memoria, con Flyway desactivado. De esta forma `clean test` no lee `DB_APP_PASSWORD`, `DB_MIGRA_PASSWORD`, ni apunta a una instancia PostgreSQL del entorno.

La integración con Testcontainers está declarada en Maven, pero Docker Desktop no estaba accesible desde el entorno de desarrollo al verificar este cambio. La prueba de contexto usa H2; esto no sustituye la validación de migraciones ni el comportamiento concurrente real en PostgreSQL.

Para desarrollo local, copia `.env.example` a `.env` y reemplaza los placeholders. El perfil `dev` importa ese archivo automáticamente (Spring Boot no carga `.env` por defecto). `JWT_SECRET` debe decodificar a al menos 32 bytes; genera uno con `openssl rand -base64 64`. Después levanta PostgreSQL:

```powershell
docker compose up -d postgres
./mvnw.cmd clean test
./mvnw.cmd spring-boot:run
```

Swagger UI: `http://localhost:8080/swagger-ui.html`; OpenAPI: `http://localhost:8080/api-docs`.

## Endpoints disponibles

- `POST /api/v1/auth/login`, `/refresh`, `/logout`: acceso JWT y rotación/revocación del refresh token.
- `/api/v1/socios`: alta, edición de datos permitidos, búsqueda paginada, habilitación y carga CSV (`numeroSocio,nombres,apellidos,correoElectronico`).
- `/api/v1/participaciones/elecciones/{id}/mi-estado`: elegibilidad y estado propio del socio autenticado.
- `/api/v1/elecciones`: elecciones disponibles, creación, cambios de estado y solicitud/aprobación de cierre por custodios distintos.
- `/api/v1/candidaturas`: registro y actualización antes del inicio de la elección.
- `/api/v1/votacion/elecciones/{id}/credencial` y `/api/v1/votacion/votos`: emisión y depósito de voto.
- `/api/v1/resultados`: escrutinio, doble aprobación, publicación y consulta.
- `/api/v1/auditoria`: consulta paginada restringida a auditores y administradores técnicos.

Las respuestas REST usan DTOs. Las credenciales de voto son tokens aleatorios de un solo uso; solo su SHA-256 se conserva. El voto no tiene relación con socio, participación ni credencial. La separación relacional y el truncado de timestamps no brindan anonimato criptográfico; el acceso privilegiado a la base puede permitir inferencia por correlación temporal.

## Cuentas semilla de desarrollo

Flyway inserta `admin.tecnico`, `custodio.1`, `custodio.2`, `auditor.independiente` y `gestor.padron`. La migración semilla documenta una contraseña temporal de desarrollo; cámbiala antes de cualquier uso real. No se crean socios con cuentas de votante automáticamente.

## Estado de las pruebas

`./mvnw.cmd clean test` usa H2 y comprueba el contexto de Spring y algunos rechazos del servicio de votación (credencial vencida, socio no propietario y elección cerrada). No cubre todavía una prueba integral con PostgreSQL, carga concurrente, autenticación HTTP completa ni la aplicación de las migraciones V5 y V6.
