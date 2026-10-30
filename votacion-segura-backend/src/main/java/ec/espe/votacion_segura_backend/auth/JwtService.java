package ec.espe.votacion_segura_backend.auth;

import ec.espe.votacion_segura_backend.config.JwtProperties;
import ec.espe.votacion_segura_backend.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
    private final JwtProperties properties;
    private final SecretKey key;
    public JwtService(JwtProperties properties) {
        this.properties = properties;
        byte[] raw;
        try { raw = Decoders.BASE64.decode(properties.secret()); }
        catch (RuntimeException ex) { raw = properties.secret().getBytes(StandardCharsets.UTF_8); }
        if (raw.length < 32) throw new IllegalStateException("JWT_SECRET debe contener al menos 256 bits");
        this.key = Keys.hmacShaKeyFor(raw);
    }
    public String create(Usuario user) {
        Instant now = Instant.now();
        return Jwts.builder().subject(user.getUsername()).claim("role", user.getRol().name())
                .issuedAt(Date.from(now)).expiration(Date.from(now.plusMillis(properties.accessExpirationMs())))
                .signWith(key).compact();
    }
    public Claims parse(String token) { return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload(); }
    public long accessExpirationMs() { return properties.accessExpirationMs(); }
}
