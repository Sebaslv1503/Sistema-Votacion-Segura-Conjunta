package ec.espe.votacion_segura_backend.auth;

import ec.espe.votacion_segura_backend.config.JwtProperties;
import ec.espe.votacion_segura_backend.entity.RefreshToken;
import ec.espe.votacion_segura_backend.entity.Usuario;
import ec.espe.votacion_segura_backend.entity.EventoAuditoria;
import ec.espe.votacion_segura_backend.enums.TipoEventoAuditoria;
import ec.espe.votacion_segura_backend.repository.EventoAuditoriaRepository;
import ec.espe.votacion_segura_backend.repository.RefreshTokenRepository;
import ec.espe.votacion_segura_backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarios;
    private final RefreshTokenRepository refreshTokens;
    private final EventoAuditoriaRepository auditoria;
    private final JwtService jwt;
    private final long refreshMs;
    private final int maxIntentos;
    private final int bloqueoMinutos;
    private final SecureRandom random = new SecureRandom();

    public AuthService(AuthenticationManager authenticationManager, UsuarioRepository usuarios,
            RefreshTokenRepository refreshTokens, EventoAuditoriaRepository auditoria, JwtService jwt, JwtProperties jwtProperties,
            @Value("${app.security.max-intentos-login:5}") int maxIntentos,
            @Value("${app.security.bloqueo-minutos:15}") int bloqueoMinutos) {
        this.authenticationManager = authenticationManager; this.usuarios = usuarios;
        this.refreshTokens = refreshTokens; this.auditoria = auditoria; this.jwt = jwt;
        this.refreshMs = jwtProperties.refreshExpirationMs(); this.maxIntentos = maxIntentos; this.bloqueoMinutos = bloqueoMinutos;
    }

    @Transactional
    public TokenResponse login(String username, String password) {
        Usuario user = usuarios.lockByUsername(username).orElse(null);
        if (user == null) {
            registrar(TipoEventoAuditoria.LOGIN_FALLIDO, null, null, "Intento de autenticación fallido");
            throw new BadCredentialsException("Credenciales inválidas");
        }
        Instant now = Instant.now();
        if (!user.isEnabled()) throw new DisabledException("Cuenta deshabilitada");
        if (!user.isAccountNonLocked() && user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) throw new LockedException("Cuenta bloqueada temporalmente");
        if (user.getLockedUntil() != null && !user.getLockedUntil().isAfter(now)) {
            user.setAccountNonLocked(true); user.setLockedUntil(null); user.setFailedLoginAttempts(0);
        }
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        } catch (BadCredentialsException ex) {
            int failures = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(failures);
            if (failures >= maxIntentos) { user.setAccountNonLocked(false); user.setLockedUntil(now.plusSeconds(bloqueoMinutos * 60L)); }
            usuarios.save(user);
            registrar(TipoEventoAuditoria.LOGIN_FALLIDO, user, user.getId(), "Intento de autenticación fallido");
            throw new BadCredentialsException("Credenciales inválidas");
        }
        user.setFailedLoginAttempts(0); user.setAccountNonLocked(true); user.setLockedUntil(null); user.setLastLoginAt(now);
        usuarios.save(user);
        registrar(TipoEventoAuditoria.LOGIN_EXITOSO, user, user.getId(), "Autenticación exitosa");
        return issue(user);
    }

    @Transactional(noRollbackFor = BadCredentialsException.class)
    public TokenResponse refresh(String presented) {
        if (presented == null || presented.isBlank()) throw new BadCredentialsException("Refresh token inválido");
        String hash = sha256(presented);
        RefreshToken old = refreshTokens.lockByHash(hash).orElseThrow(() -> new BadCredentialsException("Refresh token inválido"));
        if (old.isRevocado()) {
            // Replay means the token has been copied: invalidate every active session for the account.
            for (RefreshToken session : refreshTokens.findByUsuarioIdAndRevocadoFalse(old.getUsuario().getId())) session.setRevocado(true);
            registrar(TipoEventoAuditoria.TOKEN_REVOCADO, old.getUsuario(), old.getUsuario().getId(), "Refresh token reutilizado; sesiones revocadas");
            throw new BadCredentialsException("Refresh token inválido");
        }
        if (!old.getFechaExpiracion().isAfter(Instant.now()) || !old.getUsuario().isEnabled() || !old.getUsuario().isAccountNonLocked()) throw new BadCredentialsException("Refresh token inválido");
        old.setRevocado(true);
        registrar(TipoEventoAuditoria.TOKEN_ROTADO, old.getUsuario(), old.getUsuario().getId(), "Refresh token rotado");
        return issue(old.getUsuario());
    }

    @Transactional
    public void logout(String presented, String username) {
        if (presented == null || presented.isBlank()) return;
        refreshTokens.lockByHash(sha256(presented)).filter(token -> token.getUsuario().getUsername().equals(username))
                .ifPresent(token -> { token.setRevocado(true); registrar(TipoEventoAuditoria.LOGOUT, token.getUsuario(), token.getUsuario().getId(), "Sesión cerrada"); });
    }

    private TokenResponse issue(Usuario user) {
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        refreshTokens.save(RefreshToken.builder().tokenHash(sha256(raw)).usuario(user)
                .fechaExpiracion(Instant.now().plusMillis(refreshMs)).build());
        return new TokenResponse(jwt.create(user), raw, "Bearer", jwt.accessExpirationMs());
    }

    private static String sha256(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException("SHA-256 no disponible", e); }
    }
    private void registrar(TipoEventoAuditoria type, Usuario user, UUID actorId, String action) {
        String result = type == TipoEventoAuditoria.LOGIN_FALLIDO ? "FAILURE" : "SUCCESS";
        auditoria.save(EventoAuditoria.builder().tipoEvento(type).accion(action).resultado(result)
                .actorId(actorId).rolActor(user == null ? null : user.getRol().name()).build());
    }
    public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresInMs) { }
}
