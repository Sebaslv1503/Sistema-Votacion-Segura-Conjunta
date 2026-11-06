# Informe integral del Sistema de Votación Segura

> Documento de referencia para comprender, probar y defender el proyecto académico.
>
 **Estado:** funcional para demostración local  
> **Fecha de revisión:** 6 de noviembre de 2026  
> **Entorno objetivo:** `localhost`  
> **Repositorio:** `Sebaslv1503/Sistema-Votacion-Segura-Conjunta`

---

## 1. Resumen ejecutivo

El proyecto implementa un sistema de votación electrónica para elecciones internas
de una asociación profesional. Está compuesto por:

- Backend REST en Java 17 y Spring Boot 4.1.2.
- Frontend React 19 con Vite y TypeScript.
- PostgreSQL 16 para desarrollo.
- Flyway para migraciones versionadas.
- Spring Security y JWT para autenticación y autorización.
- Mailpit para correo local de desarrollo.
- Pruebas automatizadas con Maven, JUnit, Mockito y H2.
- Colección de seguridad para Postman en `postman-security-tests/`.

El flujo principal ya fue corregido y validado:

1. Un usuario inicia sesión.
2. El sistema entrega un access token y un refresh token.
3. Un votante habilitado solicita una credencial de voto.
4. La credencial se almacena únicamente como hash SHA-256.
5. El votante selecciona una candidatura.
6. El voto se registra sin conservar una relación directa con el votante.
7. La credencial se marca como usada.
8. La participación se marca como votada.
9. Un segundo uso de la credencial es rechazado.

La aplicación es funcional para una demostración académica local. Todavía no debe
presentarse como una plataforma electoral de producción: faltan pruebas integrales
con PostgreSQL, pruebas de carga, operación distribuida, MFA/OTP completo,
monitoreo productivo, backups y una revisión independiente de seguridad.

---

## 2. Estructura del proyecto

### Backend

Ubicación: `votacion-segura-backend/`

Componentes principales:

- `src/main/java/.../auth`: login, JWT, refresh tokens, cambio de contraseña y
  limitación de intentos.
- `src/main/java/.../config`: seguridad, CORS, configuración de beans.
- `src/main/java/.../eleccion`: elecciones, estados y cierre electoral.
- `src/main/java/.../candidatura`: candidaturas.
- `src/main/java/.../votacion`: emisión de credenciales y depósito de votos.
- `src/main/java/.../participacion`: elegibilidad y estado del votante.
- `src/main/java/.../escrutinio`: conteo, acta, aprobación y publicación.
- `src/main/java/.../socio`: padrón de socios.
- `src/main/java/.../auditoria`: eventos de auditoría.
- `src/main/java/.../entity`: entidades JPA.
- `src/main/java/.../repository`: repositorios Spring Data.
- `src/main/resources/db/migration`: migraciones de esquema.
- `src/main/resources/db/dev-migration`: datos reproducibles de desarrollo.

### Frontend

Ubicación: `frontend/`

El frontend está construido con React, TypeScript, Vite, React Query y Vitest.
Incluye login, navegación por roles y pantalla de votación. Los tokens se
mantienen en memoria; al recargar la página se pierde la sesión, lo cual reduce
persistencia en el navegador pero obliga a iniciar sesión nuevamente.

### Infraestructura local

- `docker-compose.yml`: PostgreSQL y Mailpit.
- `docker/init-db.sh`: inicialización de usuarios y base de datos.
- `.env.example`: variables esperadas sin secretos reales.
- `pom.xml`: dependencias y tareas Maven.

### Documentación

- `README.md`: instalación y uso local.
- `PLAN_VERIFICACION_EVIDENCIAS_FINAL.docx`: pruebas T01-T24.
- `Propuesta_Integral_Seguridad_Votacion_Online.docx`: requisitos de seguridad y
  privacidad.
- `GUIA_POSTMAN_EVIDENCIAS.md`: guía previa de evidencias.
- `postman-security-tests/`: colección y entorno de pruebas de seguridad.

