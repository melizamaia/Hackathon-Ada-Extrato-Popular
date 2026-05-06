package com.extratoPopular.domain.exception;

public class TransacaoDuplicadaException extends RuntimeException {
    public TransacaoDuplicadaException(String message) {
        super(message, null, true, false);
    }
}
