package com.ocasion.backend.salones.DTO;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InventarioBatchDTO {
    
    @NotEmpty(message = "El inventario debe contener al menos una categoría")
    @Size(max = 8, message = "Máximo 8 categorías permitidas por salón")
    @Valid
    private List<CategoriaBatchDTO> categorias;
}