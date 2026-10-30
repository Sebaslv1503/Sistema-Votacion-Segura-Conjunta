# Guía de pruebas manuales con Postman

Esta guía refleja las rutas declaradas actualmente por los controladores y la configuración Spring Security del backend. No incluye rutas inferidas ni operaciones que solo aparezcan en comentarios.

## Preparación

### URL base y headers

Con el backend local en ejecución, la URL base habitual es:

```text
http://localhost:8080
```

En Postman crea un Environment con estas variables:

| Variable | Valor inicial |
|---|---|
| `baseUrl` | `http://localhost:8080` |
| `adminToken` | vacío |
| `refreshToken` | vacío |
| `custodio1Token` | vacío |
| `custodio2Token` | vacío |
| `auditorToken` | vacío |
| `gestorToken` | vacío |
| `voterToken` | vacío; requiere una cuenta votante provisionada |
| `eleccionId` | vacío |
| `candidaturaId` | vacío |
| `credencial` | vacío |
| `socioId` | vacío |

Para rutas protegidas agrega este header:

```text
Authorization: Bearer {{adminToken}}
```

Sustituye `adminToken` por el token del rol que corresponda. Para peticiones JSON usa `Accept: application/json` y `Content-Type: application/json`. No hace falta configurar `Origin` en Postman; CORS normalmente solo interviene en navegadores.

### Cuentas semilla locales

El perfil `dev` siembra estas cuentas de prueba. La migración de reparación de desarrollo corrige el hash semilla defectuoso solo para estas cuentas y solo bajo ese perfil.

| Usuario | Rol | Contraseña temporal en `dev` |
|---|---|---|
| `admin.tecnico` | `ADMINISTRADOR_TECNICO` | `CambiarEsto1234!` |
| `custodio.1` | `CUSTODIO_ELECTORAL` | `CambiarEsto1234!` |
| `custodio.2` | `CUSTODIO_ELECTORAL` | `CambiarEsto1234!` |
| `auditor.independiente` | `AUDITOR_INDEPENDIENTE` | `CambiarEsto1234!` |
| `gestor.padron` | `GESTOR_PADRON` | `CambiarEsto1234!` |

Son credenciales de desarrollo; no las uses en producción. No se siembran cuentas `SOCIO_VOTANTE`.

### Estados HTTP usados por el backend

- `200 OK`: respuesta correcta. Los controladores no declaran `201 Created`; las altas exitosas también responden `200`.
- `400 Bad Request`: validación o `IllegalArgumentException` (incluye algunos recursos inexistentes; el manejador actual no los convierte a `404`).
- `401 Unauthorized`: login con credenciales incorrectas (lo confirma `AuthControllerIntegrationTest`).
- `403 Forbidden`: acceso a ruta protegida sin autenticación válida, token inválido o rol no autorizado. `SecurityConfig` no declara un entry point 401 para rutas protegidas; el filtro descarta el token inválido y Spring Security responde con su configuración por defecto. Un preflight CORS rechazado también puede dar 403.
- `409 Conflict`: `IllegalStateException`, por ejemplo transición inválida, credencial usada o elección no cerrada.
- `404 Not Found`: ruta que no existe.

Los errores tratados por `ApiExceptionHandler` normalmente tienen esta forma:

```json
{
  "timestamp": "2026-10-30T15:00:00Z",
  "status": 409,
  "error": "Conflicto de estado",
  "message": "El estado actual no permite esta operación",
  "path": "/api/v1/..."
}
```

El texto exacto de `message` depende de la validación que falle. Spring Security puede devolver el `401` o `403` con un cuerpo diferente.

## Autenticación

### Iniciar sesión

- **URL:** `{{baseUrl}}/api/v1/auth/login`
- **Método:** `POST`
- **Headers:** `Content-Type: application/json`, `Accept: application/json`
- **Rol:** público; se autentica la cuenta enviada.
- **Éxito:** `200 OK`

```json
{
  "username": "admin.tecnico",
  "password": "CambiarEsto1234!"
}
```

Respuesta:

```json
{
  "accessToken": "<JWT>",
  "refreshToken": "<token-opaco>",
  "tokenType": "Bearer",
  "expiresInMs": 900000
}
```

En **Scripts > Post-response** puedes guardar el token de administrador:

```javascript
if (pm.response.code === 200) {
  pm.environment.set("adminToken", pm.response.json().accessToken);
pm.environment.set("refreshToken", pm.response.json().refreshToken);
}
```

Para guardar tokens de otros roles, cambia el nombre por `custodio1Token`, `custodio2Token`, `auditorToken` o `gestorToken`.

### Login incorrecto

- **URL:** `{{baseUrl}}/api/v1/auth/login`
- **Método:** `POST`
- **Headers:** `Content-Type: application/json`
- **Rol:** público.
- **Esperado:** `401 Unauthorized`.

```json
{
  "username": "admin.tecnico",
  "password": "incorrecta"
}
```

El backend contabiliza intentos fallidos y puede bloquear temporalmente la cuenta al superar el límite configurado. Haz esta prueba pocas veces y no uses una contraseña equivocada repetidamente.

### Renovar token

- **URL:** `{{baseUrl}}/api/v1/auth/refresh`
- **Método:** `POST`
- **Headers:** `Content-Type: application/json`
- **Rol:** público; se requiere un refresh token vigente.
- **Éxito:** `200 OK` con el mismo formato de la respuesta de login.

```json
{
  "refreshToken": "{{refreshToken}}"
}
```

El contrato actual recibe el refresh token en JSON, no como cookie.

### Cerrar sesión

- **URL:** `{{baseUrl}}/api/v1/auth/logout`
- **Método:** `POST`
- **Headers:** `Authorization: Bearer {{adminToken}}`, `Content-Type: application/json`
- **Rol:** cualquier usuario autenticado.
- **Éxito:** `200 OK`, sin cuerpo.

```json
{
  "refreshToken": "{{refreshToken}}"
}
```

## Pruebas de seguridad solicitadas

### Acceso sin token

- **URL:** `{{baseUrl}}/api/v1/elecciones`
- **Método:** `GET`
- **Headers:** `Accept: application/json`; omite `Authorization`.
- **Esperado:** `403 Forbidden` con la cadena de seguridad actual.

### Token inválido

- **URL:** `{{baseUrl}}/api/v1/elecciones`
- **Método:** `GET`
- **Headers:** `Authorization: Bearer token-invalido`
- **Esperado:** `403 Forbidden` con la cadena de seguridad actual.

### Rol no autorizado

Usa el token de `auditor.independiente` para intentar crear una elección.

- **URL:** `{{baseUrl}}/api/v1/elecciones`
- **Método:** `POST`
- **Headers:** `Authorization: Bearer {{auditorToken}}`, `Content-Type: application/json`
- **Esperado:** `403 Forbidden`.

```json
{
  "nombre": "Prueba de permisos",
  "descripcion": "Esta solicitud debe ser rechazada para auditoría",
  "inicio": "2030-01-01T10:00:00Z",
  "fin": "2030-01-01T12:00:00Z"
}
```

La autorización ocurre antes de crear el recurso; no debe crearse una elección.

## Padrón electoral

Todos los endpoints del padrón requieren `GESTOR_PADRON` o `ADMINISTRADOR_TECNICO`.

### Consultar socios

- **URL:** `{{baseUrl}}/api/v1/socios?page=0&size=20&q=Garcia`
- **Método:** `GET`
- **Headers:** `Authorization: Bearer {{gestorToken}}`, `Accept: application/json`
- **Body:** ninguno.
- **Éxito:** `200 OK`, respuesta paginada `Page<SocioDto>`.

`q` es opcional y busca en número de socio, nombres y apellidos. `page` empieza en 0; `size` controla el tamaño de página. Se puede omitir `q` para listar.

