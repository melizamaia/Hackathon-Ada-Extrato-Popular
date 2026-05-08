package com.extratoPopular.domain.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransacaoDuplicadaExceptionTest {

    @Test
    void deve_preservar_mensagem() {
        TransacaoDuplicadaException ex = new TransacaoDuplicadaException("hash duplicado");
        assertEquals("hash duplicado", ex.getMessage());
    }

    @Test
    void deve_ser_subclasse_de_RuntimeException() {
        assertInstanceOf(RuntimeException.class, new TransacaoDuplicadaException("x"));
    }

    @Test
    void deve_ter_stack_trace_suprimido() {
        TransacaoDuplicadaException ex = new TransacaoDuplicadaException("x");
        assertEquals(0, ex.getStackTrace().length);
    }

    @Test
    void deve_ter_cause_nula() {
        TransacaoDuplicadaException ex = new TransacaoDuplicadaException("x");
        assertNull(ex.getCause());
    }
}
