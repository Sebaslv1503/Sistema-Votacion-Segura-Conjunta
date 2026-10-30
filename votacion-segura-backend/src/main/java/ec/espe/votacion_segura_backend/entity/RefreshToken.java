package ec.espe.votacion_segura_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;

import java.time.Instant;
import java.util.UUID;

/**
 * Refresh token para rotación de sesiones JWT.
 *
 * SEGURIDAD:
 * - Solo se almacena el hash SHA-256 del token, nunca el token original.
 * - Cada uso del refresh token genera uno nuevo (rotación).
 * - La detección de reutilización revoca TODOS los tokens de la sesión.
 * - TTL igual a la duración de la jornada electoral (configurable, default 8h).
 */
@Entity
@DynamicUpdate
@Table(
    name = "refresh_tokens",
    indexes = {
        @Index(name = "idx_refresh_token_hash", columnList = "token_hash", unique = true),
        @Index(name = "idx_refresh_usuario", columnList = "usuario_id"),
        @Index(name = "idx_refresh_expiracion", columnList = "fecha_expiracion")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Hash SHA-256 del refresh token opaco de 256 bits.
     * NUNCA registrar este valor en logs.
     */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, updatable = false)
    private Usuario usuario;

    @Column(name = "fecha_expiracion", nullable = false)
    private Instant fechaExpiracion;

    /** Si el token ha sido revocado (logout o detección de reutilización). */
    @Column(name = "revocado", nullable = false)
    @Builder.Default
    private boolean revocado = false;

    /** IP de origen del login que generó este token (solo para auditoría técnica, no como ID). */
    @Column(name = "ip_origen", length = 45)
    private String ipOrigen;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;
}
