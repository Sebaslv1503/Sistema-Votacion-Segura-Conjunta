# Portal web del Sistema de Votación Electrónica Segura

Interfaz React + TypeScript conectada a la API Spring Boot existente. Las operaciones se ejecutan en el servidor; no se simulan altas, votos, resultados ni permisos.

## Ejecutar

Requiere Node.js 22.12 o posterior y que el backend escuche en `http://localhost:8080`.

```powershell
npm install
npm run dev
npm run build
npm run test
```

Vite sirve en `http://localhost:5173` y proxifica `/api` y `/actuator` al backend. Para otros entornos, copia `.env.example` a `.env.local` y asigna `VITE_API_URL` con la URL de la API. Si el navegador llama directamente a otro origen, configura en el backend `CORS_ALLOWED_ORIGIN` con el origen del frontend.

## Módulos entregados

- Inicio de sesión, cierre, renovación transparente de access token y rutas por rol. Tokens solo en memoria de la pestaña; al recargar se vuelve a autenticar. El rol del JWT solo decide navegación y la API es la autoridad.
- Inicio con elección abierta según API, métricas del padrón solo para gestor/admin, estado de servicio solo para admin y auditoría reciente solo para auditor/admin. Los indicadores no disponibles se omiten.
- Padrón: búsqueda, paginación, alta, edición, habilitación y carga CSV. Se muestra una previsualización local y la importación definitiva se confirma explícitamente; el backend rechaza el lote completo si hay duplicados.
- Elecciones: creación y transiciones disponibles, solicitud/aprobación de cierre. La lista del backend solo incluye elecciones abiertas; la página conserva las creadas/gestionadas mientras dura la sesión. Las acciones requieren confirmación.
- Candidaturas: altas, edición y activar/desactivar usando los recursos existentes.
- Votación: verifica elección abierta, elegibilidad y participación; emite credencial de un solo uso y registra la selección solo tras confirmación. La selección y credencial no se guardan en storage ni se muestran como recibo. Ante error de resultado incierto, consulta estado antes de volver a intentar.
- Auditoría paginada y filtrable por actor; administración de salud; publicación oficial de resultados y acciones de escrutinio autorizadas desde servidor.

Consulta [INTEGRATION.md](./INTEGRATION.md) para el contrato y las limitaciones observadas.

## Pruebas

`npm run test` contiene pruebas unitarias de interpretación de roles y almacenamiento en memoria. No reemplaza pruebas de integración contra Spring Boot, PostgreSQL ni concurrencia. Para validar votos y permisos en servidor, ejecutar también las pruebas del backend con su base de datos de test configurada.
