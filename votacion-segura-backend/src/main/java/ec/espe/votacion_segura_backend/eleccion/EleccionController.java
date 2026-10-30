package ec.espe.votacion_segura_backend.eleccion;

import ec.espe.votacion_segura_backend.entity.Eleccion;
import ec.espe.votacion_segura_backend.enums.EstadoEleccion;
import ec.espe.votacion_segura_backend.repository.UsuarioRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/elecciones")
public class EleccionController {
    private final EleccionService service;
    private final UsuarioRepository usuarios;
    public EleccionController(EleccionService service, UsuarioRepository usuarios) { this.service = service; this.usuarios = usuarios; }

    @GetMapping
    @PreAuthorize("hasAnyRole('SOCIO_VOTANTE','GESTOR_PADRON','ADMINISTRADOR_TECNICO','CUSTODIO_ELECTORAL','AUDITOR_INDEPENDIENTE','SOPORTE')")
    public List<EleccionResponse> disponibles() { return service.listarDisponibles().stream().map(EleccionResponse::from).toList(); }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR_TECNICO','CUSTODIO_ELECTORAL')")
    public EleccionResponse crear(@Valid @RequestBody CrearEleccionRequest request) {
        return EleccionResponse.from(service.crear(request.nombre(), request.descripcion(), request.inicio(), request.fin()));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('CUSTODIO_ELECTORAL')")
    public EleccionResponse estado(@PathVariable UUID id, @Valid @RequestBody EstadoRequest request) { return EleccionResponse.from(service.transicionar(id, request.estado())); }

    @PostMapping("/{id}/solicitar-cierre")
    @PreAuthorize("hasRole('CUSTODIO_ELECTORAL')")
    public EleccionResponse solicitarCierre(@PathVariable UUID id, Authentication auth) { return EleccionResponse.from(service.solicitarCierre(id, actorId(auth))); }

    @PostMapping("/{id}/aprobar-cierre")
    @PreAuthorize("hasRole('CUSTODIO_ELECTORAL')")
    public EleccionResponse aprobarCierre(@PathVariable UUID id, Authentication auth) { return EleccionResponse.from(service.aprobarCierre(id, actorId(auth))); }

    private UUID actorId(Authentication auth) { return usuarios.findByUsername(auth.getName()).orElseThrow().getId(); }

    public record CrearEleccionRequest(@NotBlank String nombre, String descripcion, @NotNull Instant inicio, @NotNull Instant fin) { }
    public record EstadoRequest(@NotNull EstadoEleccion estado) { }
    public record EleccionResponse(UUID id, String nombre, String descripcion, Instant inicio, Instant fin, EstadoEleccion estado) {
        static EleccionResponse from(Eleccion e) { return new EleccionResponse(e.getId(), e.getNombre(), e.getDescripcion(), e.getFechaInicio(), e.getFechaFin(), e.getEstado()); }
    }
}
