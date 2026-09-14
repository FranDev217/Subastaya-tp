package com.unaj.subastaya.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "Forma uniforme de error para toda la API: nunca un 500 genérico "
        + "sin traducir para los errores de negocio conocidos")
public record ErrorResponse(
        LocalDateTime timestamp,

        @Schema(description = "Código de estado HTTP", example = "409")
        int status,

        @Schema(description = "Mensaje legible del error", example = "Email o contraseña incorrectos")
        String mensaje,

        @Schema(description = "Errores de validación por campo (solo en 400 de Bean Validation)",
                nullable = true)
        Map<String, String> errores
) {

    public ErrorResponse(int status, String mensaje) {
        this(LocalDateTime.now(), status, mensaje, null);
    }

    public ErrorResponse(int status, String mensaje, Map<String, String> errores) {
        this(LocalDateTime.now(), status, mensaje, errores);
    }
}
