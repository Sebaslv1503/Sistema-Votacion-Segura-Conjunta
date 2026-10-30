package ec.espe.votacion_segura_backend.escrutinio;

import ec.espe.votacion_segura_backend.entity.*;
import ec.espe.votacion_segura_backend.enums.EstadoEleccion;
import ec.espe.votacion_segura_backend.enums.TipoEventoAuditoria;
import ec.espe.votacion_segura_backend.repository.ActaResultadoRepository;
import ec.espe.votacion_segura_backend.repository.CandidaturaRepository;
import ec.espe.votacion_segura_backend.repository.EleccionRepository;
import ec.espe.votacion_segura_backend.repository.EventoAuditoriaRepository;
import ec.espe.votacion_segura_backend.repository.VotoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/** Conteo agregado: este module only queries urna rows and never resolves participants. */
@Service
public class EscrutinioService {
    private final EleccionRepository elecciones;
    private final CandidaturaRepository candidaturas;
    private final VotoRepository votos;
    private final ActaResultadoRepository actas;
    private final EventoAuditoriaRepository auditoria;
    public EscrutinioService(EleccionRepository elecciones, CandidaturaRepository candidaturas, VotoRepository votos,
            ActaResultadoRepository actas, EventoAuditoriaRepository auditoria) {
        this.elecciones = elecciones; this.candidaturas = candidaturas; this.votos = votos;
        this.actas = actas; this.auditoria = auditoria;
    }

    @Transactional
    public ActaResultado generar(UUID electionId) {
        Eleccion election = elecciones.lockById(electionId).orElseThrow(() -> new IllegalArgumentException("Elección no encontrada"));
        if (election.getEstado() != EstadoEleccion.CERRADA) throw new IllegalStateException("El escrutinio requiere elección cerrada");
        if (actas.findByEleccionId(electionId).isPresent()) throw new IllegalStateException("El acta ya fue generada");
        Map<UUID, Long> counts = votos.contarPorCandidatura(electionId).stream().collect(Collectors.toMap(r -> (UUID) r[0], r -> (Long) r[1]));
        long total = votos.countByEleccionId(electionId);
        long counted = counts.values().stream().mapToLong(Long::longValue).sum();
        if (total != counted) throw new IllegalStateException("El conteo no coincide con el total de votos");
        String json = candidaturas.findByEleccionId(electionId).stream().map(c -> "{\"candidaturaId\":\"" + c.getId() + "\",\"nombre\":\"" + escape(c.getNombre()) + "\",\"votos\":" + counts.getOrDefault(c.getId(), 0L) + "}").collect(Collectors.joining(",", "[", "]"));
        Instant closedAt = election.getClosedAt() == null ? Instant.now() : election.getClosedAt();
        String hash = sha256(electionId + ":" + total + ":" + json + ":" + closedAt);
        JsonNode conteos = parseJson(json);
        ActaResultado acta = actas.save(ActaResultado.builder().eleccion(election).totalVotos(total)
                .fechaCierre(closedAt)
                .conteosJson(conteos).hashManifiesto(hash).estado("PENDIENTE_APROBACION").build());
        audit(TipoEventoAuditoria.ACTA_GENERADA, "Acta de resultados generada", electionId);
        return acta;
    }

    @Transactional
    public ActaResultado aprobar(UUID electionId, UUID approverId) {
        ActaResultado acta = actas.lockByEleccionId(electionId).orElseThrow(() -> new IllegalArgumentException("Acta no generada"));
        if (!"PENDIENTE_APROBACION".equals(acta.getEstado())) throw new IllegalStateException("El acta no admite aprobaciones");
        if (acta.getAprobador1Id() == null) acta.setAprobador1Id(approverId);
        else {
            if (approverId.equals(acta.getAprobador1Id())) throw new IllegalStateException("Se requieren dos custodios distintos");
            acta.setAprobador2Id(approverId); acta.setFechaAprobacion(Instant.now()); acta.setEstado("APROBADA");
        }
        audit(TipoEventoAuditoria.ACTA_APROBADA, "Aprobación de acta registrada", electionId);
        return acta;
    }

    @Transactional
    public ActaResultado publicar(UUID electionId) {
        ActaResultado acta = actas.lockByEleccionId(electionId).orElseThrow(() -> new IllegalArgumentException("Acta no generada"));
        if (!"APROBADA".equals(acta.getEstado()) || acta.getAprobador1Id() == null || acta.getAprobador2Id() == null) throw new IllegalStateException("Se requieren dos aprobaciones antes de publicar");
        Eleccion election = elecciones.lockById(electionId).orElseThrow();
        acta.setEstado("PUBLICADA"); election.setEstado(EstadoEleccion.FINALIZADA);
        audit(TipoEventoAuditoria.RESULTADOS_PUBLICADOS, "Resultados publicados", electionId);
        return acta;
    }

    public ActaResultado consultar(UUID electionId) {
        ActaResultado acta = actas.findByEleccionId(electionId).orElseThrow(() -> new IllegalArgumentException("Resultados no disponibles"));
        if (!"PUBLICADA".equals(acta.getEstado())) throw new IllegalStateException("Resultados aún no publicados");
        return acta;
    }

    private void audit(TipoEventoAuditoria type, String action, UUID electionId) {
        auditoria.save(EventoAuditoria.builder().tipoEvento(type).accion(action).resultado("SUCCESS").detalles("{\"eleccionId\":\"" + electionId + "\"}").build());
    }
    private static String escape(String s) { return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r"); }
    private static JsonNode parseJson(String json) {
        try { return JsonMapper.builder().build().readTree(json); }
        catch (Exception e) { throw new IllegalStateException("No se pudo serializar el conteo", e); }
    }
    private static String sha256(String s) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException("SHA-256 no disponible", e); }
    }
}