```json
{
  "content": [
    {
      "id": "<uuid>",
      "numeroSocio": "SOC-1001",
      "nombres": "Ana",
      "apellidos": "García",
      "correoElectronico": "ana@example.org",
      "estadoHabilitacion": "PENDIENTE_VERIFICACION",
      "activo": true
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 20,
  "number": 0,
  "first": true,
  "last": true
}
```

La serialización completa de `Page` incluye campos adicionales de Spring Data.

### Crear socio del padrón

- **URL:** `{{baseUrl}}/api/v1/socios`
- **Método:** `POST`
- **Headers:** `Authorization: Bearer {{gestorToken}}`, `Content-Type: application/json`
- **Éxito:** `200 OK`.

```json
{
  "numeroSocio": "SOC-1001",
  "nombres": "Ana",
  "apellidos": "García",
  "correoElectronico": "ana@example.org"
}
```

Devuelve un `SocioDto`; inicialmente `estadoHabilitacion` es `PENDIENTE_VERIFICACION` y `activo` es `true`. Guarda el `id` de la respuesta como `socioId`.

### Actualizar datos permitidos de socio

- **URL:** `{{baseUrl}}/api/v1/socios/{{socioId}}`
- **Método:** `PUT`
- **Headers:** `Authorization: Bearer {{gestorToken}}`, `Content-Type: application/json`
- **Éxito:** `200 OK`.

```json
{
  "nombres": "Ana María",
  "apellidos": "García López",
  "correoElectronico": "ana@example.org"
}
```

El número de socio no se actualiza con esta ruta.

### Habilitar o cambiar estado del socio

- **URL:** `{{baseUrl}}/api/v1/socios/{{socioId}}/habilitacion`
- **Método:** `PATCH`
- **Headers:** `Authorization: Bearer {{gestorToken}}`, `Content-Type: application/json`
- **Éxito:** `200 OK`.

```json
{
  "estado": "HABILITADO",
  "activo": true
}
```

Los estados aceptados son `HABILITADO`, `INHABILITADO`, `PENDIENTE_VERIFICACION` y `BAJA`.

### Importar socios desde CSV

- **URL:** `{{baseUrl}}/api/v1/socios/importar`
- **Método:** `POST`
- **Headers:** `Authorization: Bearer {{gestorToken}}`; selecciona `form-data` en Body y no escribas manualmente el `Content-Type` multipart.
- **Parte multipart:** clave `archivo`, tipo `File`.
- **Éxito:** `200 OK`.

Contenido de ejemplo del archivo:

```csv
numeroSocio,nombres,apellidos,correoElectronico
SOC-2001,María,Pérez,maria@example.org
SOC-2002,Luis,Lozano,luis@example.org
```

Respuesta:

```json
{
  "sociosImportados": 2
}
```

No existe una ruta de previsualización. Filas duplicadas o inválidas producen un error; no uses datos reales para las evidencias.

## Elecciones

### Consultar elecciones disponibles

- **URL:** `{{baseUrl}}/api/v1/elecciones`
- **Método:** `GET`
- **Headers:** `Authorization: Bearer {{adminToken}}`, `Accept: application/json`
- **Roles:** `SOCIO_VOTANTE`, `GESTOR_PADRON`, `ADMINISTRADOR_TECNICO`, `CUSTODIO_ELECTORAL`, `AUDITOR_INDEPENDIENTE` o `SOPORTE`.
- **Éxito:** `200 OK`, arreglo JSON.

```json
[
  {
    "id": "<uuid>",
    "nombre": "Elección de prueba",
    "descripcion": "Evidencia Postman",
    "inicio": "2030-01-01T10:00:00Z",
    "fin": "2030-01-01T12:00:00Z",
    "estado": "ABIERTA"
  }
]
```

La ruta solo devuelve elecciones en estado `ABIERTA`; puede responder `[]` si no hay ninguna.

### Crear elección

