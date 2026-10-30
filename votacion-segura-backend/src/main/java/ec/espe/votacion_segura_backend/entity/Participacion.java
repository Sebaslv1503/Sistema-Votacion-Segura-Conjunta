package ec.espe.votacion_segura_backend.entity;

import ec.espe.votacion_segura_backend.enums.EstadoParticipacion;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;

import java.time.Instant;
import java.util.UUID;

/**
 * Registro de participación de un socio en una elección.
 *
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  INVARIANTE DE PRIVACIDAD — CRÍTICO                                      ║
 * ║                                                                           ║
 * ║  Esta entidad NO contiene:                                                ║
 * ║    - candidatura_id  (nunca)                                              ║
 * ║    - voto_id         (nunca)                                              ║
 * ║    - selección       (nunca)                                              ║
 * ║                                                                           ║
 * ║  Solo registra QUE el socio participó, NUNCA POR QUIÉN votó.             ║
 * ║                                                                           ║
 * ║  La restricción UNIQUE(eleccion_id, socio_id) en BD garantiza que        ║
 * ║  un socio no pueda registrar participación más de una vez por elección.   ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * LIMITACIÓN DECLARADA:
 * La correlación temporal entre el momento de emisión de credencial y el registro
 * del voto podría permitir inferencia probabilística a un adversario con acceso
 * directo a la BD. Esto es una limitación conocida del prototipo (no implementa
 * mezclas criptográficas ni criptografía homomórfica).
 */
@Entity
@DynamicUpdate
@Table(
    name = "participaciones",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_participacion_eleccion_socio",
            columnNames = {"eleccion_id", "socio_id"}
        )
    },
    indexes = {
        @Index(name = "idx_participaciones_eleccion_socio", columnList = "eleccion_id, socio_id"),
        @Index(name = "idx_participaciones_socio", columnList = "socio_id"),
        @Index(name = "idx_participaciones_estado", columnList = "eleccion_id, estado_participacion")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Participacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "eleccion_id", nullable = false, updatable = false)
    private Eleccion eleccion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "socio_id", nullable = false, updatable = false)
    private Socio socio;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_participacion", nullable = false, length = 20)
    @Builder.Default
    private EstadoParticipacion estadoParticipacion = EstadoParticipacion.SIN_CREDENCIAL;

    /**
     * Fecha de la operación con granularidad limitada al minuto.
     * Se trunca para reducir la capacidad de correlación temporal exacta con los votos.
     * Limitación: no elimina completamente el riesgo de correlación temporal.
     */
    @Column(name = "fecha_operativa")
    private Instant fechaOperativa;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;
}
