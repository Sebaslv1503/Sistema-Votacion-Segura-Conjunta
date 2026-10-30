package ec.espe.votacion_segura_backend.votacion;

import ec.espe.votacion_segura_backend.entity.*;
import ec.espe.votacion_segura_backend.enums.*;
import ec.espe.votacion_segura_backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class VotacionServiceTest {
    @Mock ParticipacionRepository participaciones;
    @Mock SocioRepository socios;
    @Mock EleccionRepository elecciones;
    @Mock CredencialVotoRepository credenciales;
    @Mock CandidaturaRepository candidaturas;
    @Mock VotoRepository votos;
    @Mock EventoAuditoriaRepository auditoria;
    VotacionService service;

    @BeforeEach void setup() {
        service = new VotacionService(participaciones, socios, elecciones, credenciales, candidaturas, votos, auditoria, 30);
    }

    @Test void rejectsExpiredCredential() {
        UUID socioId = UUID.randomUUID();
        UUID electionId = UUID.randomUUID();
        CredencialVoto credential = credential(socioId, electionId, Instant.now().minusSeconds(1));
        when(credenciales.lockByTokenHash(anyString())).thenReturn(Optional.of(credential));
        assertThrows(IllegalStateException.class, () -> service.depositarVoto("token", socioId, UUID.randomUUID()));
    }

    @Test void rejectsCredentialOwnedByAnotherSocio() {
        UUID socioId = UUID.randomUUID();
        CredencialVoto credential = credential(UUID.randomUUID(), UUID.randomUUID(), Instant.now().plusSeconds(300));
        when(credenciales.lockByTokenHash(anyString())).thenReturn(Optional.of(credential));
        assertThrows(IllegalArgumentException.class, () -> service.depositarVoto("token", socioId, UUID.randomUUID()));
    }

    @Test void rejectsVoteWhenElectionIsClosed() {
        UUID socioId = UUID.randomUUID();
        UUID electionId = UUID.randomUUID();
        CredencialVoto credential = credential(socioId, electionId, Instant.now().plusSeconds(300));
        Eleccion election = Eleccion.builder().id(electionId).estado(EstadoEleccion.CERRADA)
                .fechaInicio(Instant.now().minusSeconds(60)).fechaFin(Instant.now().plusSeconds(60)).build();
        when(credenciales.lockByTokenHash(anyString())).thenReturn(Optional.of(credential));
        when(elecciones.lockById(electionId)).thenReturn(Optional.of(election));
        assertThrows(IllegalStateException.class, () -> service.depositarVoto("token", socioId, UUID.randomUUID()));
    }

    @Test void rejectsCredentialReuse() {
        UUID socioId = UUID.randomUUID();
        CredencialVoto credential = credential(socioId, UUID.randomUUID(), Instant.now().plusSeconds(300));
        credential.setEstado(EstadoCredencial.USADA);
        when(credenciales.lockByTokenHash(anyString())).thenReturn(Optional.of(credential));
        assertThrows(IllegalStateException.class, () -> service.depositarVoto("token", socioId, UUID.randomUUID()));
        verify(votos, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test void depositsAnUnlinkedVoteAndMarksParticipationConsumed() {
        UUID socioId = UUID.randomUUID(); UUID electionId = UUID.randomUUID(); UUID candidacyId = UUID.randomUUID();
        Instant now = Instant.now();
        CredencialVoto credential = credential(socioId, electionId, now.plusSeconds(300));
        Eleccion election = Eleccion.builder().id(electionId).estado(EstadoEleccion.ABIERTA)
                .fechaInicio(now.minusSeconds(60)).fechaFin(now.plusSeconds(300)).build();
        when(credenciales.lockByTokenHash(anyString())).thenReturn(Optional.of(credential));
        when(elecciones.lockById(electionId)).thenReturn(Optional.of(election));
        Candidatura candidacy = Candidatura.builder().id(candidacyId).eleccion(election).activo(true).build();
        when(candidaturas.findByIdAndEleccionIdAndActivoTrue(candidacyId, electionId)).thenReturn(Optional.of(candidacy));
        Participacion participation = credential.getParticipacionRef();
        participation.setEstadoParticipacion(EstadoParticipacion.PENDIENTE);
        when(participaciones.lockForElectionAndMember(electionId, socioId)).thenReturn(Optional.of(participation));
        service.depositarVoto("token", socioId, candidacyId);
        verify(votos).save(org.mockito.ArgumentMatchers.argThat(v -> v.getCandidatura().getId().equals(candidacyId)));
        org.junit.jupiter.api.Assertions.assertEquals(EstadoCredencial.USADA, credential.getEstado());
        org.junit.jupiter.api.Assertions.assertEquals(EstadoParticipacion.VOTADO, participation.getEstadoParticipacion());
    }

    private static CredencialVoto credential(UUID socioId, UUID electionId, Instant expiration) {
        Socio socio = Socio.builder().id(socioId).build();
        Eleccion election = Eleccion.builder().id(electionId).build();
        Participacion participation = Participacion.builder().id(UUID.randomUUID()).socio(socio).eleccion(election).build();
        return CredencialVoto.builder().id(UUID.randomUUID()).estado(EstadoCredencial.PENDIENTE)
                .fechaExpiracion(expiration).eleccion(election).participacionRef(participation).build();
    }
}