- **URL:** `{{baseUrl}}/api/v1/elecciones`
- **Método:** `POST`
- **Headers:** `Authorization: Bearer {{custodio1Token}}`, `Content-Type: application/json`
- **Roles:** `ADMINISTRADOR_TECNICO` o `CUSTODIO_ELECTORAL`.
- **Éxito:** `200 OK`; se crea en estado `BORRADOR`.

```json
{
  "nombre": "Elección de prueba Postman",
  "descripcion": "Elección para evidencias manuales",
  "inicio": "{{inicioEleccion}}",
  "fin": "{{finEleccion}}"
}
```

En **Pre-request Script** de Postman puedes definir fechas vigentes al ejecutar:

```javascript
const now = Date.now();
pm.environment.set("inicioEleccion", new Date(now - 60_000).toISOString());
pm.environment.set("finEleccion", new Date(now + 2 * 60 * 60_000).toISOString());
```

Guarda el identificador de la respuesta:

```javascript
if (pm.response.code === 200) {
  pm.environment.set("eleccionId", pm.response.json().id);
}
```

### Cambiar estado de elección

- **URL:** `{{baseUrl}}/api/v1/elecciones/{{eleccionId}}/estado`
- **Método:** `PATCH`
- **Headers:** `Authorization: Bearer {{custodio1Token}}`, `Content-Type: application/json`
- **Rol:** solo `CUSTODIO_ELECTORAL`.
- **Éxito:** `200 OK`.

Para programarla:

```json
{
  "estado": "PROGRAMADA"
}
```

Luego de crear al menos una candidatura activa, usa el mismo endpoint con:

```json
{
  "estado": "ABIERTA"
}
```

Transiciones implementadas: `BORRADOR → PROGRAMADA → ABIERTA`, `ABIERTA → PAUSADA`, `PAUSADA → ABIERTA`. Apertura requiere candidatura activa y estar dentro del periodo configurado. El cierre no se hace con esta ruta; usa el módulo de cierre electoral.

## Candidaturas

### Consultar candidaturas activas

- **URL:** `{{baseUrl}}/api/v1/candidaturas/elecciones/{{eleccionId}}`
- **Método:** `GET`
- **Headers:** `Authorization: Bearer {{custodio1Token}}`, `Accept: application/json`
- **Roles:** socio votante, gestor del padrón, administrador técnico, custodio electoral o auditor independiente.
- **Éxito:** `200 OK`, arreglo JSON; solo candidaturas activas.

```json
[
  {
    "id": "<uuid>",
    "eleccionId": "<uuid>",
    "nombre": "Lista A",
    "descripcion": "Descripción de prueba",
    "propuesta": "Propuesta de prueba",
    "activo": true
  }
]
```

### Crear candidatura

- **URL:** `{{baseUrl}}/api/v1/candidaturas`
- **Método:** `POST`
- **Headers:** `Authorization: Bearer {{custodio1Token}}`, `Content-Type: application/json`
- **Roles:** `CUSTODIO_ELECTORAL` o `ADMINISTRADOR_TECNICO`.
- **Éxito:** `200 OK`.

```json
{
  "eleccionId": "{{eleccionId}}",
  "nombre": "Lista A",
  "descripcion": "Candidatura de prueba",
  "propuesta": "Propuesta de prueba"
}
```

Guarda el `id` devuelto como `candidaturaId`. La elección debe estar en `BORRADOR` o `PROGRAMADA`.

### Actualizar o activar candidatura

- **URL:** `{{baseUrl}}/api/v1/candidaturas/{{candidaturaId}}`
- **Método:** `PUT`
- **Headers:** `Authorization: Bearer {{custodio1Token}}`, `Content-Type: application/json`
- **Roles:** `CUSTODIO_ELECTORAL` o `ADMINISTRADOR_TECNICO`.
- **Éxito:** `200 OK`.

```json
{
  "nombre": "Lista A",
  "descripcion": "Descripción actualizada",
  "propuesta": "Propuesta actualizada",
  "activo": true
}
```

## Participación electoral

