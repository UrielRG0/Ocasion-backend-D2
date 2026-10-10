package com.ocasion.backend.salones.DTO;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VeredictoAdminDTO {
    
    public enum Veredicto { APROBADO, RECHAZADO }

    @NotNull(message = "El veredicto es obligatorio")
    private Veredicto veredicto;

    @Size(max = 255, message = "Las observaciones no pueden exceder los 255 caracteres")
    private String observaciones;
}