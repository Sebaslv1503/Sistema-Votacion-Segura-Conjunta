package ec.espe.votacion_segura_backend.repository;

import ec.espe.votacion_segura_backend.entity.Eleccion;
import ec.espe.votacion_segura_backend.enums.EstadoEleccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EleccionRepository extends JpaRepository<Eleccion, UUID> {
    List<Eleccion> findByEstado(EstadoEleccion estado);
    Optional<Eleccion> findByIdAndEstado(UUID id, EstadoEleccion estado);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Eleccion e where e.id=:id")
    Optional<Eleccion> lockById(@Param("id") UUID id);
}