### Consultar elegibilidad y estado propio

- **URL:** `{{baseUrl}}/api/v1/participaciones/elecciones/{{eleccionId}}/mi-estado`
- **Método:** `GET`
- **Headers:** `Authorization: Bearer {{voterToken}}`, `Accept: application/json`
- **Rol:** `SOCIO_VOTANTE` asociado a un registro de socio.
- **Éxito:** `200 OK`.

```json
{
  "electionId": "<uuid>",
  "eligible": true,
  "participationState": "SIN_CREDENCIAL"
}
```

`eligible` requiere socio activo, estado `HABILITADO`, elección `ABIERTA` y hora dentro del periodo. `participationState` puede ser `SIN_CREDENCIAL`, `PENDIENTE` o `VOTADO`.

## Votación

### Emitir credencial de un solo uso

- **URL:** `{{baseUrl}}/api/v1/votacion/elecciones/{{eleccionId}}/credencial`
- **Método:** `POST`
- **Headers:** `Authorization: Bearer {{voterToken}}`, `Accept: application/json`
- **Body:** ninguno.
- **Rol:** `SOCIO_VOTANTE` asociado a un socio activo y habilitado.
- **Éxito:** `200 OK`.

```json
{
  "credencial": "<token-opaco>"
}
```

Guarda `credencial` como variable secreta/local de Postman. No la publiques en capturas. Si ya existe una credencial pendiente, la emisión nueva se rechaza con `409`; si la participación ya está votada, también se rechaza.

### Emitir un voto

- **URL:** `{{baseUrl}}/api/v1/votacion/votos`
- **Método:** `POST`
- **Headers:** `Authorization: Bearer {{voterToken}}`, `Content-Type: application/json`
- **Rol:** `SOCIO_VOTANTE` correspondiente a la credencial.
- **Éxito:** `200 OK`.

```json
{
  "credencial": "{{credencial}}",
  "candidaturaId": "{{candidaturaId}}"
}
```

Respuesta:

```json
{
  "confirmacion": "Voto recibido"
}
```

La respuesta no contiene la candidatura seleccionada.

### Intentar votar dos veces

Después de una respuesta `200` en el voto anterior, repite la misma petición con la misma credencial.

- **Esperado:** `409 Conflict`, credencial vencida o ya utilizada.
- **Respuesta de error típica:** `status: 409`; `message` indica `Credencial vencida o ya utilizada`.

No generes una credencial nueva para repetir esta prueba: la participación ya consumida no admite otro voto.

## Cierre electoral

El cierre requiere dos custodios diferentes. Guarda el token de `custodio.1` en `custodio1Token` y el de `custodio.2` en `custodio2Token`.

### Solicitar cierre

- **URL:** `{{baseUrl}}/api/v1/elecciones/{{eleccionId}}/solicitar-cierre`
- **Método:** `POST`
- **Headers:** `Authorization: Bearer {{custodio1Token}}`, `Accept: application/json`
- **Body:** ninguno.
- **Rol:** `CUSTODIO_ELECTORAL`.
- **Éxito:** `200 OK`, la respuesta de elección cambia a `CIERRE_SOLICITADO`.

### Aprobar cierre

- **URL:** `{{baseUrl}}/api/v1/elecciones/{{eleccionId}}/aprobar-cierre`
- **Método:** `POST`
- **Headers:** `Authorization: Bearer {{custodio2Token}}`, `Accept: application/json`
- **Body:** ninguno.
- **Rol:** `CUSTODIO_ELECTORAL`, identidad distinta al solicitante.
- **Éxito:** `200 OK`, la respuesta de elección cambia a `CERRADA`.

Si aprueba el mismo custodio que solicitó el cierre, el backend responde `409 Conflict`.

## Escrutinio

Cerrar una elección no publica resultados. Primero se genera un acta y se requieren aprobaciones de dos custodios distintos.

### Generar escrutinio

