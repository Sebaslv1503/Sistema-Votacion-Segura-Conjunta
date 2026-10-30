package ec.espe.votacion_segura_backend.entity;

import ec.espe.votacion_segura_backend.enums.EstadoSocio;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.DynamicUpdate;

import java.time.Instant;
import java.util.UUID;

/**
 * Registro de un socio en el padrón electoral.
 *
 * PRIVACIDAD:
 * - No se almacena cédula, religión, preferencia política, domicilio,
 *   ingresos, información médica, discapacidad ni biometría.
 * - El correo electrónico es opcional y solo se usa para notificaciones.
 * - La relación con Usuario (1:1) permite autenticación sin exponer datos del padrón.
 *
 * SEPARACIÓN:
 * - Esta entidad NO tiene relación con Voto ni con CredencialVoto.
 * - La relación socio-candidatura NUNCA se almacena en ninguna tabla.
 */
@Entity
@DynamicUpdate
@Table(
    name = "socios",
    indexes = {
        @Index(name = "idx_socios_numero_socio", columnList = "numero_socio", unique = true),
        @Index(name = "idx_socios_estado", columnList = "estado_habilitacion"),
        @Index(name = "idx_socios_usuario_id", columnList = "usuario_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Socio {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Número único del socio en el padrón. Inmutable tras la creación. */
    @Column(name = "numero_socio", nullable = false, unique = true, length = 20)
    private String numeroSocio;

    @Column(name = "nombres", nullable = false, length = 100)
    private String nombres;

    @Column(name = "apellidos", nullable = false, length = 100)
    private String apellidos;

    /**
     * Correo electrónico para notificaciones (opcional).
     * No se usa como identificador de votación.
     * No se incluye en registros de auditoría de votación.
     */
    @Column(name = "correo_electronico", length = 150)
    private String correoElectronico;

    /** Si el correo ha sido verificado por el gestor del padrón. */
    @Column(name = "correo_verificado", nullable = false)
    @Builder.Default
    private boolean correoVerificado = false;

    /** Estado de habilitación para participar en elecciones. */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_habilitacion", nullable = false, length = 30)
    @Builder.Default
    private EstadoSocio estadoHabilitacion = EstadoSocio.PENDIENTE_VERIFICACION;

    /** Si el registro está activo en el sistema. */
    @Column(name = "activo", nullable = false)
    @Builder.Default
    private boolean activo = true;

    /**
     * Cuenta de usuario asociada (1:1).
     * Permite autenticación sin mezclar datos del padrón con datos de acceso.
     * Puede ser null si el socio aún no tiene cuenta creada.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", unique = true)
    private Usuario usuario;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