---

## 3. Tecnologías y decisiones técnicas

| Área | Tecnología |
|---|---|
| Lenguaje backend | Java 17 |
| Framework backend | Spring Boot 4.1.2 |
| API | REST |
| Persistencia | Spring Data JPA / Hibernate |
| Base de datos | PostgreSQL 16 |
| Migraciones | Flyway |
| Seguridad | Spring Security |
| Tokens | JWT de acceso y refresh tokens rotatorios |
| Hash de contraseñas | BCrypt mediante el encoder configurado |
| Frontend | React 19, TypeScript, Vite |
| Pruebas backend | JUnit, Mockito, Spring Boot Test, H2 |
| Pruebas frontend | Vitest |
| Correo local | Mailpit |
| Documentación API | Springdoc OpenAPI / Swagger |
| Contenedores | Docker Compose |

### Separación de usuarios de base de datos

El diseño separa la conexión de runtime de la conexión utilizada por Flyway:

- Usuario de aplicación: acceso operativo limitado.
- Usuario de migración: permisos de DDL para ejecutar cambios de esquema.

Hibernate utiliza `ddl-auto=validate`; no crea ni modifica automáticamente el
esquema. Flyway es el responsable de las migraciones.

---

## 4. Cómo levantar el proyecto

### Requisitos

- Java 17.
- Node.js y npm.
- Docker Desktop.
- Git.

### Backend y dependencias

Desde `votacion-segura-backend/`:

```powershell
docker compose up -d
./mvnw.cmd clean test
./mvnw.cmd spring-boot:run
```

En Windows también puede ejecutarse:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

### Frontend

Desde `frontend/`:

```powershell
npm install
npm run dev
```

### URLs locales

| Servicio | URL |
|---|---|
| Backend | `http://localhost:8080` |
| Frontend | `http://localhost:5173` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI | `http://localhost:8080/api-docs` |
| Health | `http://localhost:8080/actuator/health` |
| Mailpit | `http://localhost:8025` |
| PostgreSQL | `localhost:5433` en el Compose local |

Los puertos pueden variar si se cambian en `docker-compose.yml` o en las
variables de entorno.

---

## 5. Variables de configuración

El archivo `application.properties` no debe contener secretos reales. Los valores
sensibles se leen desde variables de entorno.

Variables importantes:

| Variable | Propósito |
|---|---|
| `SPRING_PROFILES_ACTIVE` | Perfil `dev`, `test` o `prod` |
| `DB_HOST`, `DB_PORT`, `DB_NAME` | Conexión PostgreSQL |
| `DB_APP_USER`, `DB_APP_PASSWORD` | Usuario runtime |
| `DB_MIGRA_USER`, `DB_MIGRA_PASSWORD` | Usuario Flyway |
| `JWT_SECRET` | Clave HMAC del JWT |
| `JWT_ACCESS_EXPIRATION_MS` | Vigencia del access token |
| `JWT_REFRESH_EXPIRATION_MS` | Vigencia del refresh token |
| `CORS_ALLOWED_ORIGIN` | Origen permitido |
| `CREDENCIAL_EXPIRACION_MINUTOS` | Vigencia de credencial de voto |
| `SECURITY_MAX_INTENTOS_LOGIN` | Intentos fallidos antes del bloqueo |
| `SECURITY_BLOQUEO_MINUTOS` | Duración del bloqueo de cuenta |
| `SECURITY_LOGIN_RATE_LIMIT` | Solicitudes de login por IP |
| `SECURITY_LOGIN_RATE_WINDOW_SECONDS` | Ventana del rate limit |
| `MAIL_HOST`, `MAIL_PORT` | SMTP local de desarrollo |

La configuración local actual utiliza:

- Cinco fallos para bloqueo de cuenta.
- Quince minutos de bloqueo.
- Diez solicitudes de login por IP cada sesenta segundos.
- Access token de quince minutos.
- Refresh token de ocho horas.
- Credencial de voto de treinta minutos por defecto.

---

