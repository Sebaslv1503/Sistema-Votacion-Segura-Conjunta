package ec.espe.votacion_segura_backend.socio;

import ec.espe.votacion_segura_backend.entity.Socio;
import ec.espe.votacion_segura_backend.enums.EstadoSocio;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Email;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/socios")
@PreAuthorize("hasAnyRole('GESTOR_PADRON','ADMINISTRADOR_TECNICO')")
public class SocioController {
    private final SocioService service;
    public SocioController(SocioService service) { this.service = service; }

    @PostMapping public SocioDto crear(@Valid @RequestBody CrearSocioRequest body) { return SocioDto.from(service.crear(body.numeroSocio(), body.nombres(), body.apellidos(), body.correoElectronico())); }
    @PutMapping("/{id}") public SocioDto actualizar(@PathVariable UUID id, @Valid @RequestBody ActualizarSocioRequest body) { return SocioDto.from(service.actualizar(id, body.nombres(), body.apellidos(), body.correoElectronico())); }
    @PostMapping(value = "/importar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportResponse importar(@RequestPart("archivo") MultipartFile archivo) throws java.io.IOException {
        return new ImportResponse(service.importarCsv(new String(archivo.getBytes(), java.nio.charset.StandardCharsets.UTF_8)));
    }
    @GetMapping public Page<SocioDto> buscar(@RequestParam(required = false) String q, Pageable pageable) { return service.buscar(q, pageable).map(SocioDto::from); }
    @PatchMapping("/{id}/habilitacion")
    public SocioDto habilitacion(@PathVariable UUID id, @Valid @RequestBody HabilitacionRequest body) { return SocioDto.from(service.habilitacion(id, body.estado(), body.activo())); }

    public record CrearSocioRequest(@NotBlank String numeroSocio, @NotBlank String nombres, @NotBlank String apellidos, String correoElectronico) { }
    public record HabilitacionRequest(@NotNull EstadoSocio estado, boolean activo) { }
    public record ActualizarSocioRequest(@NotBlank String nombres, @NotBlank String apellidos, @Email String correoElectronico) { }
    public record ImportResponse(int sociosImportados) { }
    public record SocioDto(UUID id, String numeroSocio, String nombres, String apellidos, String correoElectronico, EstadoSocio estadoHabilitacion, boolean activo) {
        static SocioDto from(Socio s) { return new SocioDto(s.getId(), s.getNumeroSocio(), s.getNombres(), s.getApellidos(), s.getCorreoElectronico(), s.getEstadoHabilitacion(), s.isActivo()); }
    }
}
