package com.unaj.subastaya.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PujaRequest(
        @Schema(description = "Id del usuario que puja (todavía no hay sesión: se manda explícito)",
                example = "2")
        @NotNull Long compradorId,

        @Schema(description = "Monto ofertado. Debe ser >= puja actual (o precio base) + incremento mínimo",
                example = "46000")
        @NotNull @Positive BigDecimal monto
) {
}
