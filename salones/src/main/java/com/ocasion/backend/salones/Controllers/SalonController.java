package com.ocasion.backend.salones.Controllers;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.ocasion.backend.salones.DTO.DisponibilidadDTO;
import com.ocasion.backend.salones.DTO.SalonCreateFase1DTO;
import com.ocasion.backend.salones.Entities.DisponibilidadEntity;
import com.ocasion.backend.salones.Entities.SalonEntity;
import com.ocasion.backend.salones.Services.DisponibilidadService;
import com.ocasion.backend.salones.Services.SalonService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/salones")
public class SalonController {

    private final SalonService salonService;

    private final DisponibilidadService disponibilidadService;

    SalonController(SalonService salonService, DisponibilidadService disponibilidadService) {
        this.salonService = salonService;
        this.disponibilidadService = disponibilidadService;
    }

    private Integer getPropietarioId() {
        return Integer.parseInt(SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @PostMapping("/fase1")
    @PreAuthorize("hasRole('Vendedor')")
    public ResponseEntity<Map<String, Object>> crearFase1(@Valid @RequestBody SalonCreateFase1DTO dto) {
        Map<String, Object> response = salonService.crearFase1(dto, getPropietarioId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/propietario")
    @PreAuthorize("hasRole('Vendedor')")
    public ResponseEntity<List<SalonEntity>> listarMisSalones() {
        return ResponseEntity.ok(salonService.obtenerSalonesPorPropietario(getPropietarioId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('Vendedor')")
    public ResponseEntity<SalonEntity> actualizarSalon(@PathVariable Integer id, @Valid @RequestBody SalonCreateFase1DTO dto) {
        return ResponseEntity.ok(salonService.actualizarSalon(id, dto, getPropietarioId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('Vendedor')")
    public ResponseEntity<String> eliminarSalon(@PathVariable Integer id) {
        salonService.eliminarSalon(id, getPropietarioId());
        return ResponseEntity.ok("Salón eliminado exitosamente");
    }

    @PostMapping("/{id}/disponibilidad")
    @PreAuthorize("hasRole('Vendedor')")
    public ResponseEntity<DisponibilidadEntity> registrarBloqueo(@PathVariable Integer id, @Valid @RequestBody DisponibilidadDTO dto) {
        return ResponseEntity.ok(disponibilidadService.registrarBloqueo(id, dto, getPropietarioId()));
    }

    @GetMapping("/{id}/disponibilidad")
    public ResponseEntity<List<DisponibilidadEntity>> consultarBloqueos(@PathVariable Integer id) {
        // Este método puede ser público o requerir rol CLIENTE/PROPIETARIO según tus reglas de negocio
        return ResponseEntity.ok(disponibilidadService.consultarBloqueos(id));
    }
}
