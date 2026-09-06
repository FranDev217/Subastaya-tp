package com.unaj.subastaya.controller;

import com.unaj.subastaya.dto.BilleteraResponse;
import com.unaj.subastaya.dto.DepositoRequest;
import com.unaj.subastaya.service.BilleteraService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/billeteras")
@RequiredArgsConstructor
public class BilleteraController {

    private final BilleteraService billeteraService;

    @GetMapping("/{usuarioId}")
    public ResponseEntity<BilleteraResponse> obtenerSaldo(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(billeteraService.obtenerSaldo(usuarioId));
    }

    @PostMapping("/{usuarioId}/depositos")
    public ResponseEntity<BilleteraResponse> depositar(@PathVariable Long usuarioId,
                                                       @Valid @RequestBody DepositoRequest request) {
        return ResponseEntity.ok(billeteraService.depositar(usuarioId, request.monto()));
    }
}
