package com.extratoPopular.interfaces.handler;

import com.extratoPopular.domain.exception.AiIntegrationException;
import com.extratoPopular.domain.exception.ArquivoVazioException;
import com.extratoPopular.domain.exception.CredenciaisInvalidasException;
import com.extratoPopular.domain.exception.EmailJaCadastradoException;
import com.extratoPopular.domain.exception.FormatoArquivoInvalidoException;
import com.extratoPopular.domain.exception.OrcamentoDuplicadoException;
import com.extratoPopular.domain.exception.OrcamentoNaoEncontradoException;
import com.extratoPopular.domain.exception.TransacaoDuplicadaException;
import com.extratoPopular.domain.exception.UsuarioNaoAutenticadoException;
import com.extratoPopular.interfaces.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<String> detalhes = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> {
                    if (error instanceof FieldError fieldError) {
                        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
                    }
                    return error.getDefaultMessage();
                })
                .collect(Collectors.toList());

        return new ErrorResponse("VALIDACAO_FALHOU", detalhes);
    }

    @ExceptionHandler(EmailJaCadastradoException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleEmailJaCadastradoException(EmailJaCadastradoException ex) {
        return new ErrorResponse("EMAIL_JA_CADASTRADO", null);
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleCredenciaisInvalidasException(CredenciaisInvalidasException ex) {
        return new ErrorResponse("CREDENCIAIS_INVALIDAS", null);
    }

    @ExceptionHandler(UsuarioNaoAutenticadoException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleUsuarioNaoAutenticadoException(UsuarioNaoAutenticadoException ex) {
        return new ErrorResponse("NAO_AUTENTICADO", null);
    }

    @ExceptionHandler(FormatoArquivoInvalidoException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleFormatoArquivoInvalidoException(FormatoArquivoInvalidoException ex) {
        return new ErrorResponse("FORMATO_INVALIDO", null);
    }

    @ExceptionHandler(ArquivoVazioException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleArquivoVazioException(ArquivoVazioException ex) {
        return new ErrorResponse("ARQUIVO_VAZIO", null);
    }

    @ExceptionHandler(TransacaoDuplicadaException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleTransacaoDuplicadaException(TransacaoDuplicadaException ex) {
        return new ErrorResponse("TRANSACAO_DUPLICADA", null);
    }

    @ExceptionHandler(OrcamentoNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleOrcamentoNaoEncontradoException(OrcamentoNaoEncontradoException ex) {
        return new ErrorResponse("ORCAMENTO_NAO_ENCONTRADO", null);
    }

    @ExceptionHandler(OrcamentoDuplicadoException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleOrcamentoDuplicadoException(OrcamentoDuplicadoException ex) {
        return new ErrorResponse("ORCAMENTO_DUPLICADO", List.of(ex.getMessage()));
    }

    @ExceptionHandler(AiIntegrationException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorResponse handleAiIntegrationException(AiIntegrationException ex) {
        return new ErrorResponse("AI_INDISPONIVEL", List.of("Serviço de IA temporariamente indisponível."));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGenericException(Exception ex) {
        return new ErrorResponse("ERRO_INTERNO", null);
    }
}
