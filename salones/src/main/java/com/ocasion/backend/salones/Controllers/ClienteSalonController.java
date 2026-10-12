package com.ocasion.backend.salones.Controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// Importamos tus DTOs
import com.ocasion.backend.salones.DTO.FiltroBusquedaDTO;
import com.ocasion.backend.salones.DTO.SalonCardResponseDTO;
import com.ocasion.backend.salones.DTO.SalonDetalleClienteDTO;
import com.ocasion.backend.salones.DTO.DisponibilidadMesDTO;
// Importamos el servicio (que vamos a crear en el paso 2)
import com.ocasion.backend.salones.Services.ClienteSalonService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/salones")
public class ClienteSalonController {

    @Autowired
    private ClienteSalonService clienteSalonService; 

    @GetMapping("/explorar")
    @PreAuthorize("hasRole('Cliente') or permitAll()") 
    public ResponseEntity<Page<SalonCardResponseDTO>> explorarSalones(
            @ModelAttribute FiltroBusquedaDTO filtros,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        if (filtros.getCapacidadMin() != null && filtros.getCapacidadMax() != null) {
            if (filtros.getCapacidadMin() > filtros.getCapacidadMax()) {
                return ResponseEntity.badRequest().build(); 
            }
        }
        if (filtros.getPrecioHoraMax() != null && filtros.getPrecioHoraMax() < 0) {
            return ResponseEntity.badRequest().build(); 
        }

        Page<SalonCardResponseDTO> resultados = clienteSalonService.buscarSalones(filtros, PageRequest.of(page, size));
        return ResponseEntity.ok(resultados);
    }

    @GetMapping("/{id}/detalle")
    @PreAuthorize("hasRole('Cliente') or permitAll()")
    public ResponseEntity<SalonDetalleClienteDTO> obtenerDetalleSalon(@PathVariable Integer id) {
        SalonDetalleClienteDTO detalle = clienteSalonService.obtenerDetalle(id);
        return ResponseEntity.ok(detalle);
    }

    @GetMapping("/{id}/calendario")
    @PreAuthorize("hasRole('Cliente') or permitAll()")
    public ResponseEntity<List<DisponibilidadMesDTO>> consultarCalendario(
            @PathVariable Integer id,
            @RequestParam int mes,
            @RequestParam int anio) {
            
        List<DisponibilidadMesDTO> bloqueos = clienteSalonService.obtenerDisponibilidadMensual(id, mes, anio);
        return ResponseEntity.ok(bloqueos);
    }
}