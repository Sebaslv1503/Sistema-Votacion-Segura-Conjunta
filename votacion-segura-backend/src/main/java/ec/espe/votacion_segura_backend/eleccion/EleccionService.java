package ec.espe.votacion_segura_backend.eleccion;

import ec.espe.votacion_segura_backend.entity.Eleccion;
import ec.espe.votacion_segura_backend.enums.EstadoEleccion;
import ec.espe.votacion_segura_backend.repository.EleccionRepository;
import ec.espe.votacion_segura_backend.repository.CandidaturaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.time.Instant;

@Service
public class EleccionService {
    private final EleccionRepository elecciones;
    private final CandidaturaRepository candidaturas;
    public EleccionService(EleccionRepository elecciones, CandidaturaRepository candidaturas) { this.elecciones = elecciones; this.candidaturas = candidaturas; }

    @Transactional
    public Eleccion crear(String nombre, String descripcion, Instant inicio, Instant fin) {
        if (nombre == null || nombre.isBlank() || inicio == null || fin == null || !fin.isAfter(inicio)) throw new IllegalArgumentException("Datos de elección inválidos");
        return elecciones.save(Eleccion.builder().nombre(nombre.trim()).descripcion(descripcion).fechaInicio(inicio).fechaFin(fin).estado(EstadoEleccion.BORRADOR).build());
    }

    public List<Eleccion> listarDisponibles() { return elecciones.findByEstado(EstadoEleccion.ABIERTA); }

    @Transactional
    public Eleccion transicionar(UUID id, EstadoEleccion destino) {
        Eleccion e = elecciones.lockById(id).orElseThrow(() -> new IllegalArgumentException("Elección no encontrada"));
        EstadoEleccion origen = e.getEstado();
        boolean valido = switch (origen) {
            case BORRADOR -> destino == EstadoEleccion.PROGRAMADA;
            case PROGRAMADA -> destino == EstadoEleccion.ABIERTA;
            case ABIERTA -> destino == EstadoEleccion.PAUSADA;
            case PAUSADA -> destino == EstadoEleccion.ABIERTA;
            case CIERRE_SOLICITADO -> false;
            case CERRADA -> false;
            case FINALIZADA -> false;
        };
        if (!valido) throw new IllegalStateException("Transición de estado no permitida");
        if (destino == EstadoEleccion.ABIERTA && candidaturas.findByEleccionIdAndActivoTrue(id).isEmpty()) throw new IllegalStateException("Se requiere al menos una candidatura activa");
        if (destino == EstadoEleccion.ABIERTA && (Instant.now().isBefore(e.getFechaInicio()) || !Instant.now().isBefore(e.getFechaFin()))) throw new IllegalStateException("Fuera del periodo configurado");
        e.setEstado(destino);
        if (destino == EstadoEleccion.ABIERTA && e.getOpenedAt() == null) e.setOpenedAt(Instant.now());
        if (destino == EstadoEleccion.CERRADA) e.setClosedAt(Instant.now());
        return e;
    }

    @Transactional
    public Eleccion solicitarCierre(UUID id, UUID custodioId) {
        Eleccion e = elecciones.lockById(id).orElseThrow(() -> new IllegalArgumentException("Elección no encontrada"));
        if (e.getEstado() != EstadoEleccion.ABIERTA && e.getEstado() != EstadoEleccion.PAUSADA) throw new IllegalStateException("Solo se puede solicitar el cierre de una elección activa");
        e.setEstado(EstadoEleccion.CIERRE_SOLICITADO); e.setSolicitanteCierreId(custodioId); e.setCierreSolicitadoAt(Instant.now());
        return e;
    }

    @Transactional
    public Eleccion aprobarCierre(UUID id, UUID custodioId) {
        Eleccion e = elecciones.lockById(id).orElseThrow(() -> new IllegalArgumentException("Elección no encontrada"));
        if (e.getEstado() != EstadoEleccion.CIERRE_SOLICITADO) throw new IllegalStateException("No hay cierre pendiente");
        if (custodioId.equals(e.getSolicitanteCierreId())) throw new IllegalStateException("El cierre requiere dos custodios distintos");
        e.setAprobadorCierreId(custodioId); e.setClosedAt(Instant.now()); e.setEstado(EstadoEleccion.CERRADA);
        return e;
    }
}
