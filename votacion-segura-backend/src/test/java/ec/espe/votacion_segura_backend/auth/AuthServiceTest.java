package ec.espe.votacion_segura_backend.auth;

import ec.espe.votacion_segura_backend.config.JwtProperties;
import ec.espe.votacion_segura_backend.entity.Usuario;
import ec.espe.votacion_segura_backend.enums.Rol;
import ec.espe.votacion_segura_backend.repository.RefreshTokenRepository;
import ec.espe.votacion_segura_backend.repository.UsuarioRepository;
import ec.espe.votacion_segura_backend.repository.EventoAuditoriaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock AuthenticationManager manager;
    @Mock UsuarioRepository users;
    @Mock RefreshTokenRepository refresh;
    @Mock EventoAuditoriaRepository audit;
    @Mock JwtService jwt;
    AuthService service;
    Usuario user;

    @BeforeEach void setup() {
        service = new AuthService(manager, users, refresh, audit, jwt, new JwtProperties("a-valid-test-signing-secret-with-32-bytes-minimum", 900000, 28800000), 5, 15);
        user = Usuario.builder().id(UUID.randomUUID()).username("voter").passwordHash("$2a$12$test").rol(Rol.SOCIO_VOTANTE).build();
        when(users.lockByUsername("voter")).thenReturn(Optional.of(user));
    }

    @Test void successfulLoginIssuesAccessAndOpaqueRefreshToken() {
        when(jwt.create(user)).thenReturn("signed-access-token");
        when(manager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(new UsernamePasswordAuthenticationToken("voter", null));
        var response = service.login("voter", "correct");
        assertEquals("signed-access-token", response.accessToken());
        assertNotNull(response.refreshToken());
        verify(refresh).save(any());
        verify(users).save(user);
    }

    @Test void badPasswordIncrementsFailureCounter() {
        when(manager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenThrow(new BadCredentialsException("bad"));
        assertThrows(BadCredentialsException.class, () -> service.login("voter", "wrong"));
        assertEquals(1, user.getFailedLoginAttempts());
        verify(users).save(user);
        verifyNoInteractions(refresh);
    }
}
