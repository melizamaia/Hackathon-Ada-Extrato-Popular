package com.extratoPopular.domain.exception;

public class ArquivoVazioException extends RuntimeException {
    public ArquivoVazioException(String message) {
        super(message, null, true, false);
    }
}
