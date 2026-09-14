package com.unaj.subastaya.controller;

import com.unaj.subastaya.dto.BilleteraResponse;
import com.unaj.subastaya.dto.DepositoRequest;
import com.unaj.subastaya.dto.ErrorResponse;
import com.unaj.subastaya.dto.MovimientoResponse;
import com.unaj.subastaya.service.BilleteraService;
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
@RequestMapping("/api/v1/billeteras")
@RequiredArgsConstructor
@Tag(name = "Billeteras", description = "Saldo, carga simulada e historial de movimientos (Módulo 4)")
public class BilleteraController {

    private final BilleteraService billeteraService;

    @Operation(summary = "Consultar saldo",
            description = "Desglose de saldo total, retenido (en garantía) y disponible.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "No existe billetera para ese usuario",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{usuarioId}")
    public ResponseEntity<BilleteraResponse> obtenerSaldo(
            @Parameter(description = "Id del usuario dueño de la billetera") @PathVariable Long usuarioId) {
        return ResponseEntity.ok(billeteraService.obtenerSaldo(usuarioId));
    }

    @Operation(summary = "Cargar saldo (simulado)",
            description = "Acreditación manual de fondos ficticios. Escribe un movimiento DEPOSITO "
                    + "en el Ledger y un evento ACREDITACION_MANUAL en la auditoría.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Saldo acreditado"),
            @ApiResponse(responseCode = "400", description = "Monto inválido (nulo o <= 0)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe billetera para ese usuario",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{usuarioId}/depositos")
    public ResponseEntity<BilleteraResponse> depositar(
            @Parameter(description = "Id del usuario dueño de la billetera") @PathVariable Long usuarioId,
            @Valid @RequestBody DepositoRequest request) {
        return ResponseEntity.ok(billeteraService.depositar(usuarioId, request.monto()));
    }

    @Operation(summary = "Historial de movimientos",
            description = "Depósitos, retenciones por puja, liberaciones por ser superado, y "
                    + "pagos/cobros por subastas ganadas/vendidas — orden más reciente primero.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "No existe billetera para ese usuario",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{usuarioId}/movimientos")
    public ResponseEntity<List<MovimientoResponse>> obtenerMovimientos(
            @Parameter(description = "Id del usuario dueño de la billetera") @PathVariable Long usuarioId) {
        return ResponseEntity.ok(billeteraService.obtenerMovimientos(usuarioId));
    }
}
