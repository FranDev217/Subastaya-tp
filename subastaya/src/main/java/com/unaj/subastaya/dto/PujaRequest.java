package com.unaj.subastaya.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PujaRequest(
        @NotNull Long compradorId,
        @NotNull @Positive BigDecimal monto
) {
}
