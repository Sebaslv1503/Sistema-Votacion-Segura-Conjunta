package ec.espe.votacion_segura_backend.repository;

import ec.espe.votacion_segura_backend.entity.Participacion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface ParticipacionRepository extends JpaRepository<Participacion, UUID> {
    Optional<Participacion> findByEleccionIdAndSocioId(UUID eleccionId, UUID socioId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Participacion p where p.eleccion.id=:eleccion and p.socio.id=:socio")
    Optional<Participacion> lockForElectionAndMember(@Param("eleccion") UUID eleccionId, @Param("socio") UUID socioId);
}
