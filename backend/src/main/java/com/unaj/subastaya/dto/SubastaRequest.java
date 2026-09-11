package com.unaj.subastaya.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SubastaRequest(
        @NotBlank @Size(max = 200) String titulo,
        @NotBlank String descripcion,
        @Size(max = 500) String urlImagen,
        @NotNull Long categoriaId,
        @NotNull @Positive BigDecimal precioBase,
        @NotNull @Positive BigDecimal incrementoMinimo,
        @NotNull LocalDateTime fechaInicio,
        @NotNull @Future LocalDateTime fechaFin,
        @NotNull Long vendedorId
) {

    @AssertTrue(message = "La fecha de finalización debe ser posterior a la de inicio")
    public boolean isFechasCoherentes() {
        if (fechaInicio == null || fechaFin == null) {
            return true;
        }
        return fechaFin.isAfter(fechaInicio);
    }
}
