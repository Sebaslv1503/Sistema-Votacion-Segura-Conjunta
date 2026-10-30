package ec.espe.votacion_segura_backend.escrutinio;

import ec.espe.votacion_segura_backend.entity.ActaResultado;
import ec.espe.votacion_segura_backend.repository.UsuarioRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/resultados")
public class EscrutinioController {
    private final EscrutinioService service;
    private final UsuarioRepository users;
    public EscrutinioController(EscrutinioService service, UsuarioRepository users) { this.service = service; this.users = users; }
    @PostMapping("/elecciones/{id}/escrutinio")
    @PreAuthorize("hasAnyRole('CUSTODIO_ELECTORAL','ADMINISTRADOR_TECNICO')")
    public ActaDto generar(@PathVariable UUID id) { return ActaDto.from(service.generar(id)); }
    @PostMapping("/elecciones/{id}/aprobacion")
    @PreAuthorize("hasRole('CUSTODIO_ELECTORAL')")
    public ActaDto aprobar(@PathVariable UUID id, Authentication auth) { return ActaDto.from(service.aprobar(id, users.findByUsername(auth.getName()).orElseThrow().getId())); }
    @PostMapping("/elecciones/{id}/publicar")
    @PreAuthorize("hasRole('CUSTODIO_ELECTORAL')")
    public ActaDto publicar(@PathVariable UUID id) { return ActaDto.from(service.publicar(id)); }
    @GetMapping("/elecciones/{id}")
    @PreAuthorize("hasAnyRole('SOCIO_VOTANTE','CUSTODIO_ELECTORAL','AUDITOR_INDEPENDIENTE','ADMINISTRADOR_TECNICO')")
    public ActaDto consultar(@PathVariable UUID id) { return ActaDto.from(service.consultar(id)); }
    public record ActaDto(UUID eleccionId, long totalVotos, Instant fechaCierre, String conteosJson, String hash, String estado) {
        static ActaDto from(ActaResultado a) { return new ActaDto(a.getEleccion().getId(), a.getTotalVotos(), a.getFechaCierre(), a.getConteosJson().toString(), a.getHashManifiesto(), a.getEstado()); }
    }
}
