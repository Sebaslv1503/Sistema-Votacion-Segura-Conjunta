package ec.espe.votacion_segura_backend.repository;

import ec.espe.votacion_segura_backend.entity.CredencialVoto;
import ec.espe.votacion_segura_backend.enums.EstadoCredencial;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface CredencialVotoRepository extends JpaRepository<CredencialVoto, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CredencialVoto c where c.tokenHash=:hash")
    Optional<CredencialVoto> lockByTokenHash(@Param("hash") String hash);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CredencialVoto c where c.participacionRef.id=:participacionId and c.estado=:estado")
    java.util.List<CredencialVoto> lockByParticipationAndState(@Param("participacionId") UUID participacionId, @Param("estado") EstadoCredencial estado);
}
