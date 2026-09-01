package com.unaj.subastaya;

import com.unaj.subastaya.exception.SaldoInsuficienteException;
import com.unaj.subastaya.model.Billetera;
import com.unaj.subastaya.model.EstadoSubasta;
import com.unaj.subastaya.model.TipoMovimiento;
import com.unaj.subastaya.model.TransaccionLedger;
import com.unaj.subastaya.repository.BilleteraRepository;
import com.unaj.subastaya.repository.SubastaRepository;
import com.unaj.subastaya.repository.TransaccionLedgerRepository;
import com.unaj.subastaya.service.LiquidacionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest
@Transactional
class LiquidacionServiceTest {

    private static final long SUBASTA_VENCIDA_CON_GANADOR = 4L;
    private static final long SUBASTA_VENCIDA_SIN_PUJAS = 5L;
    private static final long SUBASTA_PROGRAMADA = 3L;

    private static final long USUARIO_VENDEDOR = 1L;
    private static final long USUARIO_COMPRADOR_GANADOR = 3L;

    private static final BigDecimal MONTO_VENTA = new BigDecimal("32000.00");

    @Autowired
    private LiquidacionService liquidacionService;

    @Autowired
    private SubastaRepository subastaRepository;

    @Autowired
    private BilleteraRepository billeteraRepository;

    @Autowired
    private TransaccionLedgerRepository transaccionLedgerRepository;

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

    @Test
    void subastaVencidaConGanadorSeFinalizaYTransfiereElSaldo() {
        liquidacionService.cerrarSubasta(SUBASTA_VENCIDA_CON_GANADOR);

        assertThat(estadoDe(SUBASTA_VENCIDA_CON_GANADOR)).isEqualTo(EstadoSubasta.FINALIZADA);

        Billetera comprador = billeteraDe(USUARIO_COMPRADOR_GANADOR);
        assertThat(comprador.getSaldoTotal()).isEqualByComparingTo("168000");
        assertThat(comprador.getSaldoRetenido()).isEqualByComparingTo("0");
        assertThat(comprador.getSaldoDisponible()).isEqualByComparingTo("168000");

        Billetera vendedor = billeteraDe(USUARIO_VENDEDOR);
        assertThat(vendedor.getSaldoTotal()).isEqualByComparingTo(MONTO_VENTA);
        assertThat(vendedor.getSaldoRetenido()).isEqualByComparingTo("0");
        assertThat(vendedor.getSaldoDisponible()).isEqualByComparingTo(MONTO_VENTA);
    }

    @Test
    void liquidacionEscribePagoYCobroEnElLedger() {
        liquidacionService.cerrarSubasta(SUBASTA_VENCIDA_CON_GANADOR);

        assertThat(transaccionLedgerRepository
                .findByBilleteraIdOrderByFechaDesc(billeteraDe(USUARIO_COMPRADOR_GANADOR).getId()))
                .extracting(TransaccionLedger::getTipo, TransaccionLedger::getMonto)
                .contains(tuple(TipoMovimiento.PAGO, MONTO_VENTA));

        assertThat(transaccionLedgerRepository
                .findByBilleteraIdOrderByFechaDesc(billeteraDe(USUARIO_VENDEDOR).getId()))
                .extracting(TransaccionLedger::getTipo, TransaccionLedger::getMonto)
                .contains(tuple(TipoMovimiento.COBRO, MONTO_VENTA));
    }

    @Test
    void siFallaElDebitoNoSeAcreditaAlVendedorNiSeFinalizaLaSubasta() {
        billeteraDe(USUARIO_COMPRADOR_GANADOR).setSaldoRetenido(BigDecimal.ZERO);

        assertThatThrownBy(() -> liquidacionService.cerrarSubasta(SUBASTA_VENCIDA_CON_GANADOR))
                .isInstanceOf(SaldoInsuficienteException.class);

        assertThat(estadoDe(SUBASTA_VENCIDA_CON_GANADOR)).isEqualTo(EstadoSubasta.ACTIVA);
        assertThat(billeteraDe(USUARIO_VENDEDOR).getSaldoTotal()).isEqualByComparingTo("0");
    }

    private EstadoSubasta estadoDe(Long subastaId) {
        return subastaRepository.findById(subastaId).orElseThrow().getEstado();
    }

    private Billetera billeteraDe(Long usuarioId) {
        return billeteraRepository.findByUsuarioId(usuarioId).orElseThrow();
    }
}
