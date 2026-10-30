package ec.espe.votacion_segura_backend.entity;

import ec.espe.votacion_segura_backend.enums.TipoEventoAuditoria;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento de auditoría del sistema.
 *
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  REGLAS DE AUDITORÍA                                                      ║
 * ║                                                                           ║
 * ║  NUNCA registrar en esta entidad:                                         ║
 * ║    - Contraseñas (ni hash)                                                ║
 * ║    - Tokens (ni hash)                                                     ║
 * ║    - Códigos OTP                                                          ║
 * ║    - Selección de candidatura del voto                                    ║
 * ║    - Relación entre socio y candidatura                                   ║
 * ║    - Payloads sensibles                                                   ║
 * ║    - Datos personales innecesarios                                        ║
 * ║                                                                           ║
 * ║  OPERACIONES DESDE LA APLICACIÓN:                                         ║
 * ║    - Solo INSERT (el usuario votacion_app no tiene UPDATE ni DELETE)      ║
 * ║    - Los registros existentes son inmutables desde la aplicación          ║
 * ║                                                                           ║
 * ║  INTEGRIDAD:                                                              ║
 * ║    - hash_evento: SHA-256(hash_anterior + contenido_evento)               ║
 * ║    - Permite detectar alteraciones en la cadena                           ║
 * ║    - NO garantiza inmutabilidad sin protección externa (HSM, storage)     ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
@Entity
@Table(
    name = "eventos_auditoria",
    indexes = {
        @Index(name = "idx_auditoria_fecha", columnList = "fecha_evento"),
        @Index(name = "idx_auditoria_actor", columnList = "actor_id"),
        @Index(name = "idx_auditoria_tipo", columnList = "tipo_evento"),
        @Index(name = "idx_auditoria_correlation", columnList = "correlation_id")
    }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventoAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * ID del usuario que realizó la acción.
     * Puede ser null para eventos de sistema sin actor autenticado.
     * NO se almacena username directamente para evitar exposición accidental.
     */
    @Column(name = "actor_id")
    private UUID actorId;

    /** Rol del actor en el momento del evento. */
    @Column(name = "rol_actor", length = 30)
    private String rolActor;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento", nullable = false, length = 50)
    private TipoEventoAuditoria tipoEvento;

    /** Descripción de la acción ejecutada. Saneada y sin datos sensibles. */
    @Column(name = "accion", nullable = false, length = 300)
    private String accion;

    /** Resultado de la operación: SUCCESS, FAILURE, UNAUTHORIZED. */
    @Column(name = "resultado", nullable = false, length = 20)
    private String resultado;

    /** Timestamp del evento. Inmutable. */
    @CreationTimestamp
    @Column(name = "fecha_evento", updatable = false, nullable = false)
    private Instant fechaEvento;

    /**
     * Identificador de correlación técnica (UUID de la request HTTP).
     * Permite trazar una operación a través de múltiples eventos.
     * NO es el ID de sesión del usuario.
     */
    @Column(name = "correlation_id", length = 36)
    private String correlationId;

    /**
     * Detalles adicionales en formato JSON.
     * DEBEN estar saneados: sin contraseñas, tokens, OTPs ni selecciones de voto.
     * Solo metadatos técnicos seguros (IDs de recursos, estados, conteos).
     */
    @Column(name = "detalles", columnDefinition = "TEXT")
    private String detalles;

    /**
     * Hash encadenado: SHA-256(hash_del_evento_anterior + id + tipo + accion + fecha).
     * Detecta alteraciones en la secuencia de eventos.
     * LIMITACIÓN: No garantiza inmutabilidad real sin almacenamiento externo protegido.
     */
    @Column(name = "hash_evento", length = 64)
    private String hashEvento;

    // No se define @Setter: los eventos de auditoría son inmutables una vez creados.
}
