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
class ReportFinanceServiceTest {

    @Mock
    private FinancialContextService financialContextService;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    private ReportFinanceService reportFinanceService;

    @BeforeEach
    void setUp() {
        reportFinanceService = new ReportFinanceService(financialContextService, chatClient);
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

        String resultado = reportFinanceService.generateReport(1L);

        assertNotNull(resultado);
        assertFalse(resultado.isBlank());
        verify(chatClient, never()).prompt();
    }

    // -------------------------------------------------------------------------
    // Com dados
    // -------------------------------------------------------------------------

    @Test
    void deve_retornar_relatorio_gerado_pela_ia() {
        when(financialContextService.buildContext(1L)).thenReturn("CONTEXTO FINANCEIRO");
        mockChatClientFluent("Relatório: saúde financeira Positiva.");

        String resultado = reportFinanceService.generateReport(1L);

        assertEquals("Relatório: saúde financeira Positiva.", resultado);
    }

    @Test
    void deve_incluir_contexto_no_user_message() {
        when(financialContextService.buildContext(1L)).thenReturn("CONTEXTO RICO");
        mockChatClientFluent("ok");

        reportFinanceService.generateReport(1L);

        String expectedMsg = "Gere o relatório financeiro com base no contexto abaixo:\n\n" + "CONTEXTO RICO";
        verify(requestSpec).user(eq(expectedMsg));
    }

    @Test
    void deve_iniciar_user_message_com_instrucao_de_relatorio() {
        when(financialContextService.buildContext(1L)).thenReturn("CONTEXTO");
        mockChatClientFluent("ok");

        reportFinanceService.generateReport(1L);

        String expectedMsg = "Gere o relatório financeiro com base no contexto abaixo:\n\n" + "CONTEXTO";
        verify(requestSpec).user(eq(expectedMsg));
    }

    @Test
    void deve_chamar_buildContext_com_userId_correto() {
        when(financialContextService.buildContext(77L)).thenReturn("CONTEXTO");
        mockChatClientFluent("relatorio");

        reportFinanceService.generateReport(77L);

        verify(financialContextService).buildContext(77L);
    }

    @Test
    void deve_lancar_AiIntegrationException_quando_llm_falha() {
        when(financialContextService.buildContext(1L)).thenReturn("CONTEXTO");
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(new RuntimeException("timeout"));

        assertThrows(AiIntegrationException.class,
                () -> reportFinanceService.generateReport(1L));
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
                () -> reportFinanceService.generateReport(1L));

        assertSame(causaOriginal, ex.getCause());
    }

    @Test
    void deve_retornar_resultado_nao_nulo() {
        when(financialContextService.buildContext(1L)).thenReturn("CONTEXTO");
        mockChatClientFluent("relatório gerado");

        String resultado = reportFinanceService.generateReport(1L);

        assertNotNull(resultado);
        assertFalse(resultado.isBlank());
    }
}
