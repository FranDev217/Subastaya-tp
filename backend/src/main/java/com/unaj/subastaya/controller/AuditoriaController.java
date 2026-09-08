package com.unaj.subastaya.controller;

import com.unaj.subastaya.dto.AuditoriaLogResponse;
import com.unaj.subastaya.model.TipoEntidadAuditoria;
import com.unaj.subastaya.service.AuditoriaLogService;
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
public class AuditoriaController {

    private final AuditoriaLogService auditoriaLogService;

    @GetMapping
    public ResponseEntity<List<AuditoriaLogResponse>> listar(@RequestParam TipoEntidadAuditoria entidad,
                                                             @RequestParam Long entidadId) {
        return ResponseEntity.ok(auditoriaLogService.listar(entidad, entidadId));
    }
}