## 6. Usuarios, roles y cuentas de demostración

### Roles implementados

| Rol | Responsabilidad |
|---|---|
| `ADMINISTRADOR_TECNICO` | Administración técnica, elecciones, candidaturas, resultados y auditoría según endpoint |
| `AUDITOR_INDEPENDIENTE` | Consulta de resultados y auditoría |
| `CUSTODIO_ELECTORAL` | Gestión del cierre, candidaturas y publicación de resultados |
| `GESTOR_PADRON` | Alta, edición, búsqueda, habilitación e importación de socios |
| `SOCIO_VOTANTE` | Consulta de estado, solicitud de credencial y voto |
| `SOPORTE` | Cuenta operativa de soporte; no equivale automáticamente a administrador |

### Cuentas demo

Las cuentas se crean mediante la migración repetible:
`src/main/resources/db/dev-migration/R__seed_demo_accounts.sql`.

| Usuario | Rol | Estado |
|---|---|---|
| `demo.admin` | Administrador técnico | Habilitado |
| `demo.auditor` | Auditor independiente | Habilitado |
| `demo.custodio1` | Custodio electoral | Habilitado |
| `demo.custodio2` | Custodio electoral | Habilitado |
| `demo.gestor` | Gestor de padrón | Habilitado |
| `demo.soporte` | Soporte | Habilitado |
| `demo.votante01` | Socio votante | Habilitado |
| `demo.votante02` | Socio votante | Habilitado |
| `demo.votante03` | Socio votante | Habilitado |
| `demo.votante04` | Socio votante | Habilitado |
| `demo.votante05` | Socio votante | Inhabilitado deliberadamente |

Para desarrollo local, las cuentas demo utilizan la contraseña indicada en la
migración. No debe reutilizarse fuera de un entorno de prueba. Después de iniciar
sesión, se recomienda cambiarla con:

```text
POST /api/v1/auth/change-password
```

La colección Postman no almacena esta contraseña: se introduce manualmente en la
variable `local_password`.

### Socios demo

Los socios de prueba utilizan números con prefijo `DEMO-` y están asociados a los
votantes demo. El quinto socio está inhabilitado para demostrar que el sistema
rechaza la participación de un usuario no elegible.

---

## 7. Endpoints reales del backend

Todos los endpoints siguientes fueron identificados en controladores del código.
No se deben agregar rutas en Postman que no estén aquí o en los controladores.

### Autenticación

| Método | Endpoint | Acceso |
|---|---|---|
| `POST` | `/api/v1/auth/login` | Público |
| `POST` | `/api/v1/auth/refresh` | Público con refresh token |
| `POST` | `/api/v1/auth/logout` | Autenticado |
| `POST` | `/api/v1/auth/change-password` | Autenticado |

### Elecciones

| Método | Endpoint | Acceso |
|---|---|---|
| `GET` | `/api/v1/elecciones` | Roles operativos |
| `POST` | `/api/v1/elecciones` | Administrador técnico / custodio |
| `PATCH` | `/api/v1/elecciones/{id}/estado` | Custodio |
| `POST` | `/api/v1/elecciones/{id}/solicitar-cierre` | Custodio |
| `POST` | `/api/v1/elecciones/{id}/aprobar-cierre` | Custodio |

El cierre requiere separación de funciones: la aprobación no debe ser realizada
por el mismo custodio que solicitó el cierre.

### Candidaturas

| Método | Endpoint | Acceso |
|---|---|---|
| `GET` | `/api/v1/candidaturas/elecciones/{eleccionId}` | Votante, gestión, auditoría según autorización |
| `POST` | `/api/v1/candidaturas` | Administrador / custodio |
| `PUT` | `/api/v1/candidaturas/{id}` | Administrador / custodio |

La edición está restringida por las reglas de estado de la elección.

### Padrón y socios

