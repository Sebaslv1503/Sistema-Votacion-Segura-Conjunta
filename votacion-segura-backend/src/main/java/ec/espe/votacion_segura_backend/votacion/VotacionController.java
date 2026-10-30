package ec.espe.votacion_segura_backend.votacion;

import ec.espe.votacion_segura_backend.entity.Socio;
import ec.espe.votacion_segura_backend.repository.SocioRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/votacion")
public class VotacionController {
    private final VotacionService votacion;
    private final SocioRepository socios;

    public VotacionController(VotacionService votacion, SocioRepository socios) {
        this.votacion = votacion;
        this.socios = socios;
    }

    @PostMapping("/elecciones/{eleccionId}/credencial")
    @PreAuthorize("hasRole('SOCIO_VOTANTE')")
    public CredencialResponse emitir(@PathVariable UUID eleccionId, Authentication authentication) {
        Socio socio = socios.findByUsuarioUsername(authentication.getName()).orElseThrow(() -> new IllegalArgumentException("Cuenta no asociada a socio"));
        return new CredencialResponse(votacion.emitirCredencial(socio.getId(), eleccionId));
    }

    @PostMapping("/votos")
    @PreAuthorize("hasRole('SOCIO_VOTANTE')")
    public ConfirmacionResponse votar(@Valid @RequestBody VotoRequest request, Authentication authentication) {
        Socio socio = socios.findByUsuarioUsername(authentication.getName()).orElseThrow(() -> new IllegalArgumentException("Cuenta no asociada a socio"));
        votacion.depositarVoto(request.credencial(), socio.getId(), request.candidaturaId());
        return new ConfirmacionResponse("Voto recibido");
    }

    public record VotoRequest(@NotBlank String credencial, @NotNull UUID candidaturaId) { }
    public record CredencialResponse(String credencial) { }
    public record ConfirmacionResponse(String confirmacion) { }
}
