package ec.espe.votacion_segura_backend.enums;

/**
 * Estado de habilitación de un socio en el padrón electoral.
 */
public enum EstadoSocio {

    /** Socio habilitado para votar en la elección actual. */
    HABILITADO,

    /** Socio inhabilitado (mora, sanción u otra causa administrativa). No puede votar. */
    INHABILITADO,

    /** Socio pendiente de verificación de datos. No puede votar hasta validación. */
    PENDIENTE_VERIFICACION,

    /** Socio dado de baja del padrón. Registro histórico. */
    BAJA
}
