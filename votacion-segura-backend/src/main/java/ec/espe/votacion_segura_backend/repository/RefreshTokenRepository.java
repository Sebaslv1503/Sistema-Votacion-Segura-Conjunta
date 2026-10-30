package ec.espe.votacion_segura_backend.repository;

import ec.espe.votacion_segura_backend.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from RefreshToken t where t.tokenHash=:hash")
    Optional<RefreshToken> lockByHash(@Param("hash") String hash);
    java.util.List<RefreshToken> findByUsuarioIdAndRevocadoFalse(UUID usuarioId);
}
