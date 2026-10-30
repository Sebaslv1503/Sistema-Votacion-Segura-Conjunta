package ec.espe.votacion_segura_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import ec.espe.votacion_segura_backend.auth.CompatiblePasswordEncoder;
import ec.espe.votacion_segura_backend.auth.JwtAuthenticationFilter;
import ec.espe.votacion_segura_backend.auth.UsuarioDetailsService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuración de Spring Security.
 *
 * ESTRATEGIA:
 * - JWT stateless para el access token (sin sesión HTTP).
 * - Refresh token en cookie HttpOnly con SameSite=Strict (protección CSRF).
 * - CORS restrictivo: solo origen explícitamente configurado.
 * - CSRF deshabilitado para la API REST (tokens JWT en Authorization header, inmunes a CSRF).
 * - El endpoint /auth/refresh usa cookie → protegido por SameSite=Strict + validación de Origin.
 * - @EnableMethodSecurity: autorización por roles en @PreAuthorize a nivel de método.
 *
 * NOTAS DE SEGURIDAD:
 * - No se usa @CrossOrigin("*") en ningún controlador.
 * - Las autorizaciones NO dependen del frontend; se aplican en el backend.
 * - Los endpoints de Swagger solo están disponibles en perfil dev.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Value("${app.cors.allowed-origin:http://localhost:3000}")
    private String allowedOrigin;

    /**
     * BCrypt supports the legacy seeded hashes and newly encoded accounts.
     */
    @Bean public PasswordEncoder passwordEncoder() { return new CompatiblePasswordEncoder(); }

    @Bean
    public AuthenticationProvider authenticationProvider(UsuarioDetailsService users, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        return provider;
    }

    @Bean public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception { return configuration.getAuthenticationManager(); }

    /**
     * Cadena de filtros de seguridad.
     *
     * FASE 1: Configuración base que permite levantar la aplicación.
     * Los filtros JWT se añadirán en la FASE 3.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter,
                                                   AuthenticationProvider provider) throws Exception {
        http
            // ── Sin sesión HTTP (stateless JWT) ───────────────────────
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // ── CSRF: deshabilitado para API REST con JWT en header ────
            // Los tokens JWT en Authorization header son inmunes a CSRF por naturaleza.
            // El refresh token usa cookie HttpOnly + SameSite=Strict (mitigación adicional).
            .csrf(AbstractHttpConfigurer::disable)

            // ── CORS restrictivo ───────────────────────────────────────
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authenticationProvider(provider)
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)

            // ── Headers de seguridad ───────────────────────────────────
            .headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                .contentTypeOptions(cto -> {})
                .httpStrictTransportSecurity(hsts -> hsts
                    .maxAgeInSeconds(31536000)
                    .includeSubDomains(true))
            )

            // ── Autorización de rutas ──────────────────────────────────
            .authorizeHttpRequests(auth -> auth
                // Endpoints públicos
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/otp/request").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/otp/verify").permitAll()

                // Swagger (solo en perfil dev — se deshabilita en prod via properties)
                .requestMatchers(
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/api-docs",
                    "/api-docs/**",
                    "/v3/api-docs/**"
                ).permitAll()

                // Actuator health (sin autenticación para load balancer)
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/**").authenticated()

                // Todo lo demás requiere autenticación
                .anyRequest().authenticated()
            );

        return http.build();
    }

    /**
     * Configuración CORS restrictiva.
     *
     * SEGURIDAD:
     * - Solo el origen configurado en CORS_ALLOWED_ORIGIN está permitido.
     * - allowCredentials(true) es necesario para las cookies de refresh token.
     * - No se usa wildcard "*" en origins, methods ni headers.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Solo el origen del frontend React — nunca wildcard
        config.setAllowedOrigins(List.of(allowedOrigin));

        // Métodos HTTP permitidos
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Headers permitidos en requests
        config.setAllowedHeaders(List.of(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "X-Correlation-ID"
        ));

        // Headers expuestos al cliente
        config.setExposedHeaders(List.of("X-Correlation-ID"));

        // Necesario para cookies HttpOnly del refresh token
        config.setAllowCredentials(true);

        // Cache preflight por 1 hora
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
