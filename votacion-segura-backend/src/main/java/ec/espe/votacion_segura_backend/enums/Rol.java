package ec.espe.votacion_segura_backend.enums;

/**
 * Roles de usuario del sistema de votación.
 * Cada rol tiene permisos estrictamente definidos (ver SecurityConfig).
 * Las autorizaciones se aplican en el backend; no dependen del frontend.
 */
public enum Rol {

    /** Socio habilitado para votar. Acceso mínimo: consultar candidaturas, emitir voto. */
    SOCIO_VOTANTE,

    /** Administrador del padrón electoral. No puede consultar ni modificar votos. */
    GESTOR_PADRON,

    /** Administrador técnico del sistema. No puede modificar votos ni ver selecciones. */
    ADMINISTRADOR_TECNICO,

    /** Custodio electoral. Autoriza apertura, cierre y aprueba actas. Doble firma requerida. */
    CUSTODIO_ELECTORAL,

    /** Auditor independiente. Solo lectura de eventos y resultados. No edita nada. */
    AUDITOR_INDEPENDIENTE,

    /** Soporte técnico. Acceso técnico limitado. No puede ver selecciones ni contraseñas. */
    SOPORTE
}
