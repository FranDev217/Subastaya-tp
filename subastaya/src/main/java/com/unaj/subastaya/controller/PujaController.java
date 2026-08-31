package com.unaj.subastaya.controller;

import com.unaj.subastaya.dto.PujaRequest;
import com.unaj.subastaya.dto.PujaResponse;
import com.unaj.subastaya.service.PujaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/subastas")
@RequiredArgsConstructor
public class PujaController {

    private final PujaService pujaService;

    @PostMapping("/{id}/pujas")
    public ResponseEntity<PujaResponse> registrarPuja(@PathVariable("id") Long subastaId,
                                                        @Valid @RequestBody PujaRequest request) {
        return ResponseEntity.ok(pujaService.registrarPuja(subastaId, request));
    }
}
