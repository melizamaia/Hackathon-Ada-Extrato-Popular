package com.extratoPopular.application.service;

import com.extratoPopular.application.service.rag.ContextoFinanceiroService;
import com.extratoPopular.application.service.rag.PromptRelatorioService;
import com.extratoPopular.infrastructure.ai.OpenAiClient;
import com.extratoPopular.interfaces.dto.RelatorioResponse;
import org.springframework.stereotype.Service;

@Service
public class RelatorioService {

    private final ContextoFinanceiroService contextoService;
    private final PromptRelatorioService promptService;
    private final OpenAiClient openAiClient;

    public RelatorioService(
            ContextoFinanceiroService contextoService,
            PromptRelatorioService promptService,
            OpenAiClient openAiClient
    ) {
        this.contextoService = contextoService;
        this.promptService = promptService;
        this.openAiClient = openAiClient;
    }

    public RelatorioResponse gerarRelatorio(Long userId) {

        String contexto = contextoService.gerarContextoFinanceiro(userId);

        String prompt = promptService.construirPrompt(contexto);

        String resposta = openAiClient.gerarResposta(prompt);

        return new RelatorioResponse(resposta);
    }
}