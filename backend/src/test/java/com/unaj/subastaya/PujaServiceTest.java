package com.unaj.subastaya;

import com.unaj.subastaya.dto.PujaRequest;
import com.unaj.subastaya.dto.PujaResponse;
import com.unaj.subastaya.exception.MontoInvalidoException;
import com.unaj.subastaya.exception.SaldoInsuficienteException;
import com.unaj.subastaya.exception.SubastaNoActivaException;
import com.unaj.subastaya.model.AccionAuditoria;
import com.unaj.subastaya.model.AuditoriaLog;
import com.unaj.subastaya.model.Subasta;
import com.unaj.subastaya.model.TipoEntidadAuditoria;
import com.unaj.subastaya.repository.AuditoriaLogRepository;
import com.unaj.subastaya.repository.SubastaRepository;
import com.unaj.subastaya.service.PujaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "subastaya.worker.initial-delay-ms=600000")
@Sql(scripts = "/sql/reset-seed.sql")
@Transactional
class PujaServiceTest {

    private static final long SUBASTA_NOTEBOOK = 1L;
    private static final long SUBASTA_FIGURA = 2L;
    private static final long SUBASTA_PROGRAMADA = 3L;

    private static final long USUARIO_COMPRADOR_2 = 3L;
    private static final long USUARIO_SIN_FONDOS = 4L;

    @Autowired
    private PujaService pujaService;

    @Autowired
    private AuditoriaLogRepository auditoriaLogRepository;

    @Autowired
    private SubastaRepository subastaRepository;

    @Test
    void pujaEnSubastaNoActivaQuedaRegistradaComoRechazada() {
        assertThatThrownBy(() -> pujaService.registrarPuja(
                SUBASTA_PROGRAMADA, new PujaRequest(USUARIO_COMPRADOR_2, new BigDecimal("8500"))))
                .isInstanceOf(SubastaNoActivaException.class);

        assertThat(auditoriaDe(SUBASTA_PROGRAMADA))
                .extracting(AuditoriaLog::getAccion)
                .containsExactly(AccionAuditoria.PUJA_RECHAZADA);
        assertThat(auditoriaDe(SUBASTA_PROGRAMADA)).allSatisfy(registro -> {
            assertThat(registro.getUsuario().getId()).isEqualTo(USUARIO_COMPRADOR_2);
            assertThat(registro.getDetalleJson()).contains("PROGRAMADA");
        });
    }

    @Test
    void pujaConMontoMenorAlMinimoQuedaRegistradaComoRechazada() {
        assertThatThrownBy(() -> pujaService.registrarPuja(
                SUBASTA_NOTEBOOK, new PujaRequest(USUARIO_COMPRADOR_2, new BigDecimal("1000"))))
                .isInstanceOf(MontoInvalidoException.class);

        assertThat(auditoriaDe(SUBASTA_NOTEBOOK))
                .extracting(AuditoriaLog::getAccion)
                .containsExactly(AccionAuditoria.PUJA_RECHAZADA);
        assertThat(auditoriaDe(SUBASTA_NOTEBOOK).get(0).getDetalleJson()).contains("1000", "46000");
    }

    @Test
    void pujaSinSaldoQuedaRegistradaComoRechazada() {
        assertThatThrownBy(() -> pujaService.registrarPuja(
                SUBASTA_NOTEBOOK, new PujaRequest(USUARIO_SIN_FONDOS, new BigDecimal("46000"))))
                .isInstanceOf(SaldoInsuficienteException.class);

        assertThat(auditoriaDe(SUBASTA_NOTEBOOK))
                .extracting(AuditoriaLog::getAccion)
                .containsExactly(AccionAuditoria.PUJA_RECHAZADA);
        assertThat(auditoriaDe(SUBASTA_NOTEBOOK).get(0).getUsuario().getId()).isEqualTo(USUARIO_SIN_FONDOS);
    }

    @Test
    void rechazoDePujaPersisteAunqueLaTransaccionHagaRollback() {
        assertThatThrownBy(() -> pujaService.registrarPuja(
                SUBASTA_PROGRAMADA, new PujaRequest(USUARIO_COMPRADOR_2, new BigDecimal("8500"))))
                .isInstanceOf(SubastaNoActivaException.class);

        assertThat(auditoriaLogRepository.findByEntidadAndEntidadId(TipoEntidadAuditoria.SUBASTA, SUBASTA_PROGRAMADA))
                .isNotEmpty();
    }

    @Test
    void antiSnipingExtiendeFechaFinYQuedaRegistradoSinUsuario() {
        Subasta figura = subastaRepository.findById(SUBASTA_FIGURA).orElseThrow();
        LocalDateTime fechaFinCercana = LocalDateTime.now().plusSeconds(30);
        figura.setFechaFin(fechaFinCercana);

        PujaResponse response = pujaService.registrarPuja(
                SUBASTA_FIGURA, new PujaRequest(USUARIO_COMPRADOR_2, new BigDecimal("5500")));

        assertThat(response.extendidoPorAntiSniping()).isTrue();
        assertThat(response.fechaFinSubasta()).isEqualTo(fechaFinCercana.plusMinutes(2));
        assertThat(response.compradorAlias()).isEqualTo("Pujador #" + USUARIO_COMPRADOR_2);
        assertThat(response.incrementoMinimo()).isEqualByComparingTo("500");

        List<AuditoriaLog> auditoria = auditoriaDe(SUBASTA_FIGURA);
        assertThat(auditoria).extracting(AuditoriaLog::getAccion).containsExactly(AccionAuditoria.EXTENSION_TIEMPO);
        assertThat(auditoria).allSatisfy(registro -> {
            assertThat(registro.getUsuario()).isNull();
            assertThat(registro.getDetalleJson()).contains("anti-sniping");
        });
    }

    private List<AuditoriaLog> auditoriaDe(Long subastaId) {
        return auditoriaLogRepository.findByEntidadAndEntidadId(TipoEntidadAuditoria.SUBASTA, subastaId);
    }
}
