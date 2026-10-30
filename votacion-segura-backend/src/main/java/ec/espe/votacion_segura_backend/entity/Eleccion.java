package ec.espe.votacion_segura_backend.entity;

import ec.espe.votacion_segura_backend.enums.EstadoEleccion;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.DynamicUpdate;

import java.time.Instant;
import java.util.UUID;

/**
 * Representa una elección dentro del sistema.
 *
 * Ciclo de vida controlado:
 *   BORRADOR → PROGRAMADA → ABIERTA ↔ PAUSADA → CIERRE_SOLICITADO → CERRADA → FINALIZADA
 *
 * Solo se puede votar en estado ABIERTA.
 * Las transiciones de estado se validan en EleccionService.
 * El cierre requiere doble autorización de CUSTODIO_ELECTORAL (ver CierreEleccion).
 */
@Entity
@DynamicUpdate
@Table(
    name = "elecciones",
    indexes = {
        @Index(name = "idx_elecciones_estado", columnList = "estado"),
        @Index(name = "idx_elecciones_fecha_inicio", columnList = "fecha_inicio")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Eleccion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @Column(name = "descripcion", length = 1000)
    private String descripcion;

    /** Inicio planificado de la jornada electoral. */
    @Column(name = "fecha_inicio", nullable = false)
    private Instant fechaInicio;

    /** Fin planificado de la jornada electoral. */
    @Column(name = "fecha_fin", nullable = false)
    private Instant fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    @Builder.Default
    private EstadoEleccion estado = EstadoEleccion.BORRADOR;

    /** Timestamp efectivo de apertura (cuando un custodio ejecutó la apertura). */
    @Column(name = "opened_at")
    private Instant openedAt;

    /** Timestamp efectivo de cierre (cuando el segundo custodio aprobó el cierre). */
    @Column(name = "closed_at")
    private Instant closedAt;

    /** ID del primer custodio que solicitó el cierre. */
    @Column(name = "solicitante_cierre_id")
    private UUID solicitanteCierreId;

    @Column(name = "aprobador_cierre_id")
    private UUID aprobadorCierreId;

    /** Timestamp de la solicitud de cierre del primer custodio. */
    @Column(name = "cierre_solicitado_at")
    private Instant cierreSolicitadoAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
