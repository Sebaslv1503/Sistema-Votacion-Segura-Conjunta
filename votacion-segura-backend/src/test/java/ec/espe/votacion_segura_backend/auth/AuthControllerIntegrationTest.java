package ec.espe.votacion_segura_backend.auth;

import ec.espe.votacion_segura_backend.entity.Usuario;
import ec.espe.votacion_segura_backend.enums.Rol;
import ec.espe.votacion_segura_backend.repository.UsuarioRepository;
import ec.espe.votacion_segura_backend.repository.RefreshTokenRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository users;
    @Autowired RefreshTokenRepository refreshTokens;
    @Autowired PasswordEncoder passwords;
    UUID userId;

    @AfterEach void cleanup() {
        if (userId != null) {
            refreshTokens.deleteAll(refreshTokens.findByUsuarioIdAndRevocadoFalse(userId));
            users.deleteById(userId);
        }
    }

    @Test void loginAndRoleProtectedRequestWorkWithIssuedJwt() throws Exception {
        Usuario user = users.saveAndFlush(Usuario.builder().username("integration-manager").passwordHash(passwords.encode("strong-password"))
                .rol(Rol.GESTOR_PADRON).enabled(true).accountNonLocked(true).build());
        userId = user.getId();
        String body = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"integration-manager\",\"password\":\"strong-password\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String access = Pattern.compile("\\\"accessToken\\\":\\\"([^\\\"]+)\\\"").matcher(body).results().findFirst().orElseThrow().group(1);
        mvc.perform(get("/api/v1/socios").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/auditoria").header("Authorization", "Bearer " + access))
                .andExpect(status().isForbidden());
    }

    @Test void incorrectLoginReturnsUnauthorized() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"missing-user\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }
}
