package com.unaj.subastaya.controller;

import com.unaj.subastaya.dto.SubastaEvento;
import com.unaj.subastaya.service.SubastaService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class SubastaWebSocketController {

    private final SubastaService subastaService;

    @SubscribeMapping("/topic/subastas/{id}")
    public SubastaEvento estadoAlSuscribir(@DestinationVariable("id") Long subastaId) {
        return subastaService.estadoActual(subastaId);
    }
}
