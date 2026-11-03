package com.ocasion.backend.salones.Controllers;

import com.ocasion.backend.salones.Repositories.SalonRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ImagenSalonController {
    private final SalonRepository salones;
    public ImagenSalonController(SalonRepository salones) { this.salones=salones; }

    @GetMapping("/api/v1/salones/{id}/imagenes/{fotoId}")
    @org.springframework.transaction.annotation.Transactional(readOnly=true)
    public ResponseEntity<byte[]> foto(@PathVariable Integer id, @PathVariable Long fotoId,
            Authentication auth) {
        var salon=salones.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        boolean propietario=auth!=null && String.valueOf(salon.getUsuariosIdUser()).equals(auth.getName());
        boolean admin=auth!=null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_Admin"));
        if (!"PUBLICADO".equals(salon.getEstado()) && !propietario && !admin)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        var foto=salon.getFotosCatalogo().stream().filter(p -> p.getId().equals(fotoId)).findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(foto.getContentType()))
            .contentLength(foto.getTamano()).cacheControl(CacheControl.noStore())
            .header("X-Content-Type-Options", "nosniff").body(foto.getContenido());
    }
}
