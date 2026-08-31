package com.unaj.subastaya.service;

import com.unaj.subastaya.dto.SubastaEvento;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubastaNotificador {

    private static final String PREFIJO_TOPICO = "/topic/subastas/";

    private final SimpMessagingTemplate messagingTemplate;

    public void notificar(Long subastaId, SubastaEvento evento) {
        messagingTemplate.convertAndSend(PREFIJO_TOPICO + subastaId, evento);
    }
}
