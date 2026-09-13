package com.unaj.subastaya.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PujaResponse(
        Long id,
        Long subastaId,
        Long compradorId,
        String compradorAlias,
        BigDecimal monto,
        LocalDateTime fechaPuja,
        LocalDateTime fechaFinSubasta,
        BigDecimal incrementoMinimo,
        boolean extendidoPorAntiSniping
) {

    public static String aliasDe(Long usuarioId) {
        return "Pujador #" + usuarioId;
    }
}
