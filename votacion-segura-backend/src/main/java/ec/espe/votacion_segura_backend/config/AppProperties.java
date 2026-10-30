package ec.espe.votacion_segura_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades de configuración de la aplicación.
 * Lee desde el prefijo 'app' en application.properties.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Security security,
        Credencial credencial,
        Otp otp,
        Cors cors,
        Mail mail
) {

    public record Security(
            int maxIntentosLogin,
            int bloqueoMinutos
    ) {}

    public record Credencial(
            int expiracionMinutos
    ) {}

    public record Otp(
            int expiracionMinutos,
            int maxIntentos,
            boolean logCodeDev
    ) {}

    public record Cors(
            String allowedOrigin
    ) {}

    public record Mail(
            String from
    ) {}
}
