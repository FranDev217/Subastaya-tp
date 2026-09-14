package com.unaj.subastaya.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SubastaRequest(
        @Schema(description = "Título del producto", example = "Notebook Gamer RTX 4070")
        @NotBlank @Size(max = 200) String titulo,

        @Schema(description = "Descripción detallada del producto")
        @NotBlank String descripcion,

        @Schema(description = "URL de imagen referencial (opcional)",
                example = "https://picsum.photos/seed/notebook/600/400")
        @Size(max = 500) String urlImagen,

        @Schema(description = "Id de la categoría", example = "1")
        @NotNull Long categoriaId,

        @Schema(description = "Precio base inicial", example = "40000")
        @NotNull @Positive BigDecimal precioBase,

        @Schema(description = "Incremento mínimo exigido entre pujas sucesivas", example = "1000")
        @NotNull @Positive BigDecimal incrementoMinimo,

        @Schema(description = "Fecha/hora de inicio. Si es futura, la subasta arranca PROGRAMADA")
        @NotNull LocalDateTime fechaInicio,

        @Schema(description = "Fecha/hora de cierre. Debe ser futura y posterior al inicio")
        @NotNull @Future LocalDateTime fechaFin,

        @Schema(description = "Id del usuario vendedor (todavía no hay sesión: se manda explícito)",
                example = "1")
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
