package com.unaj.subastaya.service;

import com.unaj.subastaya.dto.PujaResponse;
import com.unaj.subastaya.dto.SubastaEvento;
import com.unaj.subastaya.dto.SubastaListadoResponse;
import com.unaj.subastaya.dto.TipoEvento;
import com.unaj.subastaya.exception.RecursoNoEncontradoException;
import com.unaj.subastaya.model.EstadoSubasta;
import com.unaj.subastaya.model.Puja;
import com.unaj.subastaya.model.Subasta;
import com.unaj.subastaya.repository.PujaRepository;
import com.unaj.subastaya.repository.SubastaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

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
                TipoEvento.ESTADO_ACTUAL,
                subasta.getId(),
                subasta.getEstado(),
                montoActual,
                subasta.getFechaFin(),
                ultimaPuja
        );
    }

    @Transactional(readOnly = true)
    public List<SubastaListadoResponse> buscarSubastas(EstadoSubasta estado, Long categoriaId,
                                                        BigDecimal precioMin, BigDecimal precioMax,
                                                        String sort) {
        List<Subasta> subastas = subastaRepository.buscarConFiltros(estado, categoriaId, precioMin, precioMax);

        List<SubastaListadoResponse> respuestas = subastas.stream()
                .map(this::toListadoResponse)
                .toList();

        if ("mayorPuja".equals(sort)) {
            respuestas = respuestas.stream()
                    .sorted(Comparator.comparing(SubastaListadoResponse::ofertaActual).reversed())
                    .toList();
        }

        return respuestas;
    }

    private SubastaListadoResponse toListadoResponse(Subasta subasta) {
        Puja ultimaPuja = pujaRepository.findTopBySubastaIdOrderByMontoDesc(subasta.getId()).orElse(null);
        BigDecimal ofertaActual = ultimaPuja != null ? ultimaPuja.getMonto() : subasta.getPrecioBase();
        long cantidadPujas = pujaRepository.countBySubastaId(subasta.getId());

        return new SubastaListadoResponse(
                subasta.getId(),
                subasta.getTitulo(),
                subasta.getDescripcion(),
                subasta.getUrlImagen(),
                subasta.getCategoria().getId(),
                subasta.getCategoria().getNombre(),
                subasta.getPrecioBase(),
                ofertaActual,
                (int) cantidadPujas,
                subasta.getFechaInicio(),
                subasta.getFechaFin(),
                subasta.getEstado()
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
