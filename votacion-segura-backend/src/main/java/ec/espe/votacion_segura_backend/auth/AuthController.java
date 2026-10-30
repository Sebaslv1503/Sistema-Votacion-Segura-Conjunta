package ec.espe.votacion_segura_backend.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService service;
    public AuthController(AuthService service) { this.service = service; }
    @PostMapping("/login") public AuthService.TokenResponse login(@Valid @RequestBody LoginRequest body) { return service.login(body.username(), body.password()); }
    @PostMapping("/refresh") public AuthService.TokenResponse refresh(@Valid @RequestBody RefreshRequest body) { return service.refresh(body.refreshToken()); }
    @PostMapping("/logout") public void logout(@Valid @RequestBody RefreshRequest body, Authentication authentication) { service.logout(body.refreshToken(), authentication.getName()); }
    public record LoginRequest(@NotBlank String username, @NotBlank String password) { }
    public record RefreshRequest(@NotBlank String refreshToken) { }
}
