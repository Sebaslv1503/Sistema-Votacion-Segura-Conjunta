package ec.espe.votacion_segura_backend.auth;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Supports legacy BCrypt seed hashes and Argon2 hashes for newly created credentials. */
public final class CompatiblePasswordEncoder implements PasswordEncoder {
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder(12);
    @Override public String encode(CharSequence rawPassword) { return bcrypt.encode(rawPassword); }
    @Override public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (encodedPassword == null) return false;
        if (encodedPassword.startsWith("$2a$") || encodedPassword.startsWith("$2b$") || encodedPassword.startsWith("$2y$")) return bcrypt.matches(rawPassword, encodedPassword);
        return false;
    }
}
