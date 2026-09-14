package com.unaj.subastaya.dto;

import com.unaj.subastaya.model.TipoMovimiento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovimientoResponse(
        Long id,
        TipoMovimiento tipo,
        BigDecimal monto,
        LocalDateTime fecha,
        Long subastaId,
        String subastaTitulo
) {
}
