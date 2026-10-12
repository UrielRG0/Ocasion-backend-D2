package com.ocasion.backend.salones.Services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

import com.ocasion.backend.salones.DTO.FiltroBusquedaDTO;
import com.ocasion.backend.salones.DTO.SalonCardResponseDTO;
import com.ocasion.backend.salones.DTO.SalonDetalleClienteDTO;
import com.ocasion.backend.salones.DTO.DisponibilidadMesDTO;

public interface ClienteSalonService {
    
    Page<SalonCardResponseDTO> buscarSalones(FiltroBusquedaDTO filtros, Pageable pageable);
    
    SalonDetalleClienteDTO obtenerDetalle(Integer id);
    
    List<DisponibilidadMesDTO> obtenerDisponibilidadMensual(Integer id, int mes, int anio);
}