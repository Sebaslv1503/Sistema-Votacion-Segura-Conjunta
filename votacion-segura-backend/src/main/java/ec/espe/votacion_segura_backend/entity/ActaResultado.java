package ec.espe.votacion_segura_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

/**
 * Acta de resultados de una elección.
 *
 * Solo se genera DESPUÉS del cierre y validación del escrutinio.
 * Requiere aprobación de DOS custodios electorales distintos.
 *
 * Los resultados son SIEMPRE AGREGADOS por candidatura.
 * NUNCA se incluyen votos individuales asociados a identidades.
 *
 * hashManifiesto: SHA-256 del JSON de conteos + total_votos + eleccion_id + fecha_cierre.
 * Permite verificar integridad del acta.
 */
@Entity
@DynamicUpdate
@Table(
    name = "actas_resultado",
    indexes = {
        @Index(name = "idx_acta_eleccion", columnList = "eleccion_id", unique = true)
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActaResultado {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "eleccion_id", nullable = false, unique = true, updatable = false)
    private Eleccion eleccion;

    @Column(name = "total_votos", nullable = false)
    private long totalVotos;

    @Column(name = "fecha_cierre", nullable = false)
    private Instant fechaCierre;

    /**
     * Conteos agregados por candidatura en formato JSON.
     * Estructura: [{"candidaturaId": "...", "nombre": "...", "votos": N}, ...]
     * NUNCA contiene información que vincule un voto con un socio.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "conteos_json", nullable = false, columnDefinition = "jsonb")
    private JsonNode conteosJson;

    /**
     * Hash de integridad del acta.
     * SHA-256(eleccion_id + total_votos + conteos_json + fecha_cierre).
     * Permite verificar que el acta no ha sido alterada.
     */
    @Column(name = "hash_manifiesto", nullable = false, length = 64)
    private String hashManifiesto;

    /** Estado del acta: BORRADOR, PENDIENTE_APROBACION, APROBADA, PUBLICADA. */
    @Column(name = "estado", nullable = false, length = 30)
    @Builder.Default
    private String estado = "BORRADOR";

    /** Primer custodio que aprueba el acta. */
    @Column(name = "aprobador1_id")
    private UUID aprobador1Id;

    /** Segundo custodio que aprueba el acta (doble firma requerida). */
    @Column(name = "aprobador2_id")
    private UUID aprobador2Id;

    /** Timestamp de aprobación final (cuando el segundo custodio firma). */
    @Column(name = "fecha_aprobacion")
    private Instant fechaAprobacion;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;
}
