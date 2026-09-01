package com.unaj.subastaya.service;

import com.unaj.subastaya.model.EstadoSubasta;
import com.unaj.subastaya.model.Subasta;
import com.unaj.subastaya.repository.SubastaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LiquidacionService {

    private final SubastaRepository subastaRepository;

    /**
     * Subastas que ya pasaron su fecha de cierre y todavía figuran como ACTIVA,
     * es decir, las que el Worker todavía no adjudicó.
     */
    @Transactional(readOnly = true)
    public List<Subasta> obtenerSubastasVencidas() {
        return subastaRepository.findByEstadoAndFechaFinBefore(EstadoSubasta.ACTIVA, LocalDateTime.now());
    }
}
