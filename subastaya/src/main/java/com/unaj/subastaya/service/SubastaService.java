package com.unaj.subastaya.service;

import com.unaj.subastaya.dto.PujaResponse;
import com.unaj.subastaya.dto.SubastaEvento;
import com.unaj.subastaya.exception.RecursoNoEncontradoException;
import com.unaj.subastaya.model.Puja;
import com.unaj.subastaya.model.Subasta;
import com.unaj.subastaya.repository.PujaRepository;
import com.unaj.subastaya.repository.SubastaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class SubastaService {

    private final SubastaRepository subastaRepository;
    private final PujaRepository pujaRepository;

    @Transactional(readOnly = true)
    public SubastaEvento estadoActual(Long subastaId) {
        Subasta subasta = subastaRepository.findById(subastaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Subasta " + subastaId + " no encontrada"));

        PujaResponse ultimaPuja = pujaRepository.findTopBySubastaIdOrderByMontoDesc(subastaId)
                .map(puja -> toResponse(puja, subasta))
                .orElse(null);

        BigDecimal montoActual = ultimaPuja != null ? ultimaPuja.monto() : subasta.getPrecioBase();

        return new SubastaEvento(
                SubastaEvento.TipoEvento.ESTADO_ACTUAL,
                subasta.getId(),
                subasta.getEstado(),
                montoActual,
                subasta.getFechaFin(),
                ultimaPuja
        );
    }

    private PujaResponse toResponse(Puja puja, Subasta subasta) {
        return new PujaResponse(
                puja.getId(),
                subasta.getId(),
                puja.getComprador().getId(),
                puja.getComprador().getNombre(),
                puja.getMonto(),
                puja.getFechaPuja(),
                subasta.getFechaFin(),
                false
        );
    }
}
