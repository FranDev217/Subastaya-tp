package com.unaj.subastaya.exception;

import java.math.BigDecimal;

public class SaldoInsuficienteException extends RuntimeException {

    public SaldoInsuficienteException(Long usuarioId, BigDecimal montoSolicitado, BigDecimal saldoDisponible) {
        super("El usuario " + usuarioId + " no tiene saldo suficiente: solicitado " + montoSolicitado
                + ", disponible " + saldoDisponible);
    }
}
