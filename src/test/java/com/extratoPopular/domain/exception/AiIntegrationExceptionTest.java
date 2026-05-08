package com.extratoPopular.domain.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AiIntegrationExceptionTest {

    @Test
    void deve_preservar_message() {
        AiIntegrationException ex = new AiIntegrationException("mensagem de erro", null);
        assertEquals("mensagem de erro", ex.getMessage());
    }

    @Test
    void deve_preservar_cause() {
        RuntimeException causa = new RuntimeException("causa original");
        AiIntegrationException ex = new AiIntegrationException("falha", causa);
        assertSame(causa, ex.getCause());
    }

    @Test
    void deve_aceitar_cause_nulo() {
        AiIntegrationException ex = new AiIntegrationException("falha", null);
        assertNull(ex.getCause());
    }

    @Test
    void deve_ser_subclasse_de_RuntimeException() {
        AiIntegrationException ex = new AiIntegrationException("falha", null);
        assertInstanceOf(RuntimeException.class, ex);
    }
}
