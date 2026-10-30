package ec.espe.votacion_segura_backend.repository;

import ec.espe.votacion_segura_backend.entity.Voto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface VotoRepository extends JpaRepository<Voto, UUID> {
    @Query("select v.candidatura.id, count(v) from Voto v where v.eleccion.id=:eleccion group by v.candidatura.id")
    List<Object[]> contarPorCandidatura(@Param("eleccion") UUID eleccionId);
    long countByEleccionId(UUID eleccionId);
}
