package com.unaj.subastaya.dto;

import com.unaj.subastaya.model.TipoEntidadAuditoria;

import java.time.LocalDateTime;

public record AuditoriaLogResponse(
        Long id,
        TipoEntidadAuditoria entidad,
        Long entidadId,
        String accion,
        Long usuarioId,
        String detalleJson,
        LocalDateTime fecha
) {
}
