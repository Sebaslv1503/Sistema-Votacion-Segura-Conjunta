package ec.espe.votacion_segura_backend.enums;

/**
 * Estados del ciclo de vida de una elección.
 * Las transiciones válidas se controlan en EleccionService (no se permiten saltos arbitrarios).
 *
 * Transiciones permitidas:
 *   BORRADOR → PROGRAMADA → ABIERTA → PAUSADA → ABIERTA (reanudar)
 *   ABIERTA  → CIERRE_SOLICITADO → CERRADA → FINALIZADA
 *
 * No se puede votar en estados: BORRADOR, PROGRAMADA, PAUSADA, CIERRE_SOLICITADO, CERRADA, FINALIZADA.
 */
public enum EstadoEleccion {

    /** Elección en creación. No visible para socios. */
    BORRADOR,

    /** Elección configurada y programada. Socios no pueden votar aún. */
    PROGRAMADA,

    /** Elección en curso. Los socios habilitados pueden emitir su voto. */
    ABIERTA,

    /** Elección suspendida temporalmente. No se aceptan votos. */
    PAUSADA,

    /** Cierre solicitado por un custodio. Requiere segunda aprobación. */
    CIERRE_SOLICITADO,

    /** Elección cerrada. Resultados en proceso de escrutinio. Aún no publicados. */
    CERRADA,

    /** Escrutinio validado y acta aprobada. Resultados publicables. */
    FINALIZADA
}
