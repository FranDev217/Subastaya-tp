package com.unaj.subastaya.exception;

public class SubastaNoActivaException extends RuntimeException {

    public SubastaNoActivaException(Long subastaId) {
        super("La subasta " + subastaId + " no está activa");
    }
}
