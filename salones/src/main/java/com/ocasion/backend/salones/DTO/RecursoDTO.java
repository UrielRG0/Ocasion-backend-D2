package com.ocasion.backend.salones.DTO;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
public class RecursoDTO {
    @NotBlank
    private String nombre_recurso;
    @Min(1)
    private Integer cantidad_total;
}
