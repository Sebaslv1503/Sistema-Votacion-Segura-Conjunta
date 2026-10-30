package ec.espe.votacion_segura_backend.auditoria;

import ec.espe.votacion_segura_backend.entity.EventoAuditoria;
import ec.espe.votacion_segura_backend.repository.EventoAuditoriaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auditoria")
@PreAuthorize("hasAnyRole('AUDITOR_INDEPENDIENTE','ADMINISTRADOR_TECNICO')")
public class AuditoriaController {
    private final EventoAuditoriaRepository repository;
    public AuditoriaController(EventoAuditoriaRepository repository) { this.repository = repository; }
    @GetMapping
    public Page<AuditDto> listar(@RequestParam(required = false) UUID actorId, Pageable pageable) {
        Page<EventoAuditoria> events = actorId == null ? repository.findAllByOrderByFechaEventoDesc(pageable) : repository.findByActorIdOrderByFechaEventoDesc(actorId, pageable);
        return events.map(AuditDto::from);
    }
    public record AuditDto(UUID id, UUID actorId, String rolActor, String tipo, String accion, String resultado, Instant fecha, String correlationId, String detalles, String hash) {
        static AuditDto from(EventoAuditoria e) { return new AuditDto(e.getId(), e.getActorId(), e.getRolActor(), e.getTipoEvento().name(), e.getAccion(), e.getResultado(), e.getFechaEvento(), e.getCorrelationId(), e.getDetalles(), e.getHashEvento()); }
    }
}
