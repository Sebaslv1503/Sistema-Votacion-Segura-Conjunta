package ec.espe.votacion_segura_backend.entity;

import ec.espe.votacion_segura_backend.enums.TipoCodigoVerificacion;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Código OTP de verificación de un solo uso.
 *
 * SEGURIDAD:
 * - Solo se almacena el hash BCrypt del código numérico, nunca el código original.
 * - El código tiene expiración breve (default: 10 minutos).
 * - Máximo 5 intentos fallidos antes de invalidar el código.
 * - Una vez usado, no puede reutilizarse.
 * - El código NUNCA aparece en logs de producción.
 * - En modo desarrollo (perfil 'dev'), el código se registra en log de manera controlada.
 */
@Entity
@Table(
    name = "codigos_verificacion",
    indexes = {
        @Index(name = "idx_codigo_usuario_tipo", columnList = "usuario_id, tipo, usado"),
        @Index(name = "idx_codigo_expiracion", columnList = "fecha_expiracion")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodigoVerificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, updatable = false)
    private Usuario usuario;

    /**
     * Hash BCrypt del código OTP de 6 dígitos.
     * NUNCA registrar este valor en logs.
     * NUNCA devolver este valor en respuestas HTTP.
     */
    @Column(name = "codigo_hash", nullable = false)
    private String codigoHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoCodigoVerificacion tipo;

    @Column(name = "fecha_expiracion", nullable = false)
    private Instant fechaExpiracion;

    /** Intentos fallidos de verificación. Al llegar a 5 se invalida el código. */
    @Column(name = "intentos_fallidos", nullable = false)
    @Builder.Default
    private int intentosFallidos = 0;

    /** Si el código ya fue usado exitosamente. Estado final. */
    @Column(name = "usado", nullable = false)
    @Builder.Default
    private boolean usado = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;
}
