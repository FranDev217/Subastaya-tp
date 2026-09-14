package com.unaj.subastaya.dto;

import com.unaj.subastaya.model.EstadoSubasta;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MiPublicacionResponse(
        Long id,
        String titulo,
        String urlImagen,
        EstadoSubasta estado,
        BigDecimal precioBase,
        BigDecimal ofertaActual,
        int cantidadPujas,
        LocalDateTime fechaFin,
        BigDecimal recaudacion
) {
}
