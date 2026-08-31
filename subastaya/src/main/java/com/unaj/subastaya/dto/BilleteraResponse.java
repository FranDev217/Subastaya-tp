package com.unaj.subastaya.dto;

import java.math.BigDecimal;

public record BilleteraResponse(
        Long id,
        Long usuarioId,
        BigDecimal saldoTotal,
        BigDecimal saldoRetenido,
        BigDecimal saldoDisponible
) {
}
