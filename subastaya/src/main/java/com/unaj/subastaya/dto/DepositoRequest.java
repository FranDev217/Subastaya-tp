package com.unaj.subastaya.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record DepositoRequest(
        @NotNull @Positive BigDecimal monto
) {
}
