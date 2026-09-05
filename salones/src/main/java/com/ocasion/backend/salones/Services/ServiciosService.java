package com.ocasion.backend.salones.Services;

import com.ocasion.backend.salones.Entities.SalonServiciosEntity;
import com.ocasion.backend.salones.Entities.SalonServiciosId;
import com.ocasion.backend.salones.Entities.ServiciosEntity;
import com.ocasion.backend.salones.Repositories.SalonServiciosRepository;
import com.ocasion.backend.salones.Repositories.ServiciosRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ServiciosService {

    private final ServiciosRepository serviciosRepository;
    private final SalonServiciosRepository salonServiciosRepository;
    private final SalonService salonService; // Para reutilizar validación de propietario

    public ServiciosService(ServiciosRepository serviciosRepository, SalonServiciosRepository salonServiciosRepository, SalonService salonService) {
        this.serviciosRepository = serviciosRepository;
        this.salonServiciosRepository = salonServiciosRepository;
        this.salonService = salonService;
    }

    @Transactional
    public ServiciosEntity crearServicioPersonalizado(String nombre, String descripcion) {
        if (serviciosRepository.existsByNombreServicio(nombre)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El servicio ya existe.");
        }
        ServiciosEntity servicio = new ServiciosEntity();
        servicio.setNombreServicio(nombre);
        servicio.setDescripcion(descripcion);
        return serviciosRepository.save(servicio);
    }

    @Transactional
    public void desvincularServicio(Integer idSalon, Integer idServicio, Integer idPropietario) {
        // Validar propiedad del salón antes de desvincular
        salonService.obtenerSalonesPorPropietario(idPropietario).stream()
            .filter(s -> s.getId_salon().equals(idSalon))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para modificar este salón."));

        SalonServiciosId relacionId = new SalonServiciosId(idSalon, idServicio);
        if (!salonServiciosRepository.existsById(relacionId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El servicio no está vinculado a este salón.");
        }
        salonServiciosRepository.deleteById(relacionId);
    }
}