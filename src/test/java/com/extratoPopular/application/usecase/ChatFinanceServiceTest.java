package com.extratoPopular.application.usecase;

import com.extratoPopular.application.service.FinancialContextService;
import com.extratoPopular.domain.exception.AiIntegrationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatFinanceServiceTest {

    @Mock
    private FinancialContextService financialContextService;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    private ChatFinanceService chatFinanceService;

    @BeforeEach
    void setUp() {
        chatFinanceService = new ChatFinanceService(financialContextService, chatClient);
    }

    private void mockChatClientFluent(String resposta) {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(resposta);
    }

    // -------------------------------------------------------------------------
    // Sem dados
    // -------------------------------------------------------------------------

    @Test
    void deve_retornar_mensagem_amigavel_sem_chamar_llm_quando_sem_dados() {
        when(financialContextService.buildContext(1L))
                .thenReturn("Nenhuma transação encontrada nos últimos 90 dias.");

        String resultado = chatFinanceService.chat(1L, "Qual meu gasto?");

        assertNotNull(resultado);
        assertFalse(resultado.isBlank());
        verify(chatClient, never()).prompt();
    }

    // -------------------------------------------------------------------------
    // Com dados
    // -------------------------------------------------------------------------

    @Test
    void deve_retornar_resposta_da_ia_quando_ha_dados() {
        when(financialContextService.buildContext(1L)).thenReturn("CONTEXTO FINANCEIRO RICO");
        mockChatClientFluent("Seus gastos estão controlados.");

        String resultado = chatFinanceService.chat(1L, "Como estou?");

        assertEquals("Seus gastos estão controlados.", resultado);
    }

    @Test
    void deve_incluir_mensagem_do_usuario_no_user_message() {
        when(financialContextService.buildContext(1L)).thenReturn("CONTEXTO");
        mockChatClientFluent("ok");

        chatFinanceService.chat(1L, "Qual meu maior gasto?");

        String expectedMsg = "Qual meu maior gasto?" + "\n\nContexto financeiro:\n" + "CONTEXTO";
        verify(requestSpec).user(eq(expectedMsg));
    }

    @Test
    void deve_incluir_contexto_no_user_message() {
        when(financialContextService.buildContext(1L)).thenReturn("CONTEXTO RICO");
        mockChatClientFluent("ok");

        chatFinanceService.chat(1L, "pergunta");

        String expectedMsg = "pergunta" + "\n\nContexto financeiro:\n" + "CONTEXTO RICO";
        verify(requestSpec).user(eq(expectedMsg));
    }

    @Test
    void deve_chamar_buildContext_com_userId_correto() {
        when(financialContextService.buildContext(99L)).thenReturn("CONTEXTO");
        mockChatClientFluent("resposta");

        chatFinanceService.chat(99L, "pergunta");

        verify(financialContextService).buildContext(99L);
    }

    @Test
    void deve_lancar_AiIntegrationException_quando_llm_falha() {
        when(financialContextService.buildContext(1L)).thenReturn("CONTEXTO");
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(new RuntimeException("timeout"));

        assertThrows(AiIntegrationException.class,
                () -> chatFinanceService.chat(1L, "pergunta"));
    }

    @Test
    void deve_preservar_causa_original_no_AiIntegrationException() {
        RuntimeException causaOriginal = new RuntimeException("erro de rede");
        when(financialContextService.buildContext(1L)).thenReturn("CONTEXTO");
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(causaOriginal);

        AiIntegrationException ex = assertThrows(AiIntegrationException.class,
                () -> chatFinanceService.chat(1L, "pergunta"));

        assertSame(causaOriginal, ex.getCause());
    }
}
