package ec.espe.votacion_segura_backend.repository;

import ec.espe.votacion_segura_backend.entity.EventoAuditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface EventoAuditoriaRepository extends JpaRepository<EventoAuditoria, UUID> {
    Page<EventoAuditoria> findAllByOrderByFechaEventoDesc(Pageable pageable);
    Page<EventoAuditoria> findByActorIdOrderByFechaEventoDesc(UUID actorId, Pageable pageable);
}
