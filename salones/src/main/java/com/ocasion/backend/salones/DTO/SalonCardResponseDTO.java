package com.ocasion.backend.salones.DTO;

import lombok.Data;
import java.util.List;

@Data
public class SalonCardResponseDTO {
    private Integer id_salon;
    private boolean verificado;
    private String nombreSalon;
    private Integer capacidadPersonas;
    private Double precio_hora;
    private String imagen_portada;
    private String ciudad;
    private String direccion;
    private List<String> serviciosDestacados; // Máximo 4 servicios para la vista rápida
}