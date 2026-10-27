package com.ocasion.backend.salones.Controllers;

import com.ocasion.backend.salones.Services.DocumentacionSalonService;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

@RestController
public class DocumentosSalonController {
    private final DocumentacionSalonService documentos;
    public DocumentosSalonController(DocumentacionSalonService documentos) { this.documentos = documentos; }

    @PostMapping(value = "/api/v1/salones/{id}/documentos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('Vendedor')")
    public ResponseEntity<Map<String, Object>> subir(@PathVariable Integer id, Authentication usuario,
            @RequestPart("ine") MultipartFile ine,
            @RequestPart("propiedad") MultipartFile propiedad,
            @RequestPart("comprobante") MultipartFile comprobante,
            @RequestPart("vobo") MultipartFile vobo,
            @RequestPart("fotosEstablecimiento") List<MultipartFile> fotosEstablecimiento,
            @RequestPart("fotosPropietario") List<MultipartFile> fotosPropietario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentos.guardar(id,
            Integer.valueOf(usuario.getName()), ine, propiedad, comprobante, vobo,
            fotosEstablecimiento, fotosPropietario));
    }

    @GetMapping({"/api/v1/admin/salones/{id}/archivo/{tipo}",
                 "/api/v1/admin/salones/{id}/archivo/{tipo}/{indice}"})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> descargar(@PathVariable Integer id, @PathVariable String tipo,
            @PathVariable(required = false) Integer indice) {
        byte[] archivo = documentos.archivo(id, tipo, indice == null ? 0 : indice);
        String mime = DocumentacionSalonService.tipoContenido(archivo);
        String extension = switch (mime) {
            case "application/pdf" -> ".pdf";
            case "image/png" -> ".png";
            case "image/jpeg" -> ".jpg";
            default -> ".bin";
        };
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(mime))
            .contentLength(archivo.length).cacheControl(CacheControl.noStore())
            .header("X-Content-Type-Options", "nosniff")
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                .filename("salon-" + id + "-" + tipo + "-" + (indice == null ? 0 : indice) + extension).build().toString())
            .body(archivo);
    }
}
