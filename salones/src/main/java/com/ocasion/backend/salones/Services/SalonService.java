package com.ocasion.backend.salones.Services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.ocasion.backend.salones.DTO.InventarioBatchDTO;
import com.ocasion.backend.salones.DTO.SalonCreateFase1DTO;
import com.ocasion.backend.salones.DTO.ServicioAsignadoDTO;
import com.ocasion.backend.salones.Entities.DisponibilidadEntity;
import com.ocasion.backend.salones.Entities.SalonEntity;
import com.ocasion.backend.salones.Entities.UbicacionEntity;
import com.ocasion.backend.salones.Repositories.SalonRepository;

import jakarta.transaction.Transactional;

@Service
public class SalonService {

    private final SalonRepository salonRepository;

    SalonService(SalonRepository salonRepository) {
        this.salonRepository = salonRepository;
    }

    @Transactional
    public Map<String, Object> crearFase1(SalonCreateFase1DTO dto, Integer idPropietario) {
        SalonEntity salon = new SalonEntity();
        salon.setNombreSalon(dto.getNombreSalon());
        salon.setCapacidadPersonas(dto.getCapacidadPersonas());
        salon.setPrecio_hora(dto.getPrecio_hora());
        salon.setDescripcion(dto.getDescripcion());
        salon.setUsuariosIdUser(idPropietario);
        salon.setEstado("BORRADOR");

        if (dto.getImagenes() != null && !dto.getImagenes().isEmpty()) {
            salon.setImagen(String.join(",", dto.getImagenes()));
        }

        // 1. Mapear Ubicación
        UbicacionEntity ubicacion = new UbicacionEntity();
        ubicacion.setDireccion(dto.getUbicacion().getDireccion());
        ubicacion.setLatitud(dto.getUbicacion().getLatitud());
        ubicacion.setLongitud(dto.getUbicacion().getLongitud());
        ubicacion.setCiudad(dto.getUbicacion().getCiudad());
        ubicacion.setCP(dto.getUbicacion().getCp());
        ubicacion.setSalon(salon);
        salon.setUbicacion(ubicacion);

        // 2. Mapear Disponibilidad Inicial
        DisponibilidadEntity disp = new DisponibilidadEntity();
        disp.setHora_inicio(dto.getDisponibilidadInicial().getHora_inicio());
        disp.setHora_fin(dto.getDisponibilidadInicial().getHora_fin());
        disp.setFecha(dto.getDisponibilidadInicial().getFecha());
        disp.setObservaciones(dto.getDisponibilidadInicial().getObservaciones());
        disp.setSalon(salon);
        salon.setDisponibilidades(List.of(disp)); // Asumiendo relación mapeada

        salonRepository.save(salon);

        // Armar respuesta JSON solicitada
        Map<String, Object> response = new HashMap<>();
        response.put("id_salon", salon.getId_salon());
        response.put("estado", salon.getEstado());
        response.put("mensaje", "Fase 1 registrada exitosamente. Proceda con la asignación de servicios e inventario.");
        return response;
    }

    // --- Validación Reutilizable de Propiedad ---
    private SalonEntity obtenerSalonValidado(Integer idSalon, Integer idPropietario) {
        SalonEntity salon = salonRepository.findById(idSalon)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Salón no encontrado"));
        if (!salon.getUsuariosIdUser().equals(idPropietario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para modificar este salón");
        }
        return salon;
    }

    // --- FASE 2.A ---
    @Transactional
    public void asignarServicios(Integer idSalon, List<ServicioAsignadoDTO> servicios, Integer idPropietario) {
        SalonEntity salon = obtenerSalonValidado(idSalon, idPropietario);
        
        // logica faltante por tablas faltantes
        //la scrum master así lo quizo
    }

    // --- FASE 2.B ---
    @Transactional
    public void cargarInventarioLote(Integer idSalon, InventarioBatchDTO lote, Integer idPropietario) {
        SalonEntity salon = obtenerSalonValidado(idSalon, idPropietario);
        
         // logica faltante por tablas faltantes
        //la scrum master así lo quizo
    }

    // --- FASE 2.C ---
    @Transactional
    public void publicarSalon(Integer idSalon, Integer idPropietario) {
        SalonEntity salon = obtenerSalonValidado(idSalon, idPropietario);
        
        if (salon.getUbicacion() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El salón debe tener una ubicación para ser publicado");
        }
        
        // logica faltante por tablas faltantes
        //la scrum master así lo quizo
    }

    public List<SalonEntity> obtenerSalonesPorPropietario(Integer idPropietario) {
        return salonRepository.findByUsuariosIdUser(idPropietario);
    }
    @Transactional
    public SalonEntity actualizarSalon(Integer idSalon, SalonCreateFase1DTO dto, Integer idPropietario) {
        SalonEntity salon = salonRepository.findById(idSalon)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Salón no encontrado"));

        if (!salon.getUsuariosIdUser().equals(idPropietario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para modificar este salón");
        }
        salon.setNombreSalon(dto.getNombreSalon());
        salon.setCapacidadPersonas(dto.getCapacidadPersonas());
        salon.setPrecio_hora(dto.getPrecio_hora());
        salon.setDescripcion(dto.getDescripcion());
        if (dto.getImagenes() != null && !dto.getImagenes().isEmpty()) {
            salon.setImagen(String.join(",", dto.getImagenes()));
        } 
        UbicacionEntity ubicacion = salon.getUbicacion();
        if (ubicacion == null) {
            ubicacion = new UbicacionEntity();
            ubicacion.setSalon(salon);
            salon.setUbicacion(ubicacion);
        }
        
        ubicacion.setDireccion(dto.getUbicacion().getDireccion());
        ubicacion.setLatitud(dto.getUbicacion().getLatitud());
        ubicacion.setLongitud(dto.getUbicacion().getLongitud());
        ubicacion.setCiudad(dto.getUbicacion().getCiudad());
        ubicacion.setCP(dto.getUbicacion().getCp());
        return salonRepository.save(salon);
    }

    @Transactional
    public void eliminarSalon(Integer idSalon, Integer idPropietario) {
        SalonEntity salon = salonRepository.findById(idSalon)
            .orElseThrow(() -> new RuntimeException("Salón no encontrado"));
        
        if (!salon.getUsuariosIdUser().equals(idPropietario)) {
            throw new RuntimeException("No tienes permiso para eliminar este salón");
        }
        // Borrado en cascada (Elimina Salón y su Ubicación asociada)
        salonRepository.delete(salon);
    }
}