package ec.espe.votacion_segura_backend.participacion;

import ec.espe.votacion_segura_backend.entity.Eleccion;
import ec.espe.votacion_segura_backend.entity.Participacion;
import ec.espe.votacion_segura_backend.entity.Socio;
import ec.espe.votacion_segura_backend.enums.EstadoEleccion;
import ec.espe.votacion_segura_backend.enums.EstadoSocio;
import ec.espe.votacion_segura_backend.repository.EleccionRepository;
import ec.espe.votacion_segura_backend.repository.ParticipacionRepository;
import ec.espe.votacion_segura_backend.repository.SocioRepository;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.UUID;

@Service
public class ParticipacionService {
    private final ParticipacionRepository participaciones;
    private final SocioRepository socios;
    private final EleccionRepository elecciones;
    public ParticipacionService(ParticipacionRepository participaciones, SocioRepository socios, EleccionRepository elecciones) {
        this.participaciones = participaciones; this.socios = socios; this.elecciones = elecciones;
    }
    public ParticipationStatus estado(String username, UUID electionId) {
        Socio socio = socios.findByUsuarioUsername(username).orElseThrow(() -> new IllegalArgumentException("Cuenta no asociada a socio"));
        Eleccion election = elecciones.findById(electionId).orElseThrow(() -> new IllegalArgumentException("Elección no encontrada"));
        boolean eligible = socio.isActivo() && socio.getEstadoHabilitacion() == EstadoSocio.HABILITADO
                && election.getEstado() == EstadoEleccion.ABIERTA && !Instant.now().isBefore(election.getFechaInicio()) && Instant.now().isBefore(election.getFechaFin());
        return participaciones.findByEleccionIdAndSocioId(electionId, socio.getId())
                .map(p -> new ParticipationStatus(electionId, eligible, p.getEstadoParticipacion().name()))
                .orElseGet(() -> new ParticipationStatus(electionId, eligible, "SIN_CREDENCIAL"));
    }
    public record ParticipationStatus(UUID electionId, boolean eligible, String participationState) { }
}
