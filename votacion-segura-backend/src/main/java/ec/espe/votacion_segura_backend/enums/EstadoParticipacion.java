package ec.espe.votacion_segura_backend.enums;

/**
 * Estado de participación de un socio en una elección.
 *
 * IMPORTANTE: Esta entidad NO contiene información sobre la candidatura elegida.
 * La relación socio-candidatura NUNCA se almacena en ninguna tabla.
 */
public enum EstadoParticipacion {

    /** El socio tiene una credencial emitida pero todavía no ha votado. */
    PENDIENTE,

    /** El socio ha emitido su voto. Estado final, no reversible. */
    VOTADO,

    /** El socio fue registrado pero aún no solicitó credencial. Estado inicial. */
    SIN_CREDENCIAL
}
