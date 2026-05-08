package com.extratoPopular.application.service;

import com.extratoPopular.application.service.rag.ContextoFinanceiroService;
import com.extratoPopular.application.service.rag.PromptRelatorioService;
import com.extratoPopular.infrastructure.ai.OpenAiClient;
import com.extratoPopular.interfaces.dto.RelatorioResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RelatorioServiceTest {

    @Mock private ContextoFinanceiroService contextoService;
    @Mock private PromptRelatorioService    promptService;
    @Mock private OpenAiClient              openAiClient;

    @InjectMocks
    private RelatorioService relatorioService;

    @Test
    void deve_retornar_relatorio_gerado_pela_ia() {
        when(contextoService.gerarContextoFinanceiro(1L)).thenReturn("CONTEXTO");
        when(promptService.construirPrompt("CONTEXTO")).thenReturn("PROMPT RELATORIO");
        when(openAiClient.gerarResposta("PROMPT RELATORIO")).thenReturn("Relatório completo.");

        RelatorioResponse response = relatorioService.gerarRelatorio(1L);

        assertNotNull(response);
        assertEquals("Relatório completo.", response.relatorio());
    }

    @Test
    void deve_passar_contexto_para_o_promptService() {
        when(contextoService.gerarContextoFinanceiro(1L)).thenReturn("CTX");
        when(promptService.construirPrompt("CTX")).thenReturn("p");
        when(openAiClient.gerarResposta(any())).thenReturn("r");

        relatorioService.gerarRelatorio(1L);

        verify(promptService).construirPrompt("CTX");
    }

    @Test
    void deve_enviar_prompt_ao_openai_client() {
        when(contextoService.gerarContextoFinanceiro(1L)).thenReturn("ctx");
        when(promptService.construirPrompt(any())).thenReturn("PROMPT FINAL");
        when(openAiClient.gerarResposta("PROMPT FINAL")).thenReturn("ok");

        relatorioService.gerarRelatorio(1L);

        verify(openAiClient).gerarResposta("PROMPT FINAL");
    }

    @Test
    void deve_chamar_contexto_service_uma_vez() {
        when(contextoService.gerarContextoFinanceiro(1L)).thenReturn("ctx");
        when(promptService.construirPrompt(any())).thenReturn("p");
        when(openAiClient.gerarResposta(any())).thenReturn("r");

        relatorioService.gerarRelatorio(1L);

        verify(contextoService, times(1)).gerarContextoFinanceiro(1L);
    }

    @Test
    void deve_retornar_relatorio_nao_nulo() {
        when(contextoService.gerarContextoFinanceiro(1L)).thenReturn("ctx");
        when(promptService.construirPrompt(any())).thenReturn("p");
        when(openAiClient.gerarResposta(any())).thenReturn("conteúdo do relatório");

        RelatorioResponse response = relatorioService.gerarRelatorio(1L);

        assertNotNull(response);
        assertNotNull(response.relatorio());
        assertFalse(response.relatorio().isBlank());
    }
}
