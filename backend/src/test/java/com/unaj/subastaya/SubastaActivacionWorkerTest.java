package com.unaj.subastaya;

import com.unaj.subastaya.model.AccionAuditoria;
import com.unaj.subastaya.model.AuditoriaLog;
import com.unaj.subastaya.model.EstadoSubasta;
import com.unaj.subastaya.model.Subasta;
import com.unaj.subastaya.model.TipoEntidadAuditoria;
import com.unaj.subastaya.repository.AuditoriaLogRepository;
import com.unaj.subastaya.repository.SubastaRepository;
import com.unaj.subastaya.service.SubastaActivacionWorker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "subastaya.worker.initial-delay-ms=600000")
@Sql(scripts = "/sql/reset-seed.sql")
@Transactional
class SubastaActivacionWorkerTest {

    private static final long SUBASTA_PROGRAMADA = 3L;
    private static final long SUBASTA_ACTIVA = 1L;

    @Autowired
    private SubastaActivacionWorker worker;

    @Autowired
    private SubastaRepository subastaRepository;

    @Autowired
    private AuditoriaLogRepository auditoriaLogRepository;

    @Test
    void activaLaSubastaProgramadaCuandoSuInicioYaLlego() {
        Subasta subasta = subastaRepository.findById(SUBASTA_PROGRAMADA).orElseThrow();
        subasta.setFechaInicio(LocalDateTime.now().minusMinutes(1));
        subasta.setFechaFin(LocalDateTime.now().plusHours(1));

        worker.activarSubastasProgramadas();

        assertThat(subastaRepository.findById(SUBASTA_PROGRAMADA).orElseThrow().getEstado())
                .isEqualTo(EstadoSubasta.ACTIVA);

        List<AuditoriaLog> auditoria = auditoriaDe(SUBASTA_PROGRAMADA);
        assertThat(auditoria).extracting(AuditoriaLog::getAccion)
                .containsExactly(AccionAuditoria.APERTURA_WORKER);
        assertThat(auditoria).allSatisfy(registro -> assertThat(registro.getUsuario()).isNull());
    }

    @Test
    void noActivaLaSubastaProgramadaCuyoInicioAunNoLlego() {
        Subasta subasta = subastaRepository.findById(SUBASTA_PROGRAMADA).orElseThrow();
        subasta.setFechaInicio(LocalDateTime.now().plusHours(1));
        subasta.setFechaFin(LocalDateTime.now().plusHours(2));

        worker.activarSubastasProgramadas();

        assertThat(subastaRepository.findById(SUBASTA_PROGRAMADA).orElseThrow().getEstado())
                .isEqualTo(EstadoSubasta.PROGRAMADA);
        assertThat(auditoriaDe(SUBASTA_PROGRAMADA)).isEmpty();
    }

    @Test
    void noTocaLasSubastasQueYaEstanActivas() {
        worker.activarSubastasProgramadas();

        assertThat(subastaRepository.findById(SUBASTA_ACTIVA).orElseThrow().getEstado())
                .isEqualTo(EstadoSubasta.ACTIVA);
    }

    private List<AuditoriaLog> auditoriaDe(Long subastaId) {
        return auditoriaLogRepository.findByEntidadAndEntidadId(TipoEntidadAuditoria.SUBASTA, subastaId);
    }
}
