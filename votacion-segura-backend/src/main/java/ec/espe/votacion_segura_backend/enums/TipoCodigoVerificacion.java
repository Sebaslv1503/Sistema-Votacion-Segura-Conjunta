package ec.espe.votacion_segura_backend.enums;

/**
 * Tipo de código de verificación OTP.
 */
public enum TipoCodigoVerificacion {

    /** Código para verificación de segundo factor al iniciar sesión. */
    MFA_LOGIN,

    /** Código para verificación antes de emitir credencial de voto. */
    VERIFICACION_VOTO,

    /** Código para operaciones privilegiadas (apertura, cierre de elección). */
    OPERACION_PRIVILEGIADA,

    /** Código para restablecimiento de contraseña. */
    RESET_PASSWORD
}
