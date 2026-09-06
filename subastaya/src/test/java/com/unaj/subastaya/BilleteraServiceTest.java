package com.unaj.subastaya;

import com.unaj.subastaya.dto.AuditoriaLogResponse;
import com.unaj.subastaya.dto.BilleteraResponse;
import com.unaj.subastaya.model.AccionAuditoria;
import com.unaj.subastaya.model.AuditoriaLog;
import com.unaj.subastaya.model.TipoEntidadAuditoria;
import com.unaj.subastaya.model.TipoMovimiento;
import com.unaj.subastaya.model.TransaccionLedger;
import com.unaj.subastaya.repository.AuditoriaLogRepository;
import com.unaj.subastaya.repository.BilleteraRepository;
import com.unaj.subastaya.repository.TransaccionLedgerRepository;
import com.unaj.subastaya.service.AuditoriaLogService;
import com.unaj.subastaya.service.BilleteraService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest(properties = "subastaya.worker.initial-delay-ms=600000")
@Sql(scripts = "/sql/reset-seed.sql")
@Transactional
class BilleteraServiceTest {

    private static final long USUARIO_VENDEDOR = 1L;
    private static final BigDecimal MONTO_DEPOSITO = new BigDecimal("15000.00");

    @Autowired
    private BilleteraService billeteraService;

    @Autowired
    private AuditoriaLogService auditoriaLogService;

    @Autowired
    private BilleteraRepository billeteraRepository;

    @Autowired
    private TransaccionLedgerRepository transaccionLedgerRepository;

    @Autowired
    private AuditoriaLogRepository auditoriaLogRepository;

    @Test
    void depositoEscribeLedgerYAcreditacionManual() {
        Long billeteraId = billeteraRepository.findByUsuarioId(USUARIO_VENDEDOR).orElseThrow().getId();

        BilleteraResponse respuesta = billeteraService.depositar(USUARIO_VENDEDOR, MONTO_DEPOSITO);

        assertThat(respuesta.saldoTotal()).isEqualByComparingTo(MONTO_DEPOSITO);
        assertThat(respuesta.saldoDisponible()).isEqualByComparingTo(MONTO_DEPOSITO);

        assertThat(transaccionLedgerRepository.findByBilleteraIdOrderByFechaDesc(billeteraId))
                .extracting(TransaccionLedger::getTipo, TransaccionLedger::getMonto)
                .contains(tuple(TipoMovimiento.DEPOSITO, MONTO_DEPOSITO));

        List<AuditoriaLog> auditoria = auditoriaLogRepository
                .findByEntidadAndEntidadId(TipoEntidadAuditoria.BILLETERA, billeteraId);
        assertThat(auditoria).extracting(AuditoriaLog::getAccion)
                .containsExactly(AccionAuditoria.ACREDITACION_MANUAL);
        assertThat(auditoria).allSatisfy(registro -> {
            assertThat(registro.getUsuario().getId()).isEqualTo(USUARIO_VENDEDOR);
            assertThat(registro.getDetalleJson()).contains("15000");
        });
    }

    @Test
    void listarAuditoriaDevuelveLosEventosDeLaEntidadOrdenados() {
        Long billeteraId = billeteraRepository.findByUsuarioId(USUARIO_VENDEDOR).orElseThrow().getId();
        billeteraService.depositar(USUARIO_VENDEDOR, MONTO_DEPOSITO);

        List<AuditoriaLogResponse> logs = auditoriaLogService.listar(TipoEntidadAuditoria.BILLETERA, billeteraId);

        assertThat(logs).extracting(AuditoriaLogResponse::accion, AuditoriaLogResponse::entidadId,
                        AuditoriaLogResponse::usuarioId)
                .containsExactly(tuple(AccionAuditoria.ACREDITACION_MANUAL, billeteraId, USUARIO_VENDEDOR));
    }
}
