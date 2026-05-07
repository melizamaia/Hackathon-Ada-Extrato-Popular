package com.extratoPopular.domain.exception;

public class FormatoArquivoInvalidoException extends RuntimeException {
    public FormatoArquivoInvalidoException(String message) {
        super(message, null, true, false);
    }
}
