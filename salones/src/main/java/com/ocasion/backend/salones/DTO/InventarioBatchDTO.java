package com.ocasion.backend.salones.DTO;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
@Getter 
@Setter 
public class InventarioBatchDTO {
    @Size(max = 8, message = "Máximo 8 categorías permitidas")
    @NotEmpty
    private List<CategoriaBatchDTO> categorias;
}
