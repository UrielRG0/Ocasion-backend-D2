package com.ocasion.backend.salones.DTO;

import lombok.Data;
import java.util.List;

@Data
public class SalonDetalleClienteDTO {
    private Integer id_salon;
    private String nombreSalon;
    private String descripcion;
    private Integer capacidadPersonas;
    private Double precio_hora;
    private List<String> galeriaFotos; 
    private Double latitud;
    private Double longitud;
    private String direccion;
    private String ciudad;
    private List<String> serviciosCompletos;
}