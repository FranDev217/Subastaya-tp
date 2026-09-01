package com.unaj.subastaya;

import com.unaj.subastaya.dto.SubastaEvento;
import com.unaj.subastaya.dto.TipoEvento;
import com.unaj.subastaya.model.AuditoriaLog;
import com.unaj.subastaya.model.Billetera;
import com.unaj.subastaya.model.EstadoSubasta;
import com.unaj.subastaya.model.TipoEntidadAuditoria;
import com.unaj.subastaya.model.TipoMovimiento;
import com.unaj.subastaya.model.TransaccionLedger;
import com.unaj.subastaya.repository.AuditoriaLogRepository;
import com.unaj.subastaya.repository.BilleteraRepository;
import com.unaj.subastaya.repository.SubastaRepository;
import com.unaj.subastaya.repository.TransaccionLedgerRepository;
import com.unaj.subastaya.service.SubastaLiquidacionWorker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
class SubastaLiquidacionWorkerTest {

    private static final long SUBASTA_NOTEBOOK = 1L;
    private static final long SUBASTA_FIGURA = 2L;
    private static final long SUBASTA_CAMPERA = 3L;
    private static final long SUBASTA_BICICLETA = 4L;
    private static final long SUBASTA_TECLADO = 5L;

    private static final long USUARIO_VENDEDOR = 1L;
    private static final long USUARIO_COMPRADOR_1 = 2L;
    private static final long USUARIO_COMPRADOR_2 = 3L;

    @LocalServerPort
    private int port;

    @Autowired
    private SubastaLiquidacionWorker worker;

    @Autowired
    private SubastaRepository subastaRepository;

    @Autowired
    private BilleteraRepository billeteraRepository;

    @Autowired
    private TransaccionLedgerRepository transaccionLedgerRepository;

    @Autowired
    private AuditoriaLogRepository auditoriaLogRepository;

    @Test
    void cierraTodasLasVencidasYNoTocaLasProgramadas() {
        worker.cerrarSubastasVencidas();

        assertThat(estadoDe(SUBASTA_NOTEBOOK)).isEqualTo(EstadoSubasta.FINALIZADA);
        assertThat(estadoDe(SUBASTA_FIGURA)).isEqualTo(EstadoSubasta.DESIERTA);
        assertThat(estadoDe(SUBASTA_BICICLETA)).isEqualTo(EstadoSubasta.FINALIZADA);
        assertThat(estadoDe(SUBASTA_TECLADO)).isEqualTo(EstadoSubasta.DESIERTA);
        assertThat(estadoDe(SUBASTA_CAMPERA)).isEqualTo(EstadoSubasta.PROGRAMADA);
    }

    @Test
    void liquidaLasVentasTransfiriendoElSaldoRetenidoAlVendedor() {
        worker.cerrarSubastasVencidas();

        Billetera comprador1 = billeteraDe(USUARIO_COMPRADOR_1);
        assertThat(comprador1.getSaldoTotal()).isEqualByComparingTo("105000.00");
        assertThat(comprador1.getSaldoRetenido()).isEqualByComparingTo("0.00");
        assertThat(comprador1.getSaldoDisponible()).isEqualByComparingTo("105000.00");

        Billetera comprador2 = billeteraDe(USUARIO_COMPRADOR_2);
        assertThat(comprador2.getSaldoTotal()).isEqualByComparingTo("168000.00");
        assertThat(comprador2.getSaldoRetenido()).isEqualByComparingTo("0.00");
        assertThat(comprador2.getSaldoDisponible()).isEqualByComparingTo("168000.00");

        Billetera vendedor = billeteraDe(USUARIO_VENDEDOR);
        assertThat(vendedor.getSaldoTotal()).isEqualByComparingTo("77000.00");
        assertThat(vendedor.getSaldoDisponible()).isEqualByComparingTo("77000.00");
    }

