package com.ocasion.backend.salones.Controllers;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.ocasion.backend.salones.DTO.DisponibilidadDTO;
import com.ocasion.backend.salones.DTO.InventarioBatchDTO;
import com.ocasion.backend.salones.DTO.RecursoDTO;
import com.ocasion.backend.salones.DTO.SalonCreateFase1DTO;
import com.ocasion.backend.salones.DTO.ServicioAsignadoDTO;
import com.ocasion.backend.salones.Entities.DisponibilidadEntity;
import com.ocasion.backend.salones.Entities.SalonEntity;
import com.ocasion.backend.salones.Services.DisponibilidadService;
import com.ocasion.backend.salones.Services.SalonService;
import com.ocasion.backend.salones.Services.ServiciosService;
import com.ocasion.backend.salones.Services.InventarioService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/salones")
public class SalonController {

    private final SalonService salonService;

    private final DisponibilidadService disponibilidadService;

    private final ServiciosService serviciosService;     
    private final InventarioService inventarioService;   

    public SalonController(SalonService salonService, 
                           DisponibilidadService disponibilidadService,
                           ServiciosService serviciosService,
                           InventarioService inventarioService) {
        this.salonService = salonService;
        this.disponibilidadService = disponibilidadService;
        this.serviciosService = serviciosService;
        this.inventarioService = inventarioService;
    }

    private Integer getPropietarioId() {
        return Integer.parseInt(SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @PostMapping("/fase1")
    @PreAuthorize("hasRole('PROPIETARIO')")
    public ResponseEntity<Map<String, Object>> crearFase1(@Valid @RequestBody SalonCreateFase1DTO dto) {
        Map<String, Object> response = salonService.crearFase1(dto, getPropietarioId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/propietario")
    @PreAuthorize("hasRole('PROPIETARIO')")
    public ResponseEntity<List<SalonEntity>> listarMisSalones() {
        return ResponseEntity.ok(salonService.obtenerSalonesPorPropietario(getPropietarioId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PROPIETARIO')")
    public ResponseEntity<SalonEntity> actualizarSalon(@PathVariable Integer id, @Valid @RequestBody SalonCreateFase1DTO dto) {
        return ResponseEntity.ok(salonService.actualizarSalon(id, dto, getPropietarioId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PROPIETARIO')")
    public ResponseEntity<String> eliminarSalon(@PathVariable Integer id) {
        salonService.eliminarSalon(id, getPropietarioId());
        return ResponseEntity.ok("Salón eliminado exitosamente");
    }

    @PostMapping("/{id}/disponibilidad")
    @PreAuthorize("hasRole('PROPIETARIO')")
    public ResponseEntity<DisponibilidadEntity> registrarBloqueo(@PathVariable Integer id, @Valid @RequestBody DisponibilidadDTO dto) {
        return ResponseEntity.ok(disponibilidadService.registrarBloqueo(id, dto, getPropietarioId()));
    }

    @GetMapping("/{id}/disponibilidad")
    public ResponseEntity<List<DisponibilidadEntity>> consultarBloqueos(@PathVariable Integer id) {
        // Este método puede ser público o requerir rol CLIENTE/PROPIETARIO según tus reglas de negocio
        return ResponseEntity.ok(disponibilidadService.consultarBloqueos(id));
    }

    // --- FASE 2.A: Asignación de Servicios ---
    @PostMapping("/{id}/servicios")
    @PreAuthorize("hasRole('PROPIETARIO')")
    public ResponseEntity<String> asignarServicios(@PathVariable Integer id, @Valid @RequestBody List<ServicioAsignadoDTO> dto) {
        salonService.asignarServicios(id, dto, getPropietarioId());
        return ResponseEntity.status(HttpStatus.CREATED).body("Servicios asignados correctamente.");
    }

    // --- FASE 2.B: Carga de Inventario por Lote ---
    @PostMapping("/{id}/inventario-lote")
    @PreAuthorize("hasRole('PROPIETARIO')")
    public ResponseEntity<String> cargarInventarioLote(@PathVariable Integer id, @Valid @RequestBody InventarioBatchDTO dto) {
        salonService.cargarInventarioLote(id, dto, getPropietarioId());
        return ResponseEntity.status(HttpStatus.CREATED).body("Inventario estructurado cargado correctamente.");
    }

    // --- FASE 2.C: Publicación Final ---
    @PatchMapping("/{id}/publicar")
    @PreAuthorize("hasRole('PROPIETARIO')")
    public ResponseEntity<String> publicarSalon(@PathVariable Integer id) {
        salonService.publicarSalon(id, getPropietarioId());
        return ResponseEntity.ok("El salón ha sido publicado exitosamente.");
    }

    // --- DESVINCULAR SERVICIO ---
    @DeleteMapping("/{id}/servicios/{idServicio}")
    @PreAuthorize("hasRole('PROPIETARIO')")
    public ResponseEntity<String> desvincularServicio(@PathVariable Integer id, @PathVariable Integer idServicio) {
        serviciosService.desvincularServicio(id, idServicio, getPropietarioId());
        return ResponseEntity.ok("Servicio desvinculado del salón.");
    }

    // --- AGREGAR RECURSO INDIVIDUAL AL INVENTARIO ---
    @PostMapping("/{id}/inventario")
    @PreAuthorize("hasRole('PROPIETARIO')")
    public ResponseEntity<String> agregarRecurso(
            @PathVariable Integer id, 
            @RequestParam Integer idCategoria, 
            @Valid @RequestBody RecursoDTO dto) {
        inventarioService.agregarRecurso(id, idCategoria, dto.getNombre_recurso(), dto.getCantidad_total(), getPropietarioId());
        return ResponseEntity.status(HttpStatus.CREATED).body("Recurso agregado al inventario.");
    }

    @PostMapping("/{id}/categorias")
    @PreAuthorize("hasRole('PROPIETARIO')")
    public ResponseEntity<String> crearCategoria(
            @PathVariable Integer id, 
            @RequestParam String nombre, 
            @RequestParam(required = false) String descripcion) {
        inventarioService.crearCategoriaIndividual(id, nombre, descripcion, getPropietarioId());
        return ResponseEntity.status(HttpStatus.CREATED).body("Categoría creada exitosamente.");
    }
}
