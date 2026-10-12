package com.ocasion.backend.salones.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.ocasion.backend.salones.DTO.*;
import com.ocasion.backend.salones.Entities.*;
import com.ocasion.backend.salones.Repositories.DisponibilidadRepository;
import com.ocasion.backend.salones.Repositories.SalonRepository;
import com.ocasion.backend.salones.Repositories.SalonSpecification;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClienteSalonServiceImpl implements ClienteSalonService {

    @Autowired
    private SalonRepository salonRepository;

    @Autowired
    private DisponibilidadRepository disponibilidadRepository;

    @Override
    public Page<SalonCardResponseDTO> buscarSalones(FiltroBusquedaDTO filtros, Pageable pageable) {
        Specification<SalonEntity> spec = SalonSpecification.aplicarFiltros(filtros);
        Page<SalonEntity> salones = salonRepository.findAll(spec, pageable);

        return salones.map(salon -> {
            SalonCardResponseDTO dto = new SalonCardResponseDTO();
            dto.setId_salon(salon.getId_salon());
            
            dto.setNombreSalon(salon.getNombreSalon()); 
            dto.setCapacidadPersonas(salon.getCapacidadPersonas());
            
            // CORRECCIÓN: Usando getPrecio_hora()
            if (salon.getPrecio_hora() != null) {
                dto.setPrecio_hora(salon.getPrecio_hora().doubleValue());
            }
            
            dto.setImagen_portada(salon.getImagen());
            
            if (salon.getUbicacion() != null) {
                dto.setCiudad(salon.getUbicacion().getCiudad());
                dto.setDireccion(salon.getUbicacion().getDireccion());
            }

            // CORRECCIÓN: Usando getServiciosAsignados()
            if (salon.getServiciosAsignados() != null) {
                List<String> destacados = salon.getServiciosAsignados().stream()
                        .map(relacion -> relacion.getServicio().getNombreServicio())
                        .limit(4)
                        .collect(Collectors.toList());
                dto.setServiciosDestacados(destacados);
            }

            return dto;
        });
    }

    @Override
    public SalonDetalleClienteDTO obtenerDetalle(Integer id) {
        SalonEntity salon = salonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Salón no encontrado"));

        SalonDetalleClienteDTO dto = new SalonDetalleClienteDTO();
        dto.setId_salon(salon.getId_salon());
        dto.setNombreSalon(salon.getNombreSalon());
        dto.setDescripcion(salon.getDescripcion());
        dto.setCapacidadPersonas(salon.getCapacidadPersonas());
        
        // CORRECCIÓN: Usando getPrecio_hora()
        if (salon.getPrecio_hora() != null) {
            dto.setPrecio_hora(salon.getPrecio_hora().doubleValue());
        }
        
        if (salon.getUbicacion() != null) {
            if (salon.getUbicacion().getLatitud() != null) {
                dto.setLatitud(salon.getUbicacion().getLatitud().doubleValue());
            }
            if (salon.getUbicacion().getLongitud() != null) {
                dto.setLongitud(salon.getUbicacion().getLongitud().doubleValue());
            }
            dto.setDireccion(salon.getUbicacion().getDireccion());
            dto.setCiudad(salon.getUbicacion().getCiudad());
        }

        // CORRECCIÓN: Usando getServiciosAsignados()
        if (salon.getServiciosAsignados() != null) {
            List<String> todosLosServicios = salon.getServiciosAsignados().stream()
                    .map(relacion -> relacion.getServicio().getNombreServicio())
                    .collect(Collectors.toList());
            dto.setServiciosCompletos(todosLosServicios);
        }

        return dto;
    }

    @Override
    public List<DisponibilidadMesDTO> obtenerDisponibilidadMensual(Integer id, int mes, int anio) {
        YearMonth anioMes = YearMonth.of(anio, mes);
        LocalDate inicioMes = anioMes.atDay(1);
        LocalDate finMes = anioMes.atEndOfMonth();

        List<DisponibilidadEntity> entidades = disponibilidadRepository.findDisponibilidadPorMes(id, inicioMes, finMes);

        return entidades.stream().map(d -> {
            DisponibilidadMesDTO dto = new DisponibilidadMesDTO();
            dto.setFecha(d.getFecha());
            
            if (d.getHora_inicio() != null) dto.setHora_inicio(d.getHora_inicio().toLocalTime());
            if (d.getHora_fin() != null) dto.setHora_fin(d.getHora_fin().toLocalTime());
            
            dto.setTipo_bloqueo(d.getObservaciones()); 
            return dto;
        }).collect(Collectors.toList());
    }
}