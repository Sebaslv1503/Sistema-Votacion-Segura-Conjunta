package ec.espe.votacion_segura_backend.candidatura;

import ec.espe.votacion_segura_backend.entity.Candidatura;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/candidaturas")
public class CandidaturaController {
    private final CandidaturaService service;
    public CandidaturaController(CandidaturaService service) { this.service = service; }
    @GetMapping("/elecciones/{eleccionId}")
    @PreAuthorize("hasAnyRole('SOCIO_VOTANTE','GESTOR_PADRON','ADMINISTRADOR_TECNICO','CUSTODIO_ELECTORAL','AUDITOR_INDEPENDIENTE')")
    public List<CandidaturaDto> listar(@PathVariable UUID eleccionId) { return service.listar(eleccionId).stream().map(CandidaturaDto::from).toList(); }
    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTODIO_ELECTORAL','ADMINISTRADOR_TECNICO')")
    public CandidaturaDto crear(@Valid @RequestBody CrearRequest body) { return CandidaturaDto.from(service.crear(body.eleccionId(), body.nombre(), body.descripcion(), body.propuesta())); }
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTODIO_ELECTORAL','ADMINISTRADOR_TECNICO')")
    public CandidaturaDto actualizar(@PathVariable UUID id, @Valid @RequestBody ActualizarRequest body) { return CandidaturaDto.from(service.actualizar(id, body.nombre(), body.descripcion(), body.propuesta(), body.activo())); }
    public record CrearRequest(@NotNull UUID eleccionId, @NotBlank String nombre, String descripcion, String propuesta) { }
    public record ActualizarRequest(@NotBlank String nombre, String descripcion, String propuesta, boolean activo) { }
    public record CandidaturaDto(UUID id, UUID eleccionId, String nombre, String descripcion, String propuesta, boolean activo) {
        static CandidaturaDto from(Candidatura c) { return new CandidaturaDto(c.getId(), c.getEleccion().getId(), c.getNombre(), c.getDescripcion(), c.getPropuesta(), c.isActivo()); }
    }
}
