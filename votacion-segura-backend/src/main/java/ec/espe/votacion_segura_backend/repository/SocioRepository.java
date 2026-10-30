package ec.espe.votacion_segura_backend.repository;

import ec.espe.votacion_segura_backend.entity.Socio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
import java.util.UUID;

public interface SocioRepository extends JpaRepository<Socio, UUID> {
    Optional<Socio> findByNumeroSocio(String numeroSocio);
    Optional<Socio> findByUsuarioUsername(String username);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Socio s where s.id=:id")
    Optional<Socio> lockById(@Param("id") UUID id);
    Page<Socio> findByNumeroSocioContainingIgnoreCaseOrNombresContainingIgnoreCaseOrApellidosContainingIgnoreCase(String numero, String nombres, String apellidos, Pageable pageable);
}
