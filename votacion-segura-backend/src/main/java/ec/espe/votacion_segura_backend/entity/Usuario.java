package ec.espe.votacion_segura_backend.entity;

import ec.espe.votacion_segura_backend.enums.Rol;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.DynamicUpdate;

import java.time.Instant;
import java.util.UUID;

/**
 * Cuenta de acceso al sistema.
 *
 * SEGURIDAD:
 * - passwordHash almacena únicamente el hash BCrypt/Argon2. Nunca el texto plano.
 * - El campo password nunca se devuelve en DTOs de respuesta.
 * - Los intentos fallidos se acumulan para bloqueo temporal.
 * - Ningún endpoint ordinario permite recuperar el passwordHash.
 */
@Entity
@DynamicUpdate
@Table(
    name = "usuarios",
    indexes = {
        @Index(name = "idx_usuarios_username", columnList = "username", unique = true)
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Nombre de usuario único. Mínimo 3 caracteres, máximo 50. */
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    /**
     * Hash de contraseña (BCrypt o Argon2id).
     * NUNCA almacenar ni registrar el valor original.
     * NUNCA serializar este campo en respuestas HTTP.
     */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /** Rol asignado en el sistema. Determina los permisos del usuario. */
    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 30)
    private Rol rol;

    /** Si false, el usuario no puede autenticarse. */
    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private boolean enabled = true;

    /** Si false, la cuenta está bloqueada por intentos fallidos u otra causa. */
    @Column(name = "account_non_locked", nullable = false)
    @Builder.Default
    private boolean accountNonLocked = true;

    /** Contador de intentos fallidos consecutivos de autenticación. */
    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private int failedLoginAttempts = 0;

    /** Timestamp del último inicio de sesión exitoso. Null si nunca inició sesión. */
    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    /** Timestamp de bloqueo temporal. Null si no está bloqueado. */
    @Column(name = "locked_until")
    private Instant lockedUntil;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
