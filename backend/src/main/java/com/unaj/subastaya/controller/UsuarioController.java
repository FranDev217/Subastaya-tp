package com.unaj.subastaya.controller;

import com.unaj.subastaya.dto.ErrorResponse;
import com.unaj.subastaya.dto.MiCompraResponse;
import com.unaj.subastaya.dto.MiPublicacionResponse;
import com.unaj.subastaya.service.ActividadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
@Tag(name = "Mis actividades", description = "Panel de usuario: sus compras/pujas y sus publicaciones (Módulo 5)")
public class UsuarioController {

    private final ActividadService actividadService;

    @Operation(summary = "Mis publicaciones",
            description = "Subastas creadas por el usuario como vendedor, con métricas de recaudación "
                    + "(solo si la subasta ya está FINALIZADA) y estado de adjudicación.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "Usuario inexistente",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{usuarioId}/publicaciones")
    public ResponseEntity<List<MiPublicacionResponse>> misPublicaciones(
            @Parameter(description = "Id del usuario vendedor") @PathVariable Long usuarioId) {
        return ResponseEntity.ok(actividadService.misPublicaciones(usuarioId));
    }

    @Operation(summary = "Mis compras / pujas",
            description = "Subastas donde el usuario pujó al menos una vez, indicando su mejor oferta "
                    + "y si sigue liderando (o ganó, si la subasta ya finalizó).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "Usuario inexistente",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{usuarioId}/compras")
    public ResponseEntity<List<MiCompraResponse>> misCompras(
            @Parameter(description = "Id del usuario comprador") @PathVariable Long usuarioId) {
        return ResponseEntity.ok(actividadService.misCompras(usuarioId));
    }
}
