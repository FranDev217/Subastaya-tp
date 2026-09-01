package com.unaj.subastaya;

import com.unaj.subastaya.model.EstadoSubasta;
import com.unaj.subastaya.repository.SubastaRepository;
import com.unaj.subastaya.service.LiquidacionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class LiquidacionServiceTest {

    private static final long SUBASTA_VENCIDA_SIN_PUJAS = 5L;
    private static final long SUBASTA_PROGRAMADA = 3L;

    @Autowired
    private LiquidacionService liquidacionService;

    @Autowired
    private SubastaRepository subastaRepository;

    @Test
    void subastaVencidaSinPujasPasaADesierta() {
        liquidacionService.cerrarSubasta(SUBASTA_VENCIDA_SIN_PUJAS);

        assertThat(estadoDe(SUBASTA_VENCIDA_SIN_PUJAS)).isEqualTo(EstadoSubasta.DESIERTA);
    }

    @Test
    void subastaProgramadaNoSeCierra() {
        liquidacionService.cerrarSubasta(SUBASTA_PROGRAMADA);

        assertThat(estadoDe(SUBASTA_PROGRAMADA)).isEqualTo(EstadoSubasta.PROGRAMADA);
    }

    private EstadoSubasta estadoDe(Long subastaId) {
        return subastaRepository.findById(subastaId).orElseThrow().getEstado();
    }
}
