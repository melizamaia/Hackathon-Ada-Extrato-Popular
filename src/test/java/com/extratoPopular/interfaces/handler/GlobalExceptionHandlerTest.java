package com.extratoPopular.interfaces.handler;

import com.extratoPopular.domain.exception.*;
import com.extratoPopular.interfaces.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void deve_tratar_email_ja_cadastrado() {
        ErrorResponse resp = handler.handleEmailJaCadastradoException(new EmailJaCadastradoException("email"));
        assertEquals("EMAIL_JA_CADASTRADO", resp.erro());
        assertNull(resp.detalhes());
    }

    @Test
    void deve_tratar_credenciais_invalidas() {
        ErrorResponse resp = handler.handleCredenciaisInvalidasException(new CredenciaisInvalidasException("inv"));
        assertEquals("CREDENCIAIS_INVALIDAS", resp.erro());
    }

    @Test
    void deve_tratar_usuario_nao_autenticado() {
        ErrorResponse resp = handler.handleUsuarioNaoAutenticadoException(new UsuarioNaoAutenticadoException("nao auth"));
        assertEquals("NAO_AUTENTICADO", resp.erro());
    }

    @Test
    void deve_tratar_formato_invalido() {
        ErrorResponse resp = handler.handleFormatoArquivoInvalidoException(new FormatoArquivoInvalidoException("fmt"));
        assertEquals("FORMATO_INVALIDO", resp.erro());
    }

    @Test
    void deve_tratar_arquivo_vazio() {
        ErrorResponse resp = handler.handleArquivoVazioException(new ArquivoVazioException("vazio"));
        assertEquals("ARQUIVO_VAZIO", resp.erro());
    }

    @Test
    void deve_tratar_transacao_duplicada() {
        ErrorResponse resp = handler.handleTransacaoDuplicadaException(new TransacaoDuplicadaException("dup"));
        assertEquals("TRANSACAO_DUPLICADA", resp.erro());
    }

    @Test
    void deve_tratar_orcamento_nao_encontrado() {
        ErrorResponse resp = handler.handleOrcamentoNaoEncontradoException(new OrcamentoNaoEncontradoException("not found"));
        assertEquals("ORCAMENTO_NAO_ENCONTRADO", resp.erro());
    }

    @Test
    void deve_tratar_orcamento_duplicado_com_mensagem() {
        ErrorResponse resp = handler.handleOrcamentoDuplicadoException(new OrcamentoDuplicadoException("ja existe"));
        assertEquals("ORCAMENTO_DUPLICADO", resp.erro());
        assertNotNull(resp.detalhes());
        assertTrue(resp.detalhes().contains("ja existe"));
    }

    @Test
    void deve_tratar_illegal_argument_com_mensagem() {
        ErrorResponse resp = handler.handleIllegalArgumentException(new IllegalArgumentException("arg ruim"));
        assertEquals("ARGUMENTO_INVALIDO", resp.erro());
        assertTrue(resp.detalhes().contains("arg ruim"));
    }

    @Test
    void deve_tratar_ai_integration_exception() {
        ErrorResponse resp = handler.handleAiIntegrationException(new AiIntegrationException("falha ai"));
        assertEquals("AI_INDISPONIVEL", resp.erro());
        assertFalse(resp.detalhes().isEmpty());
    }

    @Test
    void deve_tratar_generic_exception() {
        ErrorResponse resp = handler.handleGenericException(new RuntimeException("generico"));
        assertEquals("ERRO_INTERNO", resp.erro());
    }

    @Test
    void deve_tratar_validation_com_field_error() throws NoSuchMethodException {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "obj");
        bindingResult.addError(new FieldError("obj", "email", "não pode ser nulo"));

        Method method = Object.class.getMethod("toString");
        MethodParameter param = new MethodParameter(method, -1);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(param, bindingResult);

        ErrorResponse resp = handler.handleValidationExceptions(ex);
        assertEquals("VALIDACAO_FALHOU", resp.erro());
        assertNotNull(resp.detalhes());
        assertTrue(resp.detalhes().stream().anyMatch(d -> d.contains("email")));
    }

    @Test
    void deve_tratar_validation_com_global_error() throws NoSuchMethodException {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "obj");
        bindingResult.reject("global.error", "erro global");

        Method method = Object.class.getMethod("toString");
        MethodParameter param = new MethodParameter(method, -1);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(param, bindingResult);

        ErrorResponse resp = handler.handleValidationExceptions(ex);
        assertEquals("VALIDACAO_FALHOU", resp.erro());
        assertTrue(resp.detalhes().contains("erro global"));
    }
}
