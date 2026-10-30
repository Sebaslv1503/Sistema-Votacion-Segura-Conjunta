package ec.espe.votacion_segura_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.DynamicUpdate;

import java.time.Instant;
import java.util.UUID;

/**
 * Candidatura registrada para una elección específica.
 *
 * Una candidatura pertenece a exactamente una elección.
 * Los socios consultan candidaturas para tomar su decisión de voto.
 * La selección del socio NUNCA se almacena junto con su identidad.
 */
@Entity
@DynamicUpdate
@Table(
    name = "candidaturas",
    indexes = {
        @Index(name = "idx_candidaturas_eleccion", columnList = "eleccion_id"),
        @Index(name = "idx_candidaturas_activo", columnList = "eleccion_id, activo")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Candidatura {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Elección a la que pertenece esta candidatura. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "eleccion_id", nullable = false, updatable = false)
    private Eleccion eleccion;

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    /** Propuesta o plan de trabajo. Opcional. Visible para socios. */
    @Column(name = "propuesta", columnDefinition = "TEXT")
    private String propuesta;

    /** Si la candidatura está activa y visible para los votantes. */
    @Column(name = "activo", nullable = false)
    @Builder.Default
    private boolean activo = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