- **URL:** `{{baseUrl}}/api/v1/resultados/elecciones/{{eleccionId}}/escrutinio`
- **Método:** `POST`
- **Headers:** `Authorization: Bearer {{custodio1Token}}`, `Accept: application/json`
- **Body:** ninguno.
- **Roles:** `CUSTODIO_ELECTORAL` o `ADMINISTRADOR_TECNICO`.
- **Éxito:** `200 OK`; solo si la elección está `CERRADA`.

Respuesta:

```json
{
  "eleccionId": "<uuid>",
  "totalVotos": 1,
  "fechaCierre": "2030-01-01T12:00:00Z",
  "conteosJson": "[{\"candidaturaId\":\"<uuid>\",\"nombre\":\"Lista A\",\"votos\":1}]",
  "hash": "<sha256>",
  "estado": "PENDIENTE_APROBACION"
}
```

`conteosJson` es una cadena que contiene JSON.

### Aprobar acta

- **URL:** `{{baseUrl}}/api/v1/resultados/elecciones/{{eleccionId}}/aprobacion`
- **Método:** `POST`
- **Headers:** `Authorization: Bearer {{custodio1Token}}`, `Accept: application/json`
- **Body:** ninguno.
- **Rol:** `CUSTODIO_ELECTORAL`.
- **Éxito:** `200 OK` por cada aprobación; la primera registra el aprobador 1 y la segunda, de custodio distinto, lleva el acta a `APROBADA`.

Llama por segunda vez usando `custodio2Token`. La misma persona no puede aprobar dos veces; se responde `409 Conflict`.

### Publicar resultados

- **URL:** `{{baseUrl}}/api/v1/resultados/elecciones/{{eleccionId}}/publicar`
- **Método:** `POST`
- **Headers:** `Authorization: Bearer {{custodio1Token}}`, `Accept: application/json`
- **Body:** ninguno.
- **Rol:** `CUSTODIO_ELECTORAL`.
- **Éxito:** `200 OK` si el acta ya tiene dos aprobaciones distintas. El estado del acta pasa a `PUBLICADA` y la elección a `FINALIZADA`.

Publicar antes de las dos aprobaciones responde `409 Conflict`.

## Resultados

### Intentar consultar antes del cierre/publicación

- **URL:** `{{baseUrl}}/api/v1/resultados/elecciones/{{eleccionId}}`
- **Método:** `GET`
- **Headers:** `Authorization: Bearer {{auditorToken}}`, `Accept: application/json`
- **Roles:** `SOCIO_VOTANTE`, `CUSTODIO_ELECTORAL`, `AUDITOR_INDEPENDIENTE` o `ADMINISTRADOR_TECNICO`.

Antes de generar un acta, se espera `400 Bad Request` (`Resultados no disponibles`). Si el acta ya se generó pero sigue sin publicar, se espera `409 Conflict` (`Resultados aún no publicados`).

### Consultar resultados publicados

Usa la misma petición después de generar el acta, obtener dos aprobaciones y publicarla.

- **Esperado:** `200 OK` con el DTO de acta mostrado en la sección de escrutinio y `estado: "PUBLICADA"`.

La API no ofrece una ruta para consultar votos individuales ni resultados de un acta no publicada.

## Auditoría

### Consultar eventos

- **URL:** `{{baseUrl}}/api/v1/auditoria?page=0&size=20`
- **Método:** `GET`
- **Headers:** `Authorization: Bearer {{auditorToken}}`, `Accept: application/json`
- **Roles:** `AUDITOR_INDEPENDIENTE` o `ADMINISTRADOR_TECNICO`.
- **Éxito:** `200 OK`, `Page<AuditDto>` ordenada por fecha descendente.

Filtro real opcional: `actorId` UUID. Ejemplo:

```text
{{baseUrl}}/api/v1/auditoria?actorId={{actorId}}&page=0&size=20
```

