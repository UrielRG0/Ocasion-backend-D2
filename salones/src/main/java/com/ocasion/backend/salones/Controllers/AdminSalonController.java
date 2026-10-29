package com.ocasion.backend.salones.Controllers;

import com.ocasion.backend.salones.DTO.ExpedienteAdminDTO;
import com.ocasion.backend.salones.DTO.SalonPendienteAdminDTO;
import com.ocasion.backend.salones.DTO.VeredictoAdminDTO;
import com.ocasion.backend.salones.Services.AdminValidacionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/salones")
@PreAuthorize(
    "hasAuthority('ROLE_Admin')"
)
public class AdminSalonController {

    private final AdminValidacionService adminValidacionService;

    public AdminSalonController(AdminValidacionService adminValidacionService) {
        this.adminValidacionService = adminValidacionService;
    }

    @GetMapping("/pendientes")
    public ResponseEntity<Page<SalonPendienteAdminDTO>> obtenerBandejaPendientes(
            @PageableDefault(size = 10, page = 0) Pageable pageable) {
        Page<SalonPendienteAdminDTO> pendientes = adminValidacionService.obtenerSalonesPendientes(pageable);
        
        return ResponseEntity.ok(pendientes);
    }

    @GetMapping("/{id}/expediente")
    public ResponseEntity<ExpedienteAdminDTO> verExpediente(@PathVariable Integer id) {
        ExpedienteAdminDTO expediente = adminValidacionService.obtenerExpediente(id);
        return ResponseEntity.ok(expediente);
    }
    @PatchMapping("/{id}/validar")
    public ResponseEntity<String> emitirVeredicto(
            @PathVariable Integer id, 
            @Valid @RequestBody VeredictoAdminDTO dto) {
            
        adminValidacionService.procesarVeredicto(id, dto);
        return ResponseEntity.ok("Veredicto procesado exitosamente.");
    }
}