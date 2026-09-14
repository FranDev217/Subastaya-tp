package com.unaj.subastaya.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(description = "Email registrado del usuario", example = "comprador1@test.com")
        @NotBlank @Email String email,

        @Schema(description = "Contraseña en texto plano; se valida contra el hash BCrypt guardado",
                example = "Password123!")
        @NotBlank String password
) {
}
