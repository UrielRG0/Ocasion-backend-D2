package com.ocasion.backend.salones.Services;

import com.ocasion.backend.salones.Entities.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;

/** Fotos comerciales del salón. No contiene archivos del expediente privado. */
public final class ImagenesSalon {
    public static final int MAX_BYTES = 5 * 1024 * 1024;
    private ImagenesSalon() {}

    public static List<ImagenSalonEntity> leer(List<MultipartFile> files) {
        if (files == null || files.isEmpty() || files.size() > 5)
            throw error(HttpStatus.BAD_REQUEST, "Seleccione entre 1 y 5 fotografías");
        List<ImagenSalonEntity> result = new ArrayList<>();
        for (var file : files) {
            if (file.isEmpty() || file.getSize() > MAX_BYTES)
                throw error(HttpStatus.PAYLOAD_TOO_LARGE, "Cada fotografía debe tener entre 1 byte y 5 MB");
            try { result.add(validar(file.getBytes())); }
            catch (IOException ex) { throw error(HttpStatus.BAD_REQUEST, "No se pudo leer la fotografía"); }
        }
        return result;
    }

    public static ImagenSalonEntity validar(byte[] bytes) {
        if (bytes.length == 0 || bytes.length > MAX_BYTES)
            throw error(HttpStatus.PAYLOAD_TOO_LARGE, "Máximo 5 MB por fotografía");
        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IOException();
            var reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!Set.of("png", "jpeg", "jpg").contains(format)) throw new IOException();
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > 8000 || height > 8000
                        || (long) width * height > 20_000_000)
                    throw error(HttpStatus.BAD_REQUEST, "Máximo 8000 píxeles por lado y 20 megapíxeles");
                if (reader.read(0) == null) throw new IOException();
                var photo = new ImagenSalonEntity();
                photo.setContentType(format.equals("png") ? "image/png" : "image/jpeg");
                photo.setTamano(bytes.length); photo.setContenido(bytes);
                return photo;
            } finally { reader.dispose(); }
        } catch (IOException | IllegalArgumentException ex) {
            throw error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Solo fotografías JPG o PNG válidas; no basta cambiar la extensión");
        }
    }

    public static void reemplazar(SalonEntity salon, List<ImagenSalonEntity> photos) {
        salon.getFotosCatalogo().clear();
        for (int i=0; i<photos.size(); i++) {
            var photo=photos.get(i); photo.setSalon(salon); photo.setPosicion(i);
            salon.getFotosCatalogo().add(photo);
        }
        salon.setImagen(null);
    }

    /** Compatibilidad de lectura: no separa la coma de una data URL Base64. */
    public static List<String> antiguas(String stored) {
        if (stored == null || stored.isBlank()) return List.of();
        var result = new ArrayList<String>();
        var parts = stored.split(",");
        for (int i=0; i<parts.length && result.size()<5; i++) {
            String value=parts[i].trim();
            if (value.matches("data:image/(png|jpeg);base64") && i+1<parts.length)
                value += "," + parts[++i].trim();
            if (!value.isEmpty() && !result.contains(value)) result.add(value);
        }
        return result;
    }

    /** Clientes antiguos pueden enviar Base64, pero se persiste como binario. */
    public static void desdeJson(SalonEntity salon, List<String> values) {
        if (values == null || values.isEmpty()) return; // actualización de texto conserva fotos
        if (values.size()>5) throw error(HttpStatus.BAD_REQUEST, "Máximo 5 fotografías");
        var photos = new ArrayList<ImagenSalonEntity>();
        for (String value : values) {
            if (value == null || !value.matches("data:image/(png|jpeg);base64,[A-Za-z0-9+/=]+")
                    || value.length() > 7_000_000)
                throw error(HttpStatus.BAD_REQUEST, "Suba archivos JPG/PNG; no se guardan enlaces ni rutas locales");
            try { photos.add(validar(Base64.getDecoder().decode(value.substring(value.indexOf(',')+1)))); }
            catch (IllegalArgumentException ex) { throw error(HttpStatus.BAD_REQUEST, "Base64 inválido"); }
        }
        reemplazar(salon, photos);
    }

    public static List<String> urls(SalonEntity salon) {
        if (salon.getFotosCatalogo().isEmpty()) return antiguas(salon.getImagen());
        return salon.getFotosCatalogo().stream()
            .map(p -> "/api/v1/salones/"+salon.getId_salon()+"/imagenes/"+p.getId()).toList();
    }
    private static ResponseStatusException error(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
}
