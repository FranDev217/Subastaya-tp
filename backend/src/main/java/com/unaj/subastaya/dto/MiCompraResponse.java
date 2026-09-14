package com.unaj.subastaya.dto;

import com.unaj.subastaya.model.EstadoSubasta;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MiCompraResponse(
        Long subastaId,
        String titulo,
        String urlImagen,
        EstadoSubasta estado,
        BigDecimal miMejorPuja,
        BigDecimal ofertaActual,
        boolean soyElLider,
        LocalDateTime fechaFin
) {
}
