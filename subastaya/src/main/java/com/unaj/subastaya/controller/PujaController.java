package com.unaj.subastaya.controller;

import com.unaj.subastaya.dto.PujaRequest;
import com.unaj.subastaya.dto.PujaResponse;
import com.unaj.subastaya.dto.SubastaEvento;
import com.unaj.subastaya.model.EstadoSubasta;
import com.unaj.subastaya.service.PujaService;
import com.unaj.subastaya.service.SubastaNotificador;
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
    private final SubastaNotificador subastaNotificador;

    @PostMapping("/{id}/pujas")
    public ResponseEntity<PujaResponse> registrarPuja(@PathVariable("id") Long subastaId,
                                                        @Valid @RequestBody PujaRequest request) {
        PujaResponse puja = pujaService.registrarPuja(subastaId, request);

        SubastaEvento evento = new SubastaEvento(
                SubastaEvento.TipoEvento.NUEVA_PUJA,
                puja.subastaId(),
                EstadoSubasta.ACTIVA,
                puja.monto(),
                puja.fechaFinSubasta(),
                puja
        );
        subastaNotificador.notificar(subastaId, evento);

        return ResponseEntity.ok(puja);
    }
}
