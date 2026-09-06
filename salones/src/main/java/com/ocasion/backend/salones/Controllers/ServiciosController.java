package com.ocasion.backend.salones.Controllers;

import com.ocasion.backend.salones.Entities.ServiciosEntity;
import com.ocasion.backend.salones.Services.ServiciosService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/servicios")
public class ServiciosController {

    private final ServiciosService serviciosService;

    public ServiciosController(ServiciosService serviciosService) {
        this.serviciosService = serviciosService;
    }

    @PostMapping("/personalizado")
    @PreAuthorize("hasRole('PROPIETARIO')")
    public ResponseEntity<ServiciosEntity> crearServicioPersonalizado(@RequestParam String nombre, @RequestParam String descripcion) {
        ServiciosEntity nuevoServicio = serviciosService.crearServicioPersonalizado(nombre, descripcion);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoServicio);
    }
}