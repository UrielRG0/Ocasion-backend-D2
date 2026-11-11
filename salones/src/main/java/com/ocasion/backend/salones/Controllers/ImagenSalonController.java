package com.ocasion.backend.salones.Controllers;

import com.ocasion.backend.salones.Repositories.ImagenLecturaRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ImagenSalonController {
    private final ImagenLecturaRepository imagenes;
    public ImagenSalonController(ImagenLecturaRepository imagenes) { this.imagenes=imagenes; }

    @GetMapping("/api/v1/salones/{id}/imagenes/{fotoId}")
    @org.springframework.transaction.annotation.Transactional(readOnly=true)
    public ResponseEntity<byte[]> foto(@PathVariable Integer id, @PathVariable Long fotoId,
            Authentication auth) {
        var foto=imagenes.metadatos(id,fotoId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        boolean propietario=auth!=null && String.valueOf(foto.getPropietario()).equals(auth.getName());
        boolean admin=auth!=null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_Admin"));
        if (!"PUBLICADO".equals(foto.getEstado()) && !propietario && !admin)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(foto.getTipo()))
            .contentLength(foto.getTamano()).cacheControl(CacheControl.noStore())
            .header("X-Content-Type-Options", "nosniff").body(imagenes.contenido(id,fotoId));
    }
}
