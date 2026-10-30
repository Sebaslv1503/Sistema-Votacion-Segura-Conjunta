package ec.espe.votacion_segura_backend.repository;

import ec.espe.votacion_segura_backend.entity.ActaResultado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

public interface ActaResultadoRepository extends JpaRepository<ActaResultado, UUID> {
    Optional<ActaResultado> findByEleccionId(UUID eleccionId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from ActaResultado a where a.eleccion.id=:id")
    Optional<ActaResultado> lockByEleccionId(@Param("id") UUID eleccionId);
}
