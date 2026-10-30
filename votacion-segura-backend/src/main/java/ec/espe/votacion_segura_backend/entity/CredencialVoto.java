package ec.espe.votacion_segura_backend.entity;

import ec.espe.votacion_segura_backend.enums.EstadoCredencial;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;

import java.time.Instant;
import java.util.UUID;

/**
 * Credencial de voto de un solo uso.
 *
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  INVARIANTES DE SEGURIDAD — CRÍTICOS                                     ║
 * ║                                                                           ║
 * ║  1. El token original NUNCA se almacena; solo su hash SHA-256.            ║
 * ║  2. El token original NUNCA aparece en logs de producción.                ║
 * ║  3. Esta entidad NO contiene socio_id ni candidatura_id.                  ║
 * ║  4. La relación con Participacion (participacion_ref_id) permite:         ║
 * ║       a) Invalidar credenciales previas al emitir una nueva.              ║
 * ║       b) Marcar la participación como VOTADO al consumir la credencial.   ║
 * ║       c) Controlar re-emisión sin exponer identidad del socio al voto.    ║
 * ║  5. Una vez USADA, no puede reutilizarse (verificado en BD y en lógica).  ║
 * ║  6. Tiene expiración breve (configurable, default 30 minutos).            ║
 * ║  7. El token tiene al menos 256 bits de entropía aleatoria criptográfica. ║
 * ║                                                                           ║
 * ║  FLUJO: participacion_ref_id → participaciones → socio_id                ║
 * ║  NO existe FK desde votos hacia credenciales_voto.                        ║
 * ║  Esto impide navegación directa voto→socio.                               ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
@Entity
@DynamicUpdate
@Table(
    name = "credenciales_voto",
    indexes = {
        @Index(name = "idx_credencial_token_hash", columnList = "token_hash", unique = true),
        @Index(name = "idx_credencial_eleccion_estado", columnList = "eleccion_id, estado"),
        @Index(name = "idx_credencial_participacion", columnList = "participacion_ref_id"),
        @Index(name = "idx_credencial_expiracion", columnList = "fecha_expiracion")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CredencialVoto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Hash SHA-256 del token aleatorio de 256 bits.
     * El token original se entrega al socio y se descarta del servidor.
     * NUNCA registrar el valor de este campo en logs.
     */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "eleccion_id", nullable = false, updatable = false)
    private Eleccion eleccion;

    /**
     * Referencia a la participación que originó esta credencial.
     * Permite:
     *   - Invalidar credenciales antiguas al emitir una nueva.
     *   - Marcar la participación como VOTADO al consumir la credencial.
     *   - Verificar que no se haya votado ya.
     *
     * NO permite reconstruir relación directa con el voto porque:
     *   - El voto no tiene referencia a credencial ni a participación.
     *   - La única correlación posible es temporal (riesgo declarado y documentado).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participacion_ref_id", nullable = false, updatable = false)
    private Participacion participacionRef;

    @Column(name = "fecha_expiracion", nullable = false)
    private Instant fechaExpiracion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private EstadoCredencial estado = EstadoCredencial.PENDIENTE;

    /** Timestamp de consumo del token. Null hasta que se use. */
    @Column(name = "used_at")
    private Instant usedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;
}
