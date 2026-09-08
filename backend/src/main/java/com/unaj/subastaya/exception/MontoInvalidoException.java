package com.unaj.subastaya.exception;

import java.math.BigDecimal;

public class MontoInvalidoException extends RuntimeException {

    public MontoInvalidoException(BigDecimal montoOfrecido, BigDecimal montoMinimo) {
        super("El monto ofrecido " + montoOfrecido + " debe ser al menos " + montoMinimo);
    }
}
