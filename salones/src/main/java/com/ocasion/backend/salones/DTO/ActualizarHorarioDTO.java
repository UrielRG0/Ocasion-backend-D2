package com.ocasion.backend.salones.DTO;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record ActualizarHorarioDTO(@NotNull LocalDate fechaDesde,
    @NotNull LocalTime horaInicio, @NotNull LocalTime horaFin) {}
