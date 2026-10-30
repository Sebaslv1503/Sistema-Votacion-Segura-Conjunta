package ec.espe.votacion_segura_backend.repository;

import ec.espe.votacion_segura_backend.entity.CodigoVerificacion;
import ec.espe.votacion_segura_backend.enums.TipoCodigoVerificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CodigoVerificacionRepository extends JpaRepository<CodigoVerificacion, UUID> {
    List<CodigoVerificacion> findByUsuarioIdAndTipoAndUsadoFalseOrderByCreatedAtDesc(UUID usuarioId, TipoCodigoVerificacion tipo);
}
