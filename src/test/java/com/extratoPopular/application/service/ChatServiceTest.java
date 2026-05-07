package com.extratoPopular.application.service;

import com.extratoPopular.application.service.rag.ContextoFinanceiroService;
import com.extratoPopular.application.service.rag.PromptFinanceiroService;
import com.extratoPopular.infrastructure.ai.OpenAiClient;
import com.extratoPopular.interfaces.dto.ChatRequest;
import com.extratoPopular.interfaces.dto.ChatResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock private ContextoFinanceiroService contextoService;
    @Mock private PromptFinanceiroService   promptService;
    @Mock private OpenAiClient              openAiClient;

    @InjectMocks
    private ChatService chatService;

    @Test
    void deve_retornar_resposta_da_ia() {
        when(contextoService.gerarContextoFinanceiro()).thenReturn("CONTEXTO FINANCEIRO");
        when(promptService.construirPrompt(any(), any())).thenReturn("PROMPT COMPLETO");
        when(openAiClient.gerarResposta("PROMPT COMPLETO")).thenReturn("Resposta da IA");

        ChatResponse response = chatService.conversar(new ChatRequest("Qual meu maior gasto?"));

        assertNotNull(response);
        assertEquals("Resposta da IA", response.resposta());
    }

    @Test
    void deve_passar_contexto_e_pergunta_para_o_promptService() {
        when(contextoService.gerarContextoFinanceiro()).thenReturn("CTX");
        when(promptService.construirPrompt("CTX", "pergunta?")).thenReturn("prompt");
        when(openAiClient.gerarResposta(any())).thenReturn("ok");

        chatService.conversar(new ChatRequest("pergunta?"));

        verify(promptService).construirPrompt(eq("CTX"), eq("pergunta?"));
    }

    @Test
    void deve_enviar_prompt_completo_ao_openai_client() {
        when(contextoService.gerarContextoFinanceiro()).thenReturn("ctx");
        when(promptService.construirPrompt(any(), any())).thenReturn("PROMPT FINAL");
        when(openAiClient.gerarResposta("PROMPT FINAL")).thenReturn("ok");

        chatService.conversar(new ChatRequest("teste"));

        verify(openAiClient).gerarResposta("PROMPT FINAL");
    }

    @Test
    void deve_chamar_contexto_service_uma_vez_por_requisicao() {
        when(contextoService.gerarContextoFinanceiro()).thenReturn("ctx");
        when(promptService.construirPrompt(any(), any())).thenReturn("p");
        when(openAiClient.gerarResposta(any())).thenReturn("r");

        chatService.conversar(new ChatRequest("teste"));

        verify(contextoService, times(1)).gerarContextoFinanceiro();
    }

    @Test
    void deve_retornar_resposta_nao_nula() {
        when(contextoService.gerarContextoFinanceiro()).thenReturn("ctx");
        when(promptService.construirPrompt(any(), any())).thenReturn("p");
        when(openAiClient.gerarResposta(any())).thenReturn("resposta válida");

        ChatResponse response = chatService.conversar(new ChatRequest("qualquer pergunta"));

        assertNotNull(response);
        assertNotNull(response.resposta());
        assertFalse(response.resposta().isBlank());
    }
}
