package com.unaj.subastaya.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record DepositoRequest(
        @Schema(description = "Monto a acreditar (carga de saldo simulada)", example = "15000")
        @NotNull @Positive BigDecimal monto
) {
}
