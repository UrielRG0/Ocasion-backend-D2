package com.ocasion.backend.salones.DTO;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
public class SalonCreateFase1DTO {
    @NotBlank(message = "El nombre es obligatorio")
    private String nombreSalon;
    
    @Min(value = 1)
    private Integer capacidadPersonas;
    
    @NotNull
    private BigDecimal precio_hora;
    
    private String descripcion;
    
    @Size(max = 5, message = "Máximo 5 imágenes permitidas")
    private List<String> imagenes;
    
    @Valid @NotNull
    private UbicacionDTO ubicacion;
    
    @Valid @NotNull
    private DisponibilidadDTO disponibilidadInicial;

}
