package com.ocasion.backend.salones.DTO;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
@Getter 
@Setter 
public class UbicacionDTO {
    @NotBlank(message = "La dirección es obligatoria")
    private String direccion;

    @NotNull(message = "La latitud es obligatoria")
    private BigDecimal latitud;

    @NotNull(message = "La longitud es obligatoria")
    private BigDecimal longitud;

    @NotBlank(message = "La ciudad es obligatoria")
    private String ciudad;

    @NotBlank(message = "El CP es obligatorio")
    private String cp;
}