| Método | Endpoint | Acceso |
|---|---|---|
| `POST` | `/api/v1/socios` | Gestor de padrón / administrador |
| `PUT` | `/api/v1/socios/{id}` | Gestor de padrón / administrador |
| `GET` | `/api/v1/socios` | Gestor de padrón / administrador |
| `PATCH` | `/api/v1/socios/{id}/habilitacion` | Gestor de padrón / administrador |
| `POST` | `/api/v1/socios/importar` | Gestor de padrón / administrador |

La importación utiliza un archivo CSV con columnas:

```text
numeroSocio,nombres,apellidos,correoElectronico
```

No existe un endpoint de ejecución de comandos ni un endpoint que exponga una
ruta de archivo arbitraria.

### Votación

| Método | Endpoint | Acceso |
|---|---|---|
| `GET` | `/api/v1/participaciones/elecciones/{id}/mi-estado` | Socio votante |
| `POST` | `/api/v1/votacion/elecciones/{id}/credencial` | Socio votante |
| `POST` | `/api/v1/votacion/votos` | Socio votante |

La emisión de credencial acepta el parámetro:

```text
?reemplazarPendiente=true
```

Este parámetro permite reemplazar una credencial pendiente que se perdió durante
una demostración, pero no permite votar nuevamente después de que la participación
está en estado `VOTADO`.

### Resultados y escrutinio

| Método | Endpoint | Acceso |
|---|---|---|
| `POST` | `/api/v1/resultados/elecciones/{id}/escrutinio` | Custodio / administrador |
| `POST` | `/api/v1/resultados/elecciones/{id}/aprobacion` | Custodio |
| `POST` | `/api/v1/resultados/elecciones/{id}/publicar` | Custodio |
| `GET` | `/api/v1/resultados/elecciones/{id}` | Votante, custodio, auditor, administrador |

Los resultados están protegidos por el estado de cierre, aprobación y publicación.

### Auditoría

| Método | Endpoint | Acceso |
|---|---|---|
| `GET` | `/api/v1/auditoria` | Auditor independiente / administrador |

Los eventos de auditoría registran operaciones relevantes sin convertir el voto en
un objeto navegable hacia el socio.

### Diagnóstico

| Método | Endpoint | Acceso |
|---|---|---|
| `GET` | `/actuator/health` | Público |
| `GET` | `/api-docs` | Documentación |
| `GET` | `/swagger-ui.html` | Documentación |

La exposición de Swagger y Actuator debe revisarse antes de un despliegue real.

---

## 8. Flujo completo de autenticación

### Login

`POST /api/v1/auth/login` recibe usuario y contraseña. Si las credenciales son
correctas devuelve:

- `accessToken`.
- `refreshToken`.
- tipo de token.
- tiempo de expiración.

El access token se utiliza como:

```text
Authorization: Bearer <accessToken>
```

### JWT

El JWT:

- Tiene firma HMAC con secreto configurable.
- Requiere una clave de al menos 256 bits.
- Tiene expiración.
- Incluye la identidad y roles necesarios.
- Es validado por un filtro de Spring Security.

Un JWT alterado, expirado o malformado debe ser rechazado.

### Refresh token

El refresh token:

- Es aleatorio.
- Se almacena en la base como hash.
- Se rota después de utilizarse.
- El token anterior no debe poder reutilizarse.
- La reutilización provoca rechazo y revocación de sesiones según el servicio.

### Logout

El logout revoca la sesión o refresh token correspondiente. No debe confundirse
con eliminar la cuenta ni con borrar votos.

### Cambio de contraseña

El cambio de contraseña:

- Requiere autenticación.
- Exige una contraseña con longitud mínima configurada.
- Usa BCrypt.
- Revoca refresh tokens activos para obligar a iniciar sesión nuevamente.

---

## 9. Flujo de votación y privacidad

### Elegibilidad

Antes de emitir una credencial, el sistema verifica:

- El socio existe.
- Está activo.
- Está habilitado.
- La elección existe.
- La elección está abierta.
- La fecha actual está dentro del intervalo de votación.

### Credencial

La credencial de voto:

