package com.unaj.subastaya.controller;

import com.unaj.subastaya.dto.AuditoriaLogResponse;
import com.unaj.subastaya.model.TipoEntidadAuditoria;
import com.unaj.subastaya.service.AuditoriaLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auditoria")
@RequiredArgsConstructor
@Tag(name = "Auditoría", description = "Trazabilidad inmutable de eventos críticos (cambios de estado, "
        + "anti-sniping, pujas rechazadas, acreditaciones manuales)")
public class AuditoriaController {

    private final AuditoriaLogService auditoriaLogService;

    @Operation(summary = "Trazabilidad por entidad",
            description = "Historial de eventos de auditoría para una entidad puntual (ej. una "
                    + "Subasta o una Billetera), orden más reciente primero. Solo lectura: no hay "
                    + "PUT/DELETE porque el log es append-only.")
    @ApiResponse(responseCode = "200", description = "OK (lista vacía si la entidad no tiene eventos)")
    @GetMapping
    public ResponseEntity<List<AuditoriaLogResponse>> listar(
            @Parameter(description = "Tipo de entidad auditada", example = "SUBASTA")
            @RequestParam TipoEntidadAuditoria entidad,
            @Parameter(description = "Id de la entidad (subasta_id, billetera_id, etc.)", example = "4")
            @RequestParam Long entidadId) {
        return ResponseEntity.ok(auditoriaLogService.listar(entidad, entidadId));
    }
}
