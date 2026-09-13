package com.unaj.subastaya.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PujaHistorialResponse(
        Long id,
        String alias,
        BigDecimal monto,
        LocalDateTime fechaPuja
) {
}
