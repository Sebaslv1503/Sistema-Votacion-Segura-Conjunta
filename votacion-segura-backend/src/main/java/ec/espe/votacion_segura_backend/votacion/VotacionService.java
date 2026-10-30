package ec.espe.votacion_segura_backend.votacion;

import ec.espe.votacion_segura_backend.entity.*;
import ec.espe.votacion_segura_backend.enums.*;
import ec.espe.votacion_segura_backend.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/** Operaciones de urna. El voto no conserva referencias a participante ni credencial. */
@Service
public class VotacionService {
    private final ParticipacionRepository participaciones;
    private final SocioRepository socios;
    private final EleccionRepository elecciones;
    private final CredencialVotoRepository credenciales;
    private final CandidaturaRepository candidaturas;
    private final VotoRepository votos;
    private final EventoAuditoriaRepository auditoria;
    private final SecureRandom random = new SecureRandom();
    private final int expiracionMinutos;

    public VotacionService(ParticipacionRepository participaciones, SocioRepository socios,
            EleccionRepository elecciones, CredencialVotoRepository credenciales,
            CandidaturaRepository candidaturas, VotoRepository votos,
            EventoAuditoriaRepository auditoria,
            @Value("${app.credencial.expiracion-minutos:30}") int expiracionMinutos) {
        this.participaciones = participaciones;
        this.socios = socios;
        this.elecciones = elecciones;
        this.credenciales = credenciales;
        this.candidaturas = candidaturas;
        this.votos = votos;
        this.auditoria = auditoria;
        this.expiracionMinutos = expiracionMinutos;
    }

    @Transactional
    public String emitirCredencial(UUID socioId, UUID eleccionId) {
        Socio socio = socios.lockById(socioId).orElseThrow(() -> new IllegalArgumentException("Socio no encontrado"));
        Eleccion eleccion = elecciones.lockById(eleccionId).orElseThrow(() -> new IllegalArgumentException("Elección no encontrada"));
        validarElegibilidad(socio, eleccion);
        participaciones.findByEleccionIdAndSocioId(eleccionId, socioId).orElseGet(() -> {
            Participacion nueva = Participacion.builder().eleccion(eleccion).socio(socio).build();
            return participaciones.saveAndFlush(nueva);
        });
        Participacion bloqueada = participaciones.lockForElectionAndMember(eleccionId, socioId).orElseThrow();
        if (bloqueada.getEstadoParticipacion() == EstadoParticipacion.VOTADO) throw new IllegalStateException("El socio ya participó");
        for (CredencialVoto previa : credenciales.lockByParticipationAndState(bloqueada.getId(), EstadoCredencial.PENDIENTE)) {
            if (previa.getFechaExpiracion().isAfter(Instant.now())) throw new IllegalStateException("Ya existe una credencial pendiente");
            previa.setEstado(EstadoCredencial.VENCIDA);
        }

        byte[] secret = new byte[32]; random.nextBytes(secret);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        credenciales.save(CredencialVoto.builder().tokenHash(sha256(token)).eleccion(eleccion)
                .participacionRef(bloqueada).fechaExpiracion(Instant.now().plus(expiracionMinutos, ChronoUnit.MINUTES)).build());
        bloqueada.setEstadoParticipacion(EstadoParticipacion.PENDIENTE);
        bloqueada.setFechaOperativa(Instant.now().truncatedTo(ChronoUnit.MINUTES));
        registrar(TipoEventoAuditoria.CREDENCIAL_EMITIDA, "Credencial de voto emitida", socioId, eleccionId);
        return token;
    }

    @Transactional
    public void depositarVoto(String token, UUID socioId, UUID candidaturaId) {
        if (token == null || token.isBlank()) throw new IllegalArgumentException("Credencial inválida");
        CredencialVoto credencial = credenciales.lockByTokenHash(sha256(token)).orElseThrow(() -> new IllegalArgumentException("Credencial inválida"));
        if (!credencial.getParticipacionRef().getSocio().getId().equals(socioId)) throw new IllegalArgumentException("Credencial inválida");
        Instant ahora = Instant.now();
        if (credencial.getEstado() != EstadoCredencial.PENDIENTE || !credencial.getFechaExpiracion().isAfter(ahora)) {
            throw new IllegalStateException("Credencial vencida o ya utilizada");
        }
        Eleccion eleccion = elecciones.lockById(credencial.getEleccion().getId()).orElseThrow();
        if (eleccion.getEstado() != EstadoEleccion.ABIERTA || ahora.isBefore(eleccion.getFechaInicio()) || !ahora.isBefore(eleccion.getFechaFin())) {
            throw new IllegalStateException("La elección no está abierta para votar");
        }
        Candidatura candidatura = candidaturas.findByIdAndEleccionIdAndActivoTrue(candidaturaId, eleccion.getId())
                .orElseThrow(() -> new IllegalArgumentException("Candidatura no válida para esta elección"));
        Participacion participacion = participaciones.lockForElectionAndMember(eleccion.getId(), socioId).orElseThrow();
        if (participacion.getEstadoParticipacion() != EstadoParticipacion.PENDIENTE) throw new IllegalStateException("Participación ya consumida");

        // No guardar vínculo de voto con socio, participación o credencial.
        Instant minuto = ahora.truncatedTo(ChronoUnit.MINUTES);
        UUID voteId = UUID.randomUUID();
        String integridad = sha256(eleccion.getId() + ":" + candidatura.getId() + ":" + voteId + ":" + minuto);
        votos.save(Voto.builder().id(voteId).eleccion(eleccion).candidatura(candidatura).fechaRegistro(minuto).hashIntegridad(integridad).build());
        credencial.setEstado(EstadoCredencial.USADA);
        credencial.setUsedAt(ahora);
        participacion.setEstadoParticipacion(EstadoParticipacion.VOTADO);
        participacion.setFechaOperativa(minuto);
        registrar(TipoEventoAuditoria.VOTO_REGISTRADO, "Voto depositado sin datos de selección", null, eleccion.getId());
    }

    private void validarElegibilidad(Socio socio, Eleccion eleccion) {
        Instant ahora = Instant.now();
        if (!socio.isActivo() || socio.getEstadoHabilitacion() != EstadoSocio.HABILITADO) throw new IllegalStateException("Socio no habilitado");
        if (eleccion.getEstado() != EstadoEleccion.ABIERTA || ahora.isBefore(eleccion.getFechaInicio()) || !ahora.isBefore(eleccion.getFechaFin())) throw new IllegalStateException("La elección no está abierta");
    }

    private void registrar(TipoEventoAuditoria tipo, String accion, UUID actor, UUID eleccion) {
        auditoria.save(EventoAuditoria.builder().tipoEvento(tipo).accion(accion).resultado("SUCCESS")
                .actorId(actor).detalles("{\"eleccionId\":\"" + eleccion + "\"}").build());
    }

    private static String sha256(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException("SHA-256 no disponible", e); }
    }
}
