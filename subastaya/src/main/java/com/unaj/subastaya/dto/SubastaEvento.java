package com.unaj.subastaya.dto;

import com.unaj.subastaya.model.EstadoSubasta;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SubastaEvento(
        TipoEvento tipo,
        Long subastaId,
        EstadoSubasta estado,
        BigDecimal montoActual,
        LocalDateTime fechaFin,
        PujaResponse ultimaPuja
) {

    public enum TipoEvento {
        NUEVA_PUJA,
        ESTADO_ACTUAL,
        ESTADO_CAMBIADO
    }
}
