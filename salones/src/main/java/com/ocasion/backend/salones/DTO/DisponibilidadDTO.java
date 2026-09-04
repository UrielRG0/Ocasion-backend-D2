package com.ocasion.backend.salones.DTO;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
@Getter 
@Setter 
public class DisponibilidadDTO {
    @NotNull(message = "La hora de inicio es obligatoria")
    private LocalDateTime hora_inicio;

    @NotNull(message = "La hora de fin es obligatoria")
    private LocalDateTime hora_fin;

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    private String observaciones;
}
