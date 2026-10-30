package ec.espe.votacion_segura_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades de configuración JWT leídas desde application.properties.
 * Todos los valores se inyectan desde variables de entorno (nunca hardcodeados).
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        long accessExpirationMs,
        long refreshExpirationMs
) {}