- Se genera aleatoriamente con `SecureRandom`.
- Se entrega una sola vez al cliente.
- No se guarda en texto plano.
- Se almacena mediante SHA-256.
- Tiene expiración.
- Está asociada internamente a la participación para validar propiedad.

Si existe una credencial pendiente y no se solicita reemplazo, el sistema rechaza
la nueva emisión. Con `reemplazarPendiente=true`, la credencial anterior se marca
como vencida y se genera otra.

### Depósito

Al votar, el backend verifica:

- Que la credencial exista.
- Que la credencial corresponda al socio autenticado.
- Que no esté vencida.
- Que esté pendiente.
- Que la elección esté abierta.
- Que la candidatura pertenezca a la elección.
- Que la participación no esté consumida.

Después:

1. Inserta el voto.
2. Marca la credencial como `USADA`.
3. Marca la participación como `VOTADO`.
4. Registra el evento de auditoría.

El voto no guarda:

- `socio_id`.
- número de socio.
- cédula.
- correo.
- username.
- IP.
- session ID.
- credencial ID.
- participación ID.

Esto reduce la vinculación directa entre identidad y selección.

### Limitación de privacidad

La separación no es anonimato criptográfico perfecto. Un administrador de base de
datos podría intentar inferir correlaciones usando tiempos, estados, logs u otra
información auxiliar. Por eso el sistema trunca fechas operativas al minuto, pero
esa medida solo reduce el riesgo; no lo elimina.

---

## 10. Cierre, escrutinio y publicación

El flujo esperado es:

1. La elección se encuentra abierta.
2. Un custodio solicita el cierre.
3. Otro custodio aprueba el cierre.
4. Se genera el escrutinio.
5. Se aprueba el acta.
6. Se publica el resultado.
7. Los roles permitidos pueden consultar el acta.

El sistema busca impedir:

- Cierre aprobado por el mismo custodio que lo solicitó.
- Consulta de resultados antes de la publicación.
- Generación de resultados fuera del estado correcto.
- Modificación directa de votos mediante API.

El acta contiene:

- Identificador de elección.
- Total de votos.
- Fecha de cierre.
- Conteos por candidatura.
- Hash del manifiesto.
- Estado del acta.

La reproducibilidad debe comprobarse consultando el acta dos veces y comparando
total, conteos y hash.

---

## 11. Controles de seguridad implementados

### Autenticación y autorización

- JWT obligatorio para endpoints protegidos.
- Autorización por roles con `@PreAuthorize`.
- Separación entre roles operativos.
- Rechazo de llamadas anónimas.
- Rechazo de roles insuficientes.

### Contraseñas

- Hash BCrypt.
- No se devuelven hashes en DTOs.
- Cambio de contraseña autenticado.
- Revocación de sesiones después del cambio.

### Protección contra fuerza bruta

Existen dos controles:

1. Bloqueo por cuenta después de varios fallos.
2. Limitación local por IP mediante `LoginRateLimitFilter`.

El rate limiter en memoria es válido para una sola instancia. En producción
distribuida debe trasladarse a un gateway, WAF o almacén compartido.

### CORS

El backend permite el origen configurado mediante `CORS_ALLOWED_ORIGIN`.
La colección Postman incluye una prueba con `http://localhost:5173` y otra con
un origen no autorizado.

### CSRF

CSRF está deshabilitado deliberadamente porque la API utiliza JWT en el header
`Authorization` y no autenticación basada en cookies de navegador. Sin embargo,
esto no debe cambiarse sin revisar toda la estrategia de autenticación.

### SQL Injection

Las consultas se realizan mediante repositorios Spring Data y parámetros, no por
concatenación directa de SQL controlado por el usuario. La colección incluye
payloads de prueba no destructivos.

### Inmutabilidad del voto

La entidad `Voto` no define setters públicos para modificarlo. Los campos están
marcados como no actualizables y la base controla la estructura mediante
migraciones y restricciones.

### Auditoría

Se registran eventos como emisión de credencial y voto registrado. La auditoría
no debe incluir la selección del voto vinculada a la identidad del ciudadano.

---

