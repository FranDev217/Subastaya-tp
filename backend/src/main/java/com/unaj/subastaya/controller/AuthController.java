package com.unaj.subastaya.controller;

import com.unaj.subastaya.dto.ErrorResponse;
import com.unaj.subastaya.dto.LoginRequest;
import com.unaj.subastaya.dto.LoginResponse;
import com.unaj.subastaya.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sesiones")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Login modelado como creación de una sesión (sin token/sesión persistida en el servidor todavía)")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Crear sesión (login)",
            description = "Valida email + contraseña y devuelve la identidad del usuario "
                    + "(id, nombre, email). El frontend guarda esta respuesta en localStorage "
                    + "y la reutiliza donde haga falta identificar al usuario.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credenciales válidas"),
            @ApiResponse(responseCode = "401", description = "Email inexistente o contraseña incorrecta "
                    + "(mismo mensaje genérico en ambos casos)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Email con formato inválido o campos vacíos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
