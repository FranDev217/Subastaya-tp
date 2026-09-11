package com.unaj.subastaya.dto;

import com.unaj.subastaya.model.EstadoSubasta;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SubastaListadoResponse(
        Long id,
        String titulo,
        String descripcion,
        String urlImagen,
        Long categoriaId,
        String categoriaNombre,
        BigDecimal precioBase,
        BigDecimal ofertaActual,
        Integer cantidadPujas,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        EstadoSubasta estado
) {
}