## 12. Corrección importante realizada

Durante la validación del flujo apareció un error que parecía un `403`, pero en
realidad era una excepción interna de persistencia:

```text
ObjectOptimisticLockingFailureException
StaleObjectStateException
```

La causa era que el voto recibía un UUID antes de que Hibernate gestionara
correctamente su ciclo de persistencia. La solución fue:

- Dejar que la entidad inicialice su UUID en `@PrePersist`.
- Calcular el hash de integridad en `@PrePersist`.
- Guardar el voto sin forzar un identificador inconsistente desde el servicio.

También se corrigió el reemplazo de credenciales pendientes:

- La credencial anterior se marca como vencida.
- Se ejecuta `saveAndFlush`.
- Después se inserta la nueva credencial.

Esto evita la violación de la restricción única de una credencial pendiente por
participación.

Validación realizada:

- Login de `demo.votante01`.
- Reemplazo de credencial pendiente.
- Emisión de nueva credencial.
- Registro de voto.
- Estado persistido `VOTADO`.
- Credencial persistida `USADA`.
- Segundo voto rechazado con HTTP `409`.

---

## 13. Pruebas automatizadas realizadas

### Backend

Se ejecutó:

```powershell
.\mvnw.cmd clean test
```

El conjunto existente valida el arranque del contexto y varios rechazos de
`VotacionService`, entre ellos:

- Credencial vencida.
- Credencial perteneciente a otro socio.
- Elección cerrada.
- Reutilización de credencial.
- Depósito correcto de voto no vinculado.

Las pruebas unitarias utilizan Mockito y las pruebas de contexto utilizan H2.

### Frontend

Se validaron:

- Pruebas Vitest existentes.
- Construcción de producción del frontend.

### Validación manual de PostgreSQL

Se validó en PostgreSQL local:

- Migraciones aplicadas.
- Login real.
- Emisión y reemplazo de credencial.
- Depósito de voto.
- Persistencia de estado.
- Rechazo de segundo voto.

### Limitación de las pruebas actuales

Las pruebas automatizadas no cubren todavía de forma completa:

- Flujo HTTP completo con PostgreSQL.
- Concurrencia real con dos solicitudes simultáneas.
- Rotación de refresh token en integración.
- Prueba completa de cierre con dos custodios.
- Flujo completo de escrutinio y publicación.
- Carga y rendimiento.
- Recuperación ante fallos.

---

## 14. Colección de Postman

Ubicación:

```text
postman-security-tests/
```

Archivos:

- `Votacion_Seguridad.postman_collection.json`
- `Votacion_Local.postman_environment.json`
- `README_Pruebas_Seguridad.md`

La colección está organizada en carpetas para:

1. Autenticación.
2. SQL Injection.
3. Bypass de autenticación.
4. Manipulación JWT.
5. Escalamiento de privilegios.
6. IDOR.
7. Fuerza bruta y bloqueo.
8. Refresh token.
9. CORS.
10. XSS.
11. Voto duplicado.
12. Credencial ajena.
13. Resultados anticipados.
14. Doble aprobación.
15. Comandos y Path Traversal.
16. CSRF.
17. Manipulación administrativa.
18. Replay e idempotencia.
19. Información sensible.
20. Acceso no autorizado.
21. Integridad del conteo.
22. Smoke test de carga controlada.

La colección no debe ejecutarse ciegamente con un Runner sobre todas las
solicitudes porque algunas solicitudes cambian estado. Las solicitudes
mutantes deben ejecutarse con fixtures de prueba y datos descartables.

---

## 15. Matriz de requisitos de seguridad

