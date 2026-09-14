package com.unaj.subastaya.service;

import com.unaj.subastaya.dto.MiCompraResponse;
import com.unaj.subastaya.dto.MiPublicacionResponse;
import com.unaj.subastaya.exception.RecursoNoEncontradoException;
import com.unaj.subastaya.model.EstadoSubasta;
import com.unaj.subastaya.model.Puja;
import com.unaj.subastaya.model.Subasta;
import com.unaj.subastaya.repository.PujaRepository;
import com.unaj.subastaya.repository.SubastaRepository;
import com.unaj.subastaya.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ActividadService {

    private final SubastaRepository subastaRepository;
    private final PujaRepository pujaRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public List<MiPublicacionResponse> misPublicaciones(Long usuarioId) {
        validarUsuario(usuarioId);
        return subastaRepository.findByVendedorIdOrderByFechaInicioDesc(usuarioId).stream()
                .map(this::toPublicacionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MiCompraResponse> misCompras(Long usuarioId) {
        validarUsuario(usuarioId);
        return pujaRepository.buscarSubastasConPujaDe(usuarioId).stream()
                .map(subasta -> toCompraResponse(subasta, usuarioId))
                .toList();
    }

    private void validarUsuario(Long usuarioId) {
        if (!usuarioRepository.existsById(usuarioId)) {
            throw new RecursoNoEncontradoException("Usuario " + usuarioId + " no encontrado");
        }
    }

    private MiPublicacionResponse toPublicacionResponse(Subasta subasta) {
        Puja lider = pujaRepository.findTopBySubastaIdOrderByMontoDesc(subasta.getId()).orElse(null);
        BigDecimal ofertaActual = lider != null ? lider.getMonto() : subasta.getPrecioBase();
        long cantidadPujas = pujaRepository.countBySubastaId(subasta.getId());
        BigDecimal recaudacion = subasta.getEstado() == EstadoSubasta.FINALIZADA ? ofertaActual : null;

        return new MiPublicacionResponse(
                subasta.getId(),
                subasta.getTitulo(),
                subasta.getUrlImagen(),
                subasta.getEstado(),
                subasta.getPrecioBase(),
                ofertaActual,
                (int) cantidadPujas,
                subasta.getFechaFin(),
                recaudacion
        );
    }

    private MiCompraResponse toCompraResponse(Subasta subasta, Long usuarioId) {
        Puja lider = pujaRepository.findTopBySubastaIdOrderByMontoDesc(subasta.getId()).orElse(null);
        BigDecimal ofertaActual = lider != null ? lider.getMonto() : subasta.getPrecioBase();
        boolean soyElLider = lider != null && lider.getComprador().getId().equals(usuarioId);

        BigDecimal miMejorPuja = pujaRepository
                .findTopBySubastaIdAndCompradorIdOrderByMontoDesc(subasta.getId(), usuarioId)
                .map(Puja::getMonto)
                .orElse(BigDecimal.ZERO);

        return new MiCompraResponse(
                subasta.getId(),
                subasta.getTitulo(),
                subasta.getUrlImagen(),
                subasta.getEstado(),
                miMejorPuja,
                ofertaActual,
                soyElLider,
                subasta.getFechaFin()
        );
    }
}