    @Test
    void escribeUnPagoYUnCobroPorCadaVentaEnElLedger() {
        worker.cerrarSubastasVencidas();

        assertThat(movimientosDe(USUARIO_COMPRADOR_1, TipoMovimiento.PAGO))
                .extracting(TransaccionLedger::getMonto)
                .containsExactly(new BigDecimal("45000.00"));
        assertThat(movimientosDe(USUARIO_COMPRADOR_2, TipoMovimiento.PAGO))
                .extracting(TransaccionLedger::getMonto)
                .containsExactly(new BigDecimal("32000.00"));
        assertThat(movimientosDe(USUARIO_VENDEDOR, TipoMovimiento.COBRO))
                .extracting(TransaccionLedger::getMonto)
                .containsExactlyInAnyOrder(
                        new BigDecimal("45000.00"),
                        new BigDecimal("32000.00"));
    }

    @Test
    void registraElCierreDeCadaSubastaEnElAuditLogSinUsuario() {
        worker.cerrarSubastasVencidas();

        List<AuditoriaLog> auditoria = auditoriaLogRepository
                .findByEntidadAndEntidadId(TipoEntidadAuditoria.SUBASTA, SUBASTA_BICICLETA);

        assertThat(auditoria).extracting(AuditoriaLog::getAccion).containsExactly("CIERRE_WORKER");
        assertThat(auditoria).allSatisfy(registro -> assertThat(registro.getUsuario()).isNull());

        assertThat(auditoriaLogRepository.findByEntidadAndEntidadId(TipoEntidadAuditoria.SUBASTA, SUBASTA_TECLADO))
                .extracting(AuditoriaLog::getAccion)
                .containsExactly("CIERRE_WORKER");
    }

    @Test
    void difundeFinalizadaPorWebSocket() throws Exception {
        BlockingQueue<SubastaEvento> eventos = new LinkedBlockingQueue<>();
        StompSession session = conectar();
        session.subscribe("/topic/subastas/" + SUBASTA_BICICLETA, frameHandler(eventos));

        worker.cerrarSubastasVencidas();

        SubastaEvento recibido = eventos.poll(5, TimeUnit.SECONDS);
        session.disconnect();

        assertThat(recibido).isNotNull();
        assertThat(recibido.tipo()).isEqualTo(TipoEvento.FINALIZADA);
        assertThat(recibido.estado()).isEqualTo(EstadoSubasta.FINALIZADA);
        assertThat(recibido.montoActual()).isEqualByComparingTo("32000.00");
        assertThat(recibido.ultimaPuja()).isNotNull();
    }

    @Test
    void difundeDesiertaPorWebSocket() throws Exception {
        BlockingQueue<SubastaEvento> eventos = new LinkedBlockingQueue<>();
        StompSession session = conectar();
        session.subscribe("/topic/subastas/" + SUBASTA_TECLADO, frameHandler(eventos));

        worker.cerrarSubastasVencidas();

        SubastaEvento recibido = eventos.poll(5, TimeUnit.SECONDS);
        session.disconnect();

        assertThat(recibido).isNotNull();
        assertThat(recibido.tipo()).isEqualTo(TipoEvento.DESIERTA);
        assertThat(recibido.estado()).isEqualTo(EstadoSubasta.DESIERTA);
        assertThat(recibido.montoActual()).isEqualByComparingTo("10000.00");
    }

    private EstadoSubasta estadoDe(Long subastaId) {
        return subastaRepository.findById(subastaId).orElseThrow().getEstado();
    }

    private Billetera billeteraDe(Long usuarioId) {
        return billeteraRepository.findByUsuarioId(usuarioId).orElseThrow();
    }

    private List<TransaccionLedger> movimientosDe(Long usuarioId, TipoMovimiento tipo) {
        return transaccionLedgerRepository
                .findByBilleteraIdOrderByFechaDesc(billeteraDe(usuarioId).getId()).stream()
                .filter(movimiento -> movimiento.getTipo() == tipo)
                .toList();
    }

    private StompSession conectar() throws Exception {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new JacksonJsonMessageConverter());
        return client.connectAsync("ws://localhost:" + port + "/ws", new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);
    }

    private StompFrameHandler frameHandler(BlockingQueue<SubastaEvento> eventos) {
        return new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return SubastaEvento.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                eventos.add((SubastaEvento) payload);
            }
        };
    }
}
