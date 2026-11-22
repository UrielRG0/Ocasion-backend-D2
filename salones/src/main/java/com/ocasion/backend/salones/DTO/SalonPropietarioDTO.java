package com.ocasion.backend.salones.DTO;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Contrato de lectura del propietario, sin entidades persistentes ni archivos privados. */
@Data
public class SalonPropietarioDTO {
    private Integer id_salon;
    private String nombreSalon;
    private Integer capacidadPersonas;
    private String descripcion;
    private BigDecimal precio_hora;
    private Integer usuariosIdUser;
    private String estado;
    private LocalDateTime fechaSolicitud;
    private boolean verificado;
    private Ubicacion ubicacion;
    private Documentos documentosValidacionSalonEntity;
    private List<String> imagenes = new ArrayList<>();
    private List<ServicioAsignado> serviciosAsignados = new ArrayList<>();
    private List<Inventario> inventario = new ArrayList<>();
    private List<Disponibilidad> disponibilidades = new ArrayList<>();

    public record Ubicacion(Integer id_ubicacion, String direccion, BigDecimal latitud,
        BigDecimal longitud, String ciudad, String cp) {}
    public record Documentos(Integer idExpediente, String estadoRevisionAdmin, String observacionesAdmin,
        java.time.Instant fechaResolucion, java.time.Instant archivosEliminadosEn) {}
    public record Servicio(Integer idServicio, String nombreServicio, String descripcion) {}
    public record ServicioAsignado(IdServicio id, Servicio servicio, Integer numeroServiciosSolicitados) {}
    public record IdServicio(Integer salonId, Integer servicioId) {}
    public record Categoria(Integer idCategoria, String nombreCategoria, String descripcion) {}
    public record Inventario(Integer idRecurso, String nombreRecurso, Integer cantidadTotal, Categoria categoria) {}
    public record Disponibilidad(Integer id_disponibilidad, LocalDateTime hora_inicio,
        LocalDateTime hora_fin, LocalDate fecha, String observaciones) {}
}