| Requisito | Estado | Explicación |
|---|---|---|
| Login | Implementado | Usuario/contraseña y JWT |
| Roles | Implementado | Spring Security y `@PreAuthorize` |
| Refresh token | Implementado | Rotación, hash y replay detection |
| Logout | Implementado | Revocación de sesión |
| Cambio de contraseña | Implementado | BCrypt y revocación de sesiones |
| Rate limiting de login | Implementado localmente | No distribuido |
| Bloqueo de cuenta | Implementado | Configurable por variables |
| CORS | Implementado | Origen configurable |
| CSRF | Decisión documentada | Deshabilitado para API Bearer |
| SQL Injection | Mitigación implementada | Repositorios parametrizados |
| Credential stuffing | Mitigación parcial | Bloqueo y rate limit local |
| Privacidad del voto | Parcial/avanzada | Sin vínculo directo, pero no anonimato criptográfico |
| Credencial de un solo uso | Implementado | Estado y expiración |
| Voto duplicado | Implementado | Participación y credencial consumidas |
| Credencial ajena | Implementado | Verificación de propietario |
| Cierre con dos custodios | Implementado | Separación de funciones |
| Resultados anticipados | Implementado | Estados de cierre/aprobación/publicación |
| Auditoría | Implementado | Eventos operativos |
| Integridad de acta | Implementado | Hash de manifiesto |
| MFA/OTP completo | No terminado | Hay configuración, pero requiere completar flujo y pruebas |
| Idempotencia general | Parcial | El voto y refresh tienen controles; no existe clave idempotente general |
| Backups | Pendiente | No hay procedimiento automatizado validado |
| Restauración | Pendiente | Falta evidencia de recuperación |
| Alertas productivas | Pendiente | No hay SIEM/alertas distribuidas |
| SAST/SCA/SBOM | Pendiente | Debe integrarse en CI/CD |
| DAST automatizado | Pendiente | Postman cubre pruebas manuales controladas |
| Pruebas de carga | Pendiente | Solo smoke test local |
| Alta disponibilidad | Pendiente | El rate limiter actual es de instancia única |

---

## 16. Qué falta para considerarlo producción

### Prioridad alta

1. Cambiar el perfil predeterminado para que producción no use `dev`.
2. Impedir que las cuentas demo se creen fuera de desarrollo.
3. Completar MFA/OTP si es un requisito del proyecto.
4. Ejecutar integración real contra PostgreSQL.
5. Implementar backups cifrados y probar restauración.
6. Configurar secretos en un gestor seguro.
7. Deshabilitar Swagger y detalles de Actuator en producción.
8. Centralizar rate limiting para varias instancias.
9. Añadir logging estructurado y alertas.

### Prioridad media

1. Agregar pruebas de concurrencia.
2. Agregar pruebas de contrato para los endpoints.
3. Agregar pruebas de expiración y replay de JWT/refresh.
4. Añadir SAST, SCA, SBOM y análisis de dependencias.
5. Ejecutar DAST sobre un entorno aislado.
6. Formalizar política de retención de auditoría.
7. Añadir métricas de negocio y seguridad.

### Prioridad académica

1. Completar evidencias T01-T24.
2. Capturar request, response, fecha, usuario y resultado.
3. Documentar cada prueba como PASS, FAIL o NO IMPLEMENTADO.
4. Presentar una demostración reproducible con cuentas demo.
5. Explicar claramente los límites de anonimato y del entorno local.

---

## 17. Guion recomendado para la defensa

### Explicar la arquitectura

> El frontend consume una API REST Spring Boot. La API autentica mediante JWT,
> persiste en PostgreSQL y utiliza Flyway para controlar el esquema. Las
> responsabilidades se separan por roles y el voto no mantiene una relación
> directa con la identidad del votante.

### Explicar la seguridad del login

> Las contraseñas no se guardan en texto plano. Se almacenan con BCrypt. El
> login entrega un access token de corta duración y un refresh token rotatorio.
> Los fallos repetidos bloquean la cuenta y además existe un límite por IP.

### Explicar la privacidad

> La credencial se entrega al votante, pero el backend solo conserva su hash.
> La entidad del voto no tiene socio, username, IP, sesión, participación ni
> credencial. La separación reduce la correlación directa, aunque no constituye
> anonimato criptográfico perfecto frente a un administrador de base de datos.

### Explicar el voto duplicado

