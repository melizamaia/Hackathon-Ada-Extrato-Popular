package com.extratoPopular.application.service;

import com.extratoPopular.application.service.rag.ContextoFinanceiroService;
import com.extratoPopular.application.service.rag.PromptFinanceiroService;
import com.extratoPopular.infrastructure.ai.OpenAiClient;
import com.extratoPopular.interfaces.dto.ChatRequest;
import com.extratoPopular.interfaces.dto.ChatResponse;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private final ContextoFinanceiroService contextoService;
    private final PromptFinanceiroService promptService;
    private final OpenAiClient openAiClient;

    public ChatService(
            ContextoFinanceiroService contextoService,
            PromptFinanceiroService promptService,
            OpenAiClient openAiClient
    ) {
        this.contextoService = contextoService;
        this.promptService = promptService;
        this.openAiClient = openAiClient;
    }

    public ChatResponse conversar(ChatRequest request) {

        String contexto = contextoService.gerarContextoFinanceiro();

        String prompt = promptService.construirPrompt(
                contexto,
                request.pergunta()
        );

        String resposta = openAiClient.gerarResposta(prompt);

        return new ChatResponse(resposta);
    }
}