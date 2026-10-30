package ec.espe.votacion_segura_backend.auth;

import ec.espe.votacion_segura_backend.entity.Usuario;
import ec.espe.votacion_segura_backend.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UsuarioDetailsService implements UserDetailsService {
    private final UsuarioRepository usuarios;
    public UsuarioDetailsService(UsuarioRepository usuarios) { this.usuarios = usuarios; }
    @Override public UserDetails loadUserByUsername(String username) {
        Usuario u = usuarios.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas"));
        return User.withUsername(u.getUsername()).password(u.getPasswordHash()).roles(u.getRol().name())
                .disabled(!u.isEnabled()).accountLocked(!u.isAccountNonLocked()).build();
    }
}