> La credencial cambia a `USADA` y la participación cambia a `VOTADO` dentro de
> una transacción. Si se intenta repetir la operación, el sistema responde
> `409` y no registra un segundo voto.

### Explicar el error corregido

> El error observado como `403` no era un problema de permisos. Era una
> excepción de persistencia que Spring redirigía al endpoint de error. Se corrigió
> el ciclo de vida del UUID del voto y se hizo flush de la credencial anterior
> antes de crear la nueva.

### Explicar el cierre electoral

> El cierre requiere solicitud y aprobación separadas. Un custodio solicita y
> otro custodio aprueba. Después se genera, aprueba y publica el acta.

### Explicar los límites

> Es una implementación académica local. Las pruebas no sustituyen un análisis
> de producción, no existe alta disponibilidad, el rate limiter es local y aún
> se deben completar backups, MFA/OTP, carga, monitoreo y CI/CD de seguridad.

---

## 18. Riesgos conocidos

### Perfil `dev` como valor predeterminado

La configuración base usa:

```text
spring.profiles.active=${SPRING_PROFILES_ACTIVE:dev}
```

En un proyecto de prueba esto facilita el arranque. En producción es peligroso
si se omite la variable porque puede cargar cuentas demo, migraciones de
desarrollo y configuraciones de diagnóstico.

### Cuentas demo conocidas

Son útiles para la presentación, pero nunca deben desplegarse en Internet ni
reutilizarse con datos reales.

### Rate limiting de una sola instancia

El filtro de login mantiene estado local. Con varias instancias, un atacante
podría distribuir solicitudes entre nodos. La solución sería usar gateway,
WAF o almacenamiento compartido.

### OTP configurado pero no completado

Existen propiedades para expiración y número de intentos OTP, pero eso no prueba
que exista un segundo factor completo y obligatorio. No debe afirmarse MFA
completo en la defensa hasta demostrar el flujo.

### CORS por configuración

Debe verificarse que `CORS_ALLOWED_ORIGIN` sea el frontend correcto y nunca un
comodín en producción.

### Operaciones mutantes en pruebas

Alta de socios, importación, cambios de estado, cierre y votación cambian la base.
Deben ejecutarse únicamente con datos ficticios y elecciones de prueba.

---

## 19. Checklist antes de la demostración

- [ ] Docker Desktop está activo.
- [ ] PostgreSQL responde.
- [ ] Mailpit responde.
- [ ] Backend responde en el puerto 8080.
- [ ] Frontend responde en el puerto 5173.
- [ ] Se puede iniciar sesión con un usuario demo.
- [ ] Se seleccionó una elección abierta.
- [ ] El votante está habilitado.
- [ ] Existe una candidatura activa.
- [ ] Se puede emitir una credencial.
- [ ] Se puede registrar un voto.
- [ ] El estado cambia a `VOTADO`.
- [ ] La credencial cambia a `USADA`.
- [ ] Un segundo voto es rechazado.
- [ ] No se muestran resultados antes de publicación.
- [ ] Se dispone de capturas de Postman y Swagger.
- [ ] No se exponen contraseñas, JWT ni datos reales en la presentación.

---

## 20. Conclusión

El sistema tiene una base funcional y una arquitectura adecuada para un proyecto
académico de seguridad de software. Sus puntos más fuertes son:

- Separación por roles.
- JWT con refresh token rotatorio.
- Hash de contraseñas.
- Bloqueo y rate limiting de login.
- Credenciales de voto de un solo uso.
- Validación de elegibilidad.
- Prevención de voto duplicado.
- Separación entre identidad y voto.
- Cierre con dos custodios.
- Acta con hash de integridad.
- Auditoría de operaciones.
- Pruebas automatizadas y colección Postman.

La defensa debe presentar el sistema como un prototipo académico funcional,
explicando honestamente sus límites. La afirmación correcta no es “es imposible
atacarlo”, sino:

> El sistema implementa controles verificables contra los ataques evaluados,
> documenta sus supuestos y deja identificados los controles que todavía deben
> completarse antes de un despliegue productivo.

