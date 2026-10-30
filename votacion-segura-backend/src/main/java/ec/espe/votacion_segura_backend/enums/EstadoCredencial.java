package ec.espe.votacion_segura_backend.enums;

/**
 * Estado de una credencial de voto de un solo uso.
 *
 * SEGURIDAD:
 * - El token original NUNCA se almacena; solo su hash SHA-256.
 * - La credencial NO contiene socioId ni candidaturaId.
 * - Una vez USADA, no puede reutilizarse (verificado en BD y en lógica de negocio).
 */
public enum EstadoCredencial {

    /** Credencial emitida y válida. Puede ser usada hasta su fecha de expiración. */
    PENDIENTE,

    /** Credencial consumida en la emisión de un voto. Estado final. */
    USADA,

    /** Credencial vencida sin ser utilizada (fecha_expiracion < NOW()). */
    VENCIDA,

    /** Credencial invalidada al emitirse una nueva para la misma participación. */
    INVALIDADA
}
