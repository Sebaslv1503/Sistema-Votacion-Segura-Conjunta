package ec.espe.votacion_segura_backend.participacion;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/participaciones")
public class ParticipacionController {
    private final ParticipacionService service;
    public ParticipacionController(ParticipacionService service) { this.service = service; }
    @GetMapping("/elecciones/{eleccionId}/mi-estado")
    @PreAuthorize("hasRole('SOCIO_VOTANTE')")
    public ParticipacionService.ParticipationStatus estado(@PathVariable UUID eleccionId, Authentication authentication) {
        return service.estado(authentication.getName(), eleccionId);
    }
}
