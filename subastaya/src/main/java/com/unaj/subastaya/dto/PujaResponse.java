package com.unaj.subastaya.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PujaResponse(
        Long id,
        Long subastaId,
        Long compradorId,
        String compradorNombre,
        BigDecimal monto,
        LocalDateTime fechaPuja,
        LocalDateTime fechaFinSubasta,
        boolean extendidoPorAntiSniping
) {
}
