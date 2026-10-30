package ec.espe.votacion_segura_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Voto depositado en la urna electrónica.
 *
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  INVARIANTES DE PRIVACIDAD — CRÍTICOS — NO NEGOCIABLES                   ║
 * ║                                                                           ║
 * ║  Esta entidad NO contiene y NUNCA debe contener:                          ║
 * ║    - socio_id          → NO                                               ║
 * ║    - numero_socio      → NO                                               ║
 * ║    - cedula            → NO                                               ║
 * ║    - correo            → NO                                               ║
 * ║    - username          → NO                                               ║
 * ║    - session_id        → NO                                               ║
 * ║    - ip_address        → NO                                               ║
 * ║    - credencial_id     → NO (no hay FK hacia credenciales_voto)           ║
 * ║    - participacion_id  → NO (no hay FK hacia participaciones)             ║
 * ║                                                                           ║
 * ║  No existe ninguna relación JPA que permita navegar desde Voto           ║
 * ║  hasta un Socio o Participacion.                                          ║
 * ║                                                                           ║
 * ║  OPERACIONES PROHIBIDAS (controladas también en la BD):                   ║
 * ║    - UPDATE de cualquier campo de esta tabla                              ║
 * ║    - DELETE de registros de esta tabla                                    ║
 * ║                                                                           ║
 * ║  LIMITACIÓN DECLARADA:                                                    ║
 * ║  La correlación temporal entre credencial consumida y voto registrado     ║
 * ║  representa un riesgo residual de privacidad. La fecha_registro está      ║
 * ║  truncada al minuto para mitigar (no eliminar) este riesgo.               ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
@Entity
@Table(
    name = "votos",
    indexes = {
        @Index(name = "idx_votos_eleccion", columnList = "eleccion_id"),
        @Index(name = "idx_votos_eleccion_candidatura", columnList = "eleccion_id, candidatura_id"),
        @Index(name = "idx_votos_fecha", columnList = "fecha_registro")
    }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Voto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "eleccion_id", nullable = false, updatable = false)
    private Eleccion eleccion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidatura_id", nullable = false, updatable = false)
    private Candidatura candidatura;

    /**
     * Fecha de registro truncada al minuto.
     * NO usar timestamp exacto para reducir riesgo de correlación temporal.
     * Limitación documentada: correlación probabilística aún es posible.
     */
    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private Instant fechaRegistro;

    /**
     * Hash de integridad del voto: SHA-256(eleccion_id + candidatura_id + id + timestamp).
     * Permite detectar manipulación del registro, no garantiza anonimato.
     * Inmutable tras la inserción.
     */
    @Column(name = "hash_integridad", nullable = false, updatable = false, length = 64)
    private String hashIntegridad;

    // Nota: No se define @Setter deliberadamente para reforzar la inmutabilidad
    // del voto una vez insertado. Toda la entidad es de solo escritura inicial.
}
