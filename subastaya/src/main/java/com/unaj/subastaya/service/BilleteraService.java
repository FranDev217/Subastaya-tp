package com.unaj.subastaya.service;

import com.unaj.subastaya.dto.BilleteraResponse;
import com.unaj.subastaya.exception.RecursoNoEncontradoException;
import com.unaj.subastaya.exception.SaldoInsuficienteException;
import com.unaj.subastaya.model.Billetera;
import com.unaj.subastaya.model.Subasta;
import com.unaj.subastaya.model.TipoMovimiento;
import com.unaj.subastaya.model.TransaccionLedger;
import com.unaj.subastaya.repository.BilleteraRepository;
import com.unaj.subastaya.repository.TransaccionLedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class BilleteraService {

    private final BilleteraRepository billeteraRepository;
    private final TransaccionLedgerRepository transaccionLedgerRepository;

    @Transactional(readOnly = true)
    public BilleteraResponse obtenerSaldo(Long usuarioId) {
        return toResponse(obtenerBilletera(usuarioId));
    }

    @Transactional
    public BilleteraResponse depositar(Long usuarioId, BigDecimal monto) {
        Billetera billetera = obtenerBilletera(usuarioId);
        billetera.setSaldoTotal(billetera.getSaldoTotal().add(monto));
        billetera.setSaldoDisponible(billetera.getSaldoDisponible().add(monto));
        registrarMovimiento(billetera, TipoMovimiento.DEPOSITO, monto, null);
        return toResponse(billetera);
    }

    @Transactional
    public void congelarSaldo(Long usuarioId, BigDecimal monto, Subasta subasta) {
        Billetera billetera = obtenerBilletera(usuarioId);
        if (billetera.getSaldoDisponible().compareTo(monto) < 0) {
            throw new SaldoInsuficienteException(usuarioId, monto, billetera.getSaldoDisponible());
        }
        billetera.setSaldoDisponible(billetera.getSaldoDisponible().subtract(monto));
        billetera.setSaldoRetenido(billetera.getSaldoRetenido().add(monto));
        registrarMovimiento(billetera, TipoMovimiento.RETENCION, monto, subasta);
        // flush inmediato: si hay conflicto de version, que salte aca y no al commit de la transaccion del llamador
        billeteraRepository.flush();
    }

    @Transactional
    public void liberarSaldo(Long usuarioId, BigDecimal monto, Subasta subasta) {
        Billetera billetera = obtenerBilletera(usuarioId);
        billetera.setSaldoRetenido(billetera.getSaldoRetenido().subtract(monto));
        billetera.setSaldoDisponible(billetera.getSaldoDisponible().add(monto));
        registrarMovimiento(billetera, TipoMovimiento.LIBERACION, monto, subasta);
    }

    // Debita el saldo que el comprador tenía congelado por su puja ganadora.
    // El disponible no cambia: esos fondos ya se habían descontado al congelar.
    @Transactional
    public void pagar(Long usuarioId, BigDecimal monto, Subasta subasta) {
        Billetera billetera = obtenerBilletera(usuarioId);
        if (billetera.getSaldoRetenido().compareTo(monto) < 0) {
            throw new SaldoInsuficienteException(usuarioId, monto, billetera.getSaldoRetenido());
        }
        billetera.setSaldoRetenido(billetera.getSaldoRetenido().subtract(monto));
        billetera.setSaldoTotal(billetera.getSaldoTotal().subtract(monto));
        registrarMovimiento(billetera, TipoMovimiento.PAGO, monto, subasta);
    }

    @Transactional
    public void cobrar(Long usuarioId, BigDecimal monto, Subasta subasta) {
        Billetera billetera = obtenerBilletera(usuarioId);
        billetera.setSaldoTotal(billetera.getSaldoTotal().add(monto));
        billetera.setSaldoDisponible(billetera.getSaldoDisponible().add(monto));
        registrarMovimiento(billetera, TipoMovimiento.COBRO, monto, subasta);
    }

    private Billetera obtenerBilletera(Long usuarioId) {
        return billeteraRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Billetera de usuario " + usuarioId + " no encontrada"));
    }

    private void registrarMovimiento(Billetera billetera, TipoMovimiento tipo, BigDecimal monto, Subasta subasta) {
        TransaccionLedger movimiento = TransaccionLedger.builder()
                .billetera(billetera)
                .tipo(tipo)
                .monto(monto)
                .subasta(subasta)
                .build();
        transaccionLedgerRepository.save(movimiento);
    }

    private BilleteraResponse toResponse(Billetera billetera) {
        return new BilleteraResponse(
                billetera.getId(),
                billetera.getUsuario().getId(),
                billetera.getSaldoTotal(),
                billetera.getSaldoRetenido(),
                billetera.getSaldoDisponible()
        );
    }
}
