package com.ocasion.backend.salones.Controllers;

import com.ocasion.backend.salones.Services.InventarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class InventarioController {

    private final InventarioService inventarioService;

    public InventarioController(InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    // --- RUTAS DE CATEGORÍAS ---
    
    @PutMapping("/categorias/{id}")
    @PreAuthorize("hasRole('Vendedor')")
    public ResponseEntity<String> editarCategoria(@PathVariable Integer id, @RequestParam String nombre, @RequestParam String descripcion) {
        inventarioService.actualizarCategoria(id, nombre, descripcion);
        return ResponseEntity.ok("Categoría actualizada.");
    }

    @DeleteMapping("/categorias/{id}")
    @PreAuthorize("hasRole('Vendedor')")
    public ResponseEntity<String> eliminarCategoria(@PathVariable Integer id) {
        inventarioService.eliminarCategoria(id);
        return ResponseEntity.ok("Categoría eliminada correctamente.");
    }

    // --- RUTAS DE INVENTARIO (RECURSOS) ---

    @PutMapping("/inventario/{id}")
    @PreAuthorize("hasRole('Vendedor')")
    public ResponseEntity<String> actualizarRecurso(@PathVariable Integer id, @RequestParam String nombre, @RequestParam Integer cantidad, Authentication authentication) {
        inventarioService.actualizarRecurso(id, nombre, cantidad, Integer.valueOf(authentication.getName()));
        return ResponseEntity.ok("Recurso actualizado.");
    }

    @DeleteMapping("/inventario/{id}")
    @PreAuthorize("hasRole('Vendedor')")
    public ResponseEntity<String> eliminarRecurso(@PathVariable Integer id, Authentication authentication) {
        inventarioService.eliminarRecurso(id, Integer.valueOf(authentication.getName()));
        return ResponseEntity.ok("Recurso eliminado del inventario.");
    }
}
