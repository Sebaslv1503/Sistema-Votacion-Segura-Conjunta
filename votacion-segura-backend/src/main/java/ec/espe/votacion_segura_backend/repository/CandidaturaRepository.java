package ec.espe.votacion_segura_backend.repository;

import ec.espe.votacion_segura_backend.entity.Candidatura;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CandidaturaRepository extends JpaRepository<Candidatura, UUID> {
    List<Candidatura> findByEleccionIdAndActivoTrue(UUID eleccionId);
    List<Candidatura> findByEleccionId(UUID eleccionId);
    Optional<Candidatura> findByIdAndEleccionIdAndActivoTrue(UUID id, UUID eleccionId);
}
