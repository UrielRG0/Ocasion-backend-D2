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
        SalonEntity salon = salonRepository.findById(idSalon)
            .orElseThrow(() -> new RuntimeException("Salón no encontrado"));

        if (!salon.getUsuariosIdUser().equals(idPropietario)) {
            throw new RuntimeException("No tienes permiso para bloquear fechas en este salón");
        }

        DisponibilidadEntity disponibilidad = new DisponibilidadEntity();
        disponibilidad.setHora_inicio(dto.getHora_inicio());
        disponibilidad.setHora_fin(dto.getHora_fin());
        disponibilidad.setFecha(dto.getFecha());
        disponibilidad.setObservaciones(dto.getObservaciones());
        disponibilidad.setSalon(salon);

        return disponibilidadRepository.save(disponibilidad);
    }

    public List<DisponibilidadEntity> consultarBloqueos(Integer idSalon) {
        SalonEntity salon = salonRepository.findById(idSalon)
            .orElseThrow(() -> new RuntimeException("Salón no encontrado"));
        return salon.getDisponibilidades();
    }
}