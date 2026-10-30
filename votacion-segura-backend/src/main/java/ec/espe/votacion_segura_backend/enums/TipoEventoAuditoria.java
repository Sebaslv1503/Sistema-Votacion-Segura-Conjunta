package ec.espe.votacion_segura_backend.enums;

/**
 * Tipos de eventos registrados en el log de auditoría.
 *
 * NUNCA se registran: contraseñas, OTPs, tokens completos, selecciones de voto,
 * ni relaciones entre socio e identificador de candidatura.
 */
public enum TipoEventoAuditoria {

    // ── Autenticación ────────────────────────────────────────────────────────
    /** Inicio de sesión exitoso. */
    LOGIN_EXITOSO,
    /** Intento de inicio de sesión fallido (credenciales incorrectas). */
    LOGIN_FALLIDO,
    /** Cuenta bloqueada por exceso de intentos fallidos. */
    CUENTA_BLOQUEADA,
    /** Cierre de sesión. */
    LOGOUT,
    /** Solicitud de código OTP. */
    OTP_SOLICITADO,
    /** Verificación de OTP exitosa. */
    OTP_VERIFICADO,
    /** Intento de OTP fallido. */
    OTP_FALLIDO,
    /** Refresh token rotado. */
    TOKEN_ROTADO,
    /** Refresh token revocado (logout o detección de reutilización). */
    TOKEN_REVOCADO,

    // ── Padrón electoral ─────────────────────────────────────────────────────
    /** Registro de nuevo socio en el padrón. */
    SOCIO_REGISTRADO,
    /** Actualización de datos de socio. */
    SOCIO_ACTUALIZADO,
    /** Cambio de estado de habilitación de socio. */
    HABILITACION_MODIFICADA,

    // ── Gestión de elecciones ────────────────────────────────────────────────
    /** Creación de una elección. */
    ELECCION_CREADA,
    /** Modificación de configuración de elección (solo en estado BORRADOR). */
    ELECCION_MODIFICADA,
    /** Elección programada (transición BORRADOR → PROGRAMADA). */
    ELECCION_PROGRAMADA,
    /** Apertura de la elección (transición PROGRAMADA → ABIERTA). */
    ELECCION_ABIERTA,
    /** Pausa de la elección. */
    ELECCION_PAUSADA,
    /** Reanudación de la elección. */
    ELECCION_REANUDADA,
    /** Solicitud de cierre (primer custodio firma). */
    CIERRE_SOLICITADO,
    /** Aprobación del cierre (segundo custodio firma, elección pasa a CERRADA). */
    CIERRE_APROBADO,

    // ── Candidaturas ────────────────────────────────────────────────────────
    /** Registro de candidatura. */
    CANDIDATURA_REGISTRADA,
    /** Modificación de candidatura. */
    CANDIDATURA_MODIFICADA,
    /** Cambio de estado de candidatura (activo/inactivo). */
    CANDIDATURA_ESTADO_MODIFICADO,

    // ── Proceso de votación ──────────────────────────────────────────────────
    /** Credencial de voto emitida. NO se registra el token, solo el ID de participación. */
    CREDENCIAL_EMITIDA,
    /** Credencial de voto consumida al votar. NO se registra la candidatura elegida. */
    CREDENCIAL_CONSUMIDA,
    /** Voto registrado en la urna. NO se registra candidatura ni identidad del socio. */
    VOTO_REGISTRADO,

    // ── Escrutinio y resultados ──────────────────────────────────────────────
    /** Inicio del proceso de escrutinio. */
    ESCRUTINIO_INICIADO,
    /** Generación del acta de resultados. */
    ACTA_GENERADA,
    /** Aprobación del acta (firma de custodio). */
    ACTA_APROBADA,
    /** Publicación de resultados. */
    RESULTADOS_PUBLICADOS,

    // ── Seguridad y acceso ───────────────────────────────────────────────────
    /** Intento de operación no autorizada. */
    ACCESO_NO_AUTORIZADO,
    /** Consulta administrativa sensible realizada. */
    CONSULTA_ADMIN_SENSIBLE,
    /** Error interno del sistema registrado para auditoría. */
    ERROR_SISTEMA
}
