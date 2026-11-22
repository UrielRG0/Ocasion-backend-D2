package com.ocasion.backend.salones.Services;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ocasion.backend.salones.DTO.DisponibilidadDTO;
import com.ocasion.backend.salones.Entities.DisponibilidadEntity;
import com.ocasion.backend.salones.Entities.SalonEntity;
import com.ocasion.backend.salones.Repositories.DisponibilidadRepository;
import com.ocasion.backend.salones.Repositories.SalonRepository;

import java.util.List;

@Service
public class DisponibilidadService {

    private final DisponibilidadRepository disponibilidadRepository;

    private final SalonRepository salonRepository;

    DisponibilidadService(SalonRepository salonRepository,DisponibilidadRepository disponibilidadRepository) {
        this.disponibilidadRepository = disponibilidadRepository;
        this.salonRepository = salonRepository;
    }

    @Transactional
    public DisponibilidadEntity registrarBloqueo(Integer idSalon, DisponibilidadDTO dto, Integer idPropietario) {
        SalonEntity salon = propio(idSalon, idPropietario, true);
        if (dto.getFecha() == null || dto.getHora_inicio() == null || dto.getHora_fin() == null
            || dto.getFecha().isBefore(java.time.LocalDate.now())
            || !dto.getHora_inicio().toLocalDate().equals(dto.getFecha())
            || !dto.getHora_fin().toLocalDate().equals(dto.getFecha())
            || !dto.getHora_fin().isAfter(dto.getHora_inicio()))
            throw error(400, "Indique una fecha vigente y un intervalo válido del mismo día");
        if (dto.getObservaciones() != null && dto.getObservaciones().length() > 500)
            throw error(400, "La nota admite hasta 500 caracteres");
        // Mantiene la primera fila como horario inicial, también en salones antiguos sin horario.
        if (inicial(idSalon) == null) guardarHorario(salon,
            new com.ocasion.backend.salones.DTO.ActualizarHorarioDTO(java.time.LocalDate.now(),
                java.time.LocalTime.MIDNIGHT, java.time.LocalTime.of(23,59)));

        DisponibilidadEntity disponibilidad = new DisponibilidadEntity();
        disponibilidad.setHora_inicio(dto.getHora_inicio());
        disponibilidad.setHora_fin(dto.getHora_fin());
        disponibilidad.setFecha(dto.getFecha());
        disponibilidad.setObservaciones(dto.getObservaciones());
        disponibilidad.setSalon(salon);

        return disponibilidadRepository.save(disponibilidad);
    }

    @Transactional(readOnly = true)
    public List<DisponibilidadEntity> consultarBloqueos(Integer idSalon, Integer propietario) {
        var salon = propio(idSalon, propietario, false);
        var primera = inicial(idSalon);
        return salon.getDisponibilidades().stream().filter(d -> primera == null
            || !d.getId_disponibilidad().equals(primera.getId_disponibilidad()))
            .sorted(java.util.Comparator.comparing(DisponibilidadEntity::getHora_inicio)).toList();
    }

    @Transactional(readOnly = true)
    public com.ocasion.backend.salones.DTO.HorarioSalonDTO consultarHorario(Integer id, Integer propietario) {
        propio(id, propietario, false);
        var d = inicial(id);
        if (d == null || d.getHora_inicio() == null || d.getHora_fin() == null
            || !d.getHora_fin().toLocalTime().isAfter(d.getHora_inicio().toLocalTime())) return null;
        return new com.ocasion.backend.salones.DTO.HorarioSalonDTO(d.getFecha(),
            d.getHora_inicio().toLocalTime(), d.getHora_fin().toLocalTime());
    }

    @Transactional
    public com.ocasion.backend.salones.DTO.HorarioSalonDTO actualizarHorario(Integer id,
        com.ocasion.backend.salones.DTO.ActualizarHorarioDTO dto, Integer propietario) {
        var salon = propio(id, propietario, true);
        if (dto.fechaDesde() == null || dto.horaInicio() == null || dto.horaFin() == null
            || !dto.horaFin().isAfter(dto.horaInicio())) throw error(400, "La hora final debe ser posterior a la inicial");
        guardarHorario(salon, dto);
        return new com.ocasion.backend.salones.DTO.HorarioSalonDTO(dto.fechaDesde(), dto.horaInicio(), dto.horaFin());
    }

    @Transactional
    public void eliminarBloqueo(Integer id, Integer bloqueo, Integer propietario) {
        propio(id, propietario, true);
        var d = disponibilidadRepository.findById(bloqueo).filter(b -> b.getSalon().getId_salon().equals(id))
            .orElseThrow(() -> error(404, "Bloqueo no encontrado"));
        var primera = inicial(id);
        if (primera != null && primera.getId_disponibilidad().equals(bloqueo))
            throw error(400, "El horario de atención se modifica desde su formulario");
        disponibilidadRepository.delete(d);
    }

    private void guardarHorario(SalonEntity salon, com.ocasion.backend.salones.DTO.ActualizarHorarioDTO dto) {
        var d = inicial(salon.getId_salon());
        if (d == null) { d = new DisponibilidadEntity(); d.setSalon(salon); }
        d.setFecha(dto.fechaDesde()); d.setHora_inicio(dto.fechaDesde().atTime(dto.horaInicio()));
        d.setHora_fin(dto.fechaDesde().atTime(dto.horaFin()));
        disponibilidadRepository.saveAndFlush(d);
    }

    private DisponibilidadEntity inicial(Integer id) {
        return disponibilidadRepository.findInicial(id, org.springframework.data.domain.PageRequest.of(0,1))
            .stream().findFirst().orElse(null);
    }

    private SalonEntity propio(Integer id, Integer propietario, boolean bloquear) {
        var salon = (bloquear ? salonRepository.findByIdForUpdate(id) : salonRepository.findById(id))
            .orElseThrow(() -> error(404, "Salón no encontrado"));
        if (!java.util.Objects.equals(salon.getUsuariosIdUser(), propietario)) throw error(403, "No puedes administrar este salón");
        return salon;
    }

    private org.springframework.web.server.ResponseStatusException error(int status, String mensaje) {
        return new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.valueOf(status), mensaje);
    }
}
