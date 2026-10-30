package ec.espe.votacion_segura_backend.candidatura;

import ec.espe.votacion_segura_backend.entity.Candidatura;
import ec.espe.votacion_segura_backend.entity.Eleccion;
import ec.espe.votacion_segura_backend.entity.EventoAuditoria;
import ec.espe.votacion_segura_backend.enums.EstadoEleccion;
import ec.espe.votacion_segura_backend.enums.TipoEventoAuditoria;
import ec.espe.votacion_segura_backend.repository.CandidaturaRepository;
import ec.espe.votacion_segura_backend.repository.EleccionRepository;
import ec.espe.votacion_segura_backend.repository.EventoAuditoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CandidaturaService {
    private final CandidaturaRepository candidaturas;
    private final EleccionRepository elecciones;
    private final EventoAuditoriaRepository auditoria;
    public CandidaturaService(CandidaturaRepository candidaturas, EleccionRepository elecciones, EventoAuditoriaRepository auditoria) {
        this.candidaturas = candidaturas; this.elecciones = elecciones; this.auditoria = auditoria;
    }
    public List<Candidatura> listar(UUID eleccionId) { return candidaturas.findByEleccionIdAndActivoTrue(eleccionId); }
    @Transactional
    public Candidatura crear(UUID eleccionId, String nombre, String descripcion, String propuesta) {
        Eleccion eleccion = elecciones.findById(eleccionId).orElseThrow(() -> new IllegalArgumentException("Elección no encontrada"));
        if (eleccion.getEstado() != EstadoEleccion.BORRADOR && eleccion.getEstado() != EstadoEleccion.PROGRAMADA) throw new IllegalStateException("La elección ya no admite candidaturas");
        if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException("Nombre obligatorio");
        Candidatura item = candidaturas.save(Candidatura.builder().eleccion(eleccion).nombre(nombre.trim()).descripcion(descripcion).propuesta(propuesta).build());
        auditoria.save(EventoAuditoria.builder().tipoEvento(TipoEventoAuditoria.CANDIDATURA_REGISTRADA).accion("Candidatura registrada").resultado("SUCCESS")
                .detalles("{\"eleccionId\":\"" + eleccionId + "\",\"candidaturaId\":\"" + item.getId() + "\"}").build());
        return item;
    }
    @Transactional
    public Candidatura actualizar(UUID id, String nombre, String descripcion, String propuesta, boolean activo) {
        Candidatura item = candidaturas.findById(id).orElseThrow(() -> new IllegalArgumentException("Candidatura no encontrada"));
        if (item.getEleccion().getEstado() != EstadoEleccion.BORRADOR && item.getEleccion().getEstado() != EstadoEleccion.PROGRAMADA) throw new IllegalStateException("La elección ya no admite cambios en candidaturas");
        if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException("Nombre obligatorio");
        item.setNombre(nombre.trim()); item.setDescripcion(descripcion); item.setPropuesta(propuesta); item.setActivo(activo);
        auditoria.save(EventoAuditoria.builder().tipoEvento(TipoEventoAuditoria.CANDIDATURA_MODIFICADA).accion("Candidatura modificada").resultado("SUCCESS")
                .detalles("{\"candidaturaId\":\"" + id + "\"}").build());
        return item;
    }
}