```json
{
  "content": [
    {
      "id": "<uuid>",
      "actorId": "<uuid-o-null>",
      "rolActor": "CUSTODIO_ELECTORAL",
      "tipo": "ACTA_GENERADA",
      "accion": "Acta de resultados generada",
      "resultado": "SUCCESS",
      "fecha": "2030-01-01T12:00:00Z",
      "correlationId": null,
      "detalles": "{\"eleccionId\":\"<uuid>\"}",
      "hash": "<sha256-o-null>"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 20,
  "number": 0,
  "first": true,
  "last": true
}
```

No existen filtros por fechas/tipo ni exportación.

## Usuarios y administración

### Usuarios

No hay controlador para crear/listar/editar/deshabilitar usuarios, asignar roles o cambiar contraseña. Las cuentas indicadas en esta guía son datos semilla de desarrollo. No existe una ruta para registrar usuarios votantes ni asociarlos a socios.

Aunque `SecurityConfig` permite sin autenticación los paths `POST /api/v1/auth/otp/request` y `POST /api/v1/auth/otp/verify`, **no existe un controlador que los implemente**; no son endpoints funcionales y responderán `404`.

### Administración / Actuator

No hay controlador administrativo de usuarios o configuración. Las rutas Actuator realmente configuradas son:

| URL | Método | Headers / acceso | Éxito |
|---|---|---|---|
| `{{baseUrl}}/actuator/health` | `GET` | Público; `Accept: application/json` | `200` si está `UP`; `503` si el estado es `DOWN` (por ejemplo, Mailpit no disponible) |
| `{{baseUrl}}/actuator/info` | `GET` | `Authorization: Bearer {{adminToken}}`; cualquier usuario autenticado | `200`; objeto con información de `info.*` |
| `{{baseUrl}}/actuator/metrics` | `GET` | `Authorization: Bearer {{adminToken}}`; cualquier usuario autenticado | `200`; métricas/nombres disponibles en perfil `dev` |

`metrics` está expuesto en la configuración base/dev, pero no en producción. En producción `health` e `info` están expuestos; el detalle de health está oculto.

## Prerrequisitos y funciones que no se pueden probar solo con las cuentas semilla

- Para probar elegibilidad, credencial, voto y estado de participación necesitas un usuario con rol `SOCIO_VOTANTE` asociado a un socio activo y `HABILITADO`. `POST /api/v1/socios` solo crea el registro del padrón; no crea cuenta ni relación con usuario. No hay API para provisionar esa asociación.
- Para probar candidaturas y apertura, crea la elección con fechas vigentes, crea al menos una candidatura, programa la elección y luego ábrela con un custodio.
- Para probar el cierre, usa dos tokens de custodios distintos.
- Para que resultados aparezcan al socio/auditor, ejecuta cierre, generación de acta, dos aprobaciones y publicación.
- No están implementados endpoints de registro de usuario, restablecimiento/cambio de contraseña, OTP, CRUD de usuarios/roles, eliminación de socios, edición/eliminación de votos, exportación de auditoría ni listado de elecciones cerradas.

## Secuencia sugerida para evidencias

1. Login correcto como `admin.tecnico`; captura status `200` sin publicar los tokens.
2. Login incorrecto una sola vez; captura `401`.
3. Consulta `/api/v1/elecciones` sin token y con `Bearer token-invalido`; captura `403` con la configuración actual.
4. Intenta `POST /api/v1/elecciones` como auditor; captura `403`.
5. Login como custodio 1 y custodio 2. Crea elección, candidatura, prográmala y ábrela.
6. Captura consulta de elecciones y candidaturas.
7. Con cuenta votante preprovisionada: captura estado elegible, emisión de credencial, voto, intento de segundo voto y estado `VOTADO`.
8. Solicita cierre con custodio 1; aprueba con custodio 2.
9. Captura consulta de resultados antes de publicar; luego genera, aprueba, publica y captura la consulta final.
10. Consulta auditoría como auditor y captura eventos relevantes.

En evidencias, oculta `Authorization`, access/refresh tokens, la credencial de voto, datos personales y cualquier información sensible.




