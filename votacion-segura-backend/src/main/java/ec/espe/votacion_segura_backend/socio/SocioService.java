package ec.espe.votacion_segura_backend.socio;

import ec.espe.votacion_segura_backend.entity.EventoAuditoria;
import ec.espe.votacion_segura_backend.entity.Socio;
import ec.espe.votacion_segura_backend.enums.EstadoSocio;
import ec.espe.votacion_segura_backend.enums.TipoEventoAuditoria;
import ec.espe.votacion_segura_backend.repository.EventoAuditoriaRepository;
import ec.espe.votacion_segura_backend.repository.SocioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

@Service
public class SocioService {
    private final SocioRepository socios;
    private final EventoAuditoriaRepository auditoria;
    public SocioService(SocioRepository socios, EventoAuditoriaRepository auditoria) { this.socios = socios; this.auditoria = auditoria; }

    @Transactional
    public Socio crear(String numero, String nombres, String apellidos, String correo) {
        String normalized = numero == null ? "" : numero.trim();
        if (normalized.isBlank() || nombres == null || nombres.isBlank() || apellidos == null || apellidos.isBlank()) throw new IllegalArgumentException("Número, nombres y apellidos son obligatorios");
        return registrar(normalized, nombres, apellidos, correo);
    }

    @Transactional
    public Socio actualizar(UUID id, String nombres, String apellidos, String correo) {
        Socio socio = socios.findById(id).orElseThrow(() -> new IllegalArgumentException("Socio no encontrado"));
        if (nombres == null || nombres.isBlank() || apellidos == null || apellidos.isBlank()) throw new IllegalArgumentException("Nombres y apellidos son obligatorios");
        socio.setNombres(nombres.trim()); socio.setApellidos(apellidos.trim()); socio.setCorreoElectronico(correo);
        audit(TipoEventoAuditoria.SOCIO_ACTUALIZADO, "Datos permitidos de socio actualizados", id);
        return socio;
    }

    @Transactional
    public int importarCsv(String csv) {
        if (csv == null || csv.isBlank()) throw new IllegalArgumentException("El archivo CSV está vacío");
        try (BufferedReader reader = new BufferedReader(new StringReader(csv))) {
            String line; int row = 0; int imported = 0;
            while ((line = reader.readLine()) != null) {
                row++;
                if (line.isBlank()) continue;
                List<String> fields = parseCsvLine(line);
                if (row == 1 && fields.get(0).replace("\uFEFF", "").equalsIgnoreCase("numeroSocio")) continue;
                if (fields.size() != 4) throw new IllegalArgumentException("Fila " + row + ": se esperan numeroSocio,nombres,apellidos,correoElectronico");
                registrar(fields.get(0), fields.get(1), fields.get(2), fields.get(3).isBlank() ? null : fields.get(3));
                imported++;
            }
            return imported;
        } catch (java.io.IOException e) { throw new IllegalArgumentException("No se pudo leer el CSV", e); }
    }

    private Socio registrar(String numero, String nombres, String apellidos, String correo) {
        String normalized = numero == null ? "" : numero.trim();
        if (normalized.isBlank() || nombres == null || nombres.isBlank() || apellidos == null || apellidos.isBlank()) throw new IllegalArgumentException("Número, nombres y apellidos son obligatorios");
        if (socios.findByNumeroSocio(normalized).isPresent()) throw new IllegalStateException("El número de socio ya existe: " + normalized);
        Socio saved = socios.save(Socio.builder().numeroSocio(normalized).nombres(nombres.trim()).apellidos(apellidos.trim()).correoElectronico(correo).build());
        audit(TipoEventoAuditoria.SOCIO_REGISTRADO, "Socio registrado", saved.getId());
        return saved;
    }

    private static List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>(); StringBuilder value = new StringBuilder(); boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') { value.append('"'); i++; }
                else quoted = !quoted;
            } else if (ch == ',' && !quoted) { values.add(value.toString().trim()); value.setLength(0); }
            else value.append(ch);
        }
        if (quoted) throw new IllegalArgumentException("CSV con comillas sin cerrar");
        values.add(value.toString().trim()); return values;
    }

    public Page<Socio> buscar(String q, Pageable pageable) {
        if (q == null || q.isBlank()) return socios.findAll(pageable);
        return socios.findByNumeroSocioContainingIgnoreCaseOrNombresContainingIgnoreCaseOrApellidosContainingIgnoreCase(q, q, q, pageable);
    }

    @Transactional
    public Socio habilitacion(UUID id, EstadoSocio estado, boolean activo) {
        Socio socio = socios.findById(id).orElseThrow(() -> new IllegalArgumentException("Socio no encontrado"));
        socio.setEstadoHabilitacion(estado); socio.setActivo(activo);
        audit(TipoEventoAuditoria.HABILITACION_MODIFICADA, "Habilitación de socio modificada", id);
        return socio;
    }

    private void audit(TipoEventoAuditoria type, String action, UUID id) {
        auditoria.save(EventoAuditoria.builder().tipoEvento(type).accion(action).resultado("SUCCESS")
                .detalles("{\"socioId\":\"" + id + "\"}").build());
    }
}
