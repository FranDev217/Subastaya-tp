package com.unaj.subastaya.controller;

import com.unaj.subastaya.dto.ErrorResponse;
import com.unaj.subastaya.dto.PujaHistorialResponse;
import com.unaj.subastaya.dto.PujaRequest;
import com.unaj.subastaya.dto.PujaResponse;
import com.unaj.subastaya.dto.SubastaEvento;
import com.unaj.subastaya.dto.TipoEvento;
import com.unaj.subastaya.model.EstadoSubasta;
import com.unaj.subastaya.service.PujaService;
import com.unaj.subastaya.service.SubastaNotificador;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subastas")
@RequiredArgsConstructor
@Tag(name = "Pujas", description = "Escrow atómico, anti-sniping y difusión en vivo por WebSocket")
public class PujaController {

    private final PujaService pujaService;
    private final SubastaNotificador subastaNotificador;

    @Operation(summary = "Registrar una puja",
            description = """
                    Valida que la subasta esté ACTIVA, que el monto supere a la puja actual + \
                    incremento mínimo, y que el comprador tenga saldo disponible. Si todo es \
                    válido, congela el saldo del nuevo postor, libera el del anterior y evalúa \
                    la regla anti-sniping (extiende +2min si faltan <=60s para el cierre). \
                    La puja aceptada se difunde por WebSocket a /topic/subastas/{id}.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Puja aceptada"),
            @ApiResponse(responseCode = "400", description = "La subasta no está ACTIVA, o el body no "
                    + "pasa las validaciones de campo",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Subasta o comprador inexistente",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Monto por debajo del mínimo, o saldo "
                    + "disponible insuficiente",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Conflicto de concurrencia optimista "
                    + "(otra puja modificó la Billetera/Subasta primero): reintentar",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{id}/pujas")
    public ResponseEntity<PujaResponse> registrarPuja(
            @Parameter(description = "Id de la subasta") @PathVariable("id") Long subastaId,
            @Valid @RequestBody PujaRequest request) {
        PujaResponse puja = pujaService.registrarPuja(subastaId, request);

        SubastaEvento evento = new SubastaEvento(
                TipoEvento.NUEVA_PUJA,
                puja.subastaId(),
                EstadoSubasta.ACTIVA,
                puja.monto(),
                puja.incrementoMinimo(),
                puja.fechaFinSubasta(),
                puja
        );
        subastaNotificador.notificar(subastaId, evento);

        return ResponseEntity.ok(puja);
    }

    @Operation(summary = "Historial de pujas de una subasta",
            description = "Orden cronológico descendente (más reciente primero).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "Subasta inexistente",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}/pujas")
    public ResponseEntity<List<PujaHistorialResponse>> listarPujas(
            @Parameter(description = "Id de la subasta") @PathVariable("id") Long subastaId) {
        return ResponseEntity.ok(pujaService.listarPujas(subastaId));
    }
}
