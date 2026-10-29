package com.ocasion.backend.salones.Services;

import com.ocasion.backend.salones.Entities.*;
import com.ocasion.backend.salones.Repositories.SalonRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class DocumentacionSalonService {
    public static final long MAX_ARCHIVO = 5L * 1024 * 1024;
    private final SalonRepository salones;

    public DocumentacionSalonService(SalonRepository salones) { this.salones = salones; }

    @Transactional
    public Map<String, Object> guardar(Integer id, Integer propietario, MultipartFile ine,
            MultipartFile propiedad, MultipartFile comprobante, MultipartFile vobo,
            List<MultipartFile> establecimiento, List<MultipartFile> propietarioFotos) {
        SalonEntity salon = salones.findByIdForUpdate(id)
            .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Salón no encontrado"));
        if (!Objects.equals(salon.getUsuariosIdUser(), propietario))
            throw error(HttpStatus.FORBIDDEN, "El salón no pertenece al usuario autenticado");
        if (!"PUBLICADO".equals(salon.getEstado()))
            throw error(HttpStatus.CONFLICT, "Primero debe publicar el salón para cargar documentos de verificación");
        var expedienteActual = salon.getDocumentosValidacionSalonEntity();
        if (expedienteActual != null && ("PENDIENTE".equals(expedienteActual.getEstadoRevisionAdmin())
                || "APROBADO".equals(expedienteActual.getEstadoRevisionAdmin())))
            throw error(HttpStatus.CONFLICT, "El expediente está en revisión o ya fue aprobado");
        if (establecimiento == null || establecimiento.isEmpty() || establecimiento.size() > 10)
            throw error(HttpStatus.BAD_REQUEST, "Debe adjuntar entre 1 y 10 fotos del establecimiento");
        if (propietarioFotos == null || propietarioFotos.size() < 3)
            throw error(HttpStatus.BAD_REQUEST, "Debe adjuntar al menos 3 fotos del propietario");

        // Validar todo antes de sustituir el expediente. Una carga reemplaza el conjunto completo.
        byte[] ineBytes = leer(ine, true);
        byte[] propiedadBytes = leer(propiedad, false);
        byte[] comprobanteBytes = leer(comprobante, false);
        byte[] voboBytes = leer(vobo, false);
        List<byte[]> fotosSalon = establecimiento.stream().map(f -> leer(f, true)).toList();
        List<byte[]> fotosPersona = propietarioFotos.stream().map(f -> leer(f, true)).toList();
        DocumentosValidacionSalonEntity docs = salon.getDocumentosValidacionSalonEntity();
        if (docs == null) {
            docs = new DocumentosValidacionSalonEntity();
            docs.setSalon(salon);
            salon.setDocumentosValidacionSalonEntity(docs);
        }
        docs.setInePropietarioPasaporte(ineBytes);
        docs.setDocumentoPropiedad(propiedadBytes);
        docs.setComprobanteDomSalon(comprobanteBytes);
        docs.setVistoBuenoProteccionCivil(voboBytes);
        docs.setFotoEstablecimiento(fotosSalon.getFirst());
        docs.setFotoPropietario(fotosPersona.getFirst());
        docs.getFotosAdicionales().clear();
        agregarFotos(docs, "foto-establecimiento", fotosSalon);
        agregarFotos(docs, "foto-propietario", fotosPersona);
        docs.setEstadoRevisionAdmin("SIN_ENVIAR");
        docs.setObservacionesAdmin(null);
        // merge puede crear una copia gestionada del expediente nuevo.
        // El ID generado se lee del resultado persistido, no de la referencia original.
        SalonEntity guardado = salones.saveAndFlush(salon);
        Integer idExpediente = guardado.getDocumentosValidacionSalonEntity().getIdExpediente();
        return Map.of("id_salon", id, "id_expediente", idExpediente,
            "estadoRevisionAdmin", "SIN_ENVIAR", "estadoSalon", salon.getEstado(),
            "fotosEstablecimiento", fotosSalon.size(), "fotosPropietario", fotosPersona.size(),
            "mensaje", "Documentos guardados. Ya puede solicitar la revisión.");
    }

    private void agregarFotos(DocumentosValidacionSalonEntity docs, String tipo, List<byte[]> fotos) {
        for (int i = 1; i < fotos.size(); i++) {
            FotoExpedienteEntity foto = new FotoExpedienteEntity();
            foto.setExpediente(docs);
            foto.setTipo(tipo);
            foto.setPosicion(i);
            foto.setContenido(fotos.get(i));
            docs.getFotosAdicionales().add(foto);
        }
    }

    private byte[] leer(MultipartFile archivo, boolean soloImagen) {
        if (archivo == null || archivo.isEmpty())
            throw error(HttpStatus.BAD_REQUEST, "Debe adjuntar todos los archivos obligatorios sin archivos vacíos");
        if (archivo.getSize() > MAX_ARCHIVO)
            throw error(HttpStatus.PAYLOAD_TOO_LARGE, "Máximo 5 MB por archivo");
        try {
            byte[] bytes = archivo.getBytes();
            String mime = tipoContenido(bytes);
            String nombre = Objects.toString(archivo.getOriginalFilename(), "").toLowerCase(Locale.ROOT);
            boolean extensionCorrecta = switch (mime) {
                case "image/jpeg" -> nombre.endsWith(".jpg") || nombre.endsWith(".jpeg");
                case "image/png" -> nombre.endsWith(".png");
                case "application/pdf" -> !soloImagen && nombre.endsWith(".pdf");
                default -> false;
            };
            if (!extensionCorrecta)
                throw error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, soloImagen
                    ? "INE y fotos: solo JPG o PNG con contenido correspondiente"
                    : "Documentos: solo PDF, JPG o PNG con contenido correspondiente");
            return bytes;
        } catch (IOException e) {
            throw error(HttpStatus.BAD_REQUEST, "No se pudo leer el archivo; vuelva a adjuntarlo");
        }
    }

    public static String tipoContenido(byte[] bytes) {
        if (bytes == null) return "application/octet-stream";
        if (bytes.length >= 3 && (bytes[0] & 255) == 255 && (bytes[1] & 255) == 216 && (bytes[2] & 255) == 255)
            return "image/jpeg";
        byte[] png = {(byte)137, 80, 78, 71, 13, 10, 26, 10};
        if (bytes.length >= 8 && Arrays.equals(Arrays.copyOf(bytes, 8), png)) return "image/png";
        if (bytes.length >= 5 && new String(bytes, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-"))
            return "application/pdf";
        return "application/octet-stream";
    }

    public static void comprobarExpediente(DocumentosValidacionSalonEntity docs) {
        if (docs == null || !existe(docs.getInePropietarioPasaporte()) || !existe(docs.getDocumentoPropiedad())
                || !existe(docs.getComprobanteDomSalon()) || !existe(docs.getVistoBuenoProteccionCivil())
                || !existe(docs.getFotoEstablecimiento()) || !existe(docs.getFotoPropietario())
                || contarFotos(docs, "foto-propietario") < 3 || contarFotos(docs, "foto-establecimiento") > 10)
            throw error(HttpStatus.UNPROCESSABLE_ENTITY,
                "Faltan documentos legales obligatorios para solicitar la revisión: cuatro documentos, de 1 a 10 fotos del establecimiento y al menos 3 del propietario");
    }

    public static long contarFotos(DocumentosValidacionSalonEntity docs, String tipo) {
        byte[] primera = "foto-establecimiento".equals(tipo) ? docs.getFotoEstablecimiento() : docs.getFotoPropietario();
        return (existe(primera) ? 1 : 0) + docs.getFotosAdicionales().stream()
            .filter(f -> tipo.equals(f.getTipo()) && existe(f.getContenido())).count();
    }

    @Transactional(readOnly = true)
    public byte[] archivo(Integer id, String tipo, int indice) {
        SalonEntity salon = salones.findById(id)
            .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Salón no encontrado"));
        DocumentosValidacionSalonEntity docs = salon.getDocumentosValidacionSalonEntity();
        if (docs == null || indice < 0) throw error(HttpStatus.NOT_FOUND, "Archivo no encontrado");
        byte[] bytes;
        if (indice > 0) {
            bytes = docs.getFotosAdicionales().stream()
                .filter(f -> tipo.equals(f.getTipo()) && f.getPosicion() == indice)
                .map(FotoExpedienteEntity::getContenido).findFirst().orElse(null);
        } else {
            bytes = switch (tipo) {
                case "ine" -> docs.getInePropietarioPasaporte();
                case "propiedad" -> docs.getDocumentoPropiedad();
                case "comprobante" -> docs.getComprobanteDomSalon();
                case "vobo" -> docs.getVistoBuenoProteccionCivil();
                case "foto-establecimiento" -> docs.getFotoEstablecimiento();
                case "foto-propietario" -> docs.getFotoPropietario();
                default -> null;
            };
        }
        if (!existe(bytes)) throw error(HttpStatus.NOT_FOUND, "Archivo no encontrado");
        return bytes;
    }

    private static boolean existe(byte[] bytes) { return bytes != null && bytes.length > 0; }
    private static ResponseStatusException error(HttpStatus status, String mensaje) {
        return new ResponseStatusException(status, mensaje);
    }
}
