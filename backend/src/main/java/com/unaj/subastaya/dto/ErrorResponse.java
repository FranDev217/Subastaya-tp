package com.unaj.subastaya.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String mensaje,
        Map<String, String> errores
) {

    public ErrorResponse(int status, String mensaje) {
        this(LocalDateTime.now(), status, mensaje, null);
    }

    public ErrorResponse(int status, String mensaje, Map<String, String> errores) {
        this(LocalDateTime.now(), status, mensaje, errores);
    }
}
