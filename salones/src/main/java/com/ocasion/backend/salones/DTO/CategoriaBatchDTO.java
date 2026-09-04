package com.ocasion.backend.salones.DTO;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public class CategoriaBatchDTO {
    @NotBlank
    private String nombreCategoria;
    
    @Size(max = 20, message = "Máximo 20 recursos por categoría")
    @NotEmpty
    private List<RecursoDTO> recursos;
}