# Contrato e integración con el backend

Rutas inspeccionadas bajo `/api/v1`; los permisos indicados son los definidos por Spring Security.

| Área | Operaciones usadas | Roles principales |
|---|---|---|
| Sesión | `POST /auth/login`, `/auth/refresh`, `/auth/logout` | login público; logout autenticado |
| Elecciones | `GET /elecciones` (solo ABIERTA), `POST /elecciones`, `PATCH /{id}/estado`, `POST /{id}/solicitar-cierre`, `/aprobar-cierre` | lectura autenticada; gestión Custodio/Admin |
| Padrón | `GET/POST /socios`, `PUT /{id}`, `PATCH /{id}/habilitacion`, `POST /importar` (CSV `numeroSocio,nombres,apellidos,correoElectronico`) | Gestor/Admin |
| Candidaturas | `GET /candidaturas/elecciones/{id}`, `POST /candidaturas`, `PUT /candidaturas/{id}` | lectura autenticada según API; escritura Custodio/Admin |
| Participación y voto | `GET /participaciones/elecciones/{id}/mi-estado`, `POST /votacion/elecciones/{id}/credencial`, `POST /votacion/votos` | Socio votante |
| Escrutinio | `POST /resultados/elecciones/{id}/escrutinio`, `/aprobacion`, `/publicar`, `GET /resultados/elecciones/{id}` | Custodio/Admin para mutaciones; socio/custodio/auditor/admin para lectura publicada |
| Auditoría | `GET /auditoria?page=&size=&actorId=` | Auditor/Admin |
| Salud | `GET /actuator/health` | público |

## Capacidades de backend que faltan

- No hay endpoints de recuperación de contraseña, OTP, alta/gestión de cuentas y roles ni canal de soporte. La UI informa esas ausencias sin afirmar que las operaciones se hayan realizado.
- `GET /elecciones` solo retorna elecciones ABIERTAS. No hay catálogo de borradores/cerradas ni recurso de detalle. La interfaz no puede recuperar una lista completa tras recargar la sesión.
- No existe un endpoint de previsualización transaccional del padrón; el resumen de CSV se calcula en el navegador y la validación definitiva ocurre al importar.
- No hay endpoint para contar candidaturas elegibles/padrón asignado/participación en tarjetas de resumen. Tampoco filtros por fechas o tipos ni exportación de auditoría.
- El resultado se obtiene únicamente una vez publicado. No hay un listado de elecciones con resultados ni endpoint separado para consultar estado de aprobación.
- La API devuelve tokens en JSON, no cookies HttpOnly; el frontend los mantiene solo en memoria para reducir persistencia, por lo que una recarga cierra la sesión. La seguridad de autorización depende siempre del backend.

Para despliegue con llamadas directas entre orígenes, `CORS_ALLOWED_ORIGIN` del backend debe ser el origen exacto del frontend. En desarrollo el proxy Vite evita esa llamada cross-origin.
