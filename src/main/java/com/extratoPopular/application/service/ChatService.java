package com.extratoPopular.application.service;

import com.extratoPopular.interfaces.dto.ChatRequest;
import com.extratoPopular.interfaces.dto.ChatResponse;
import com.extratoPopular.application.service.rag.ContextoFinanceiroService;
import com.extratoPopular.application.service.rag.PromptFinanceiroService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private final ContextoFinanceiroService contextoService;
    private final PromptFinanceiroService promptService;
    private final ChatClient chatClient;

    public ChatService(
            ContextoFinanceiroService contextoService,
            PromptFinanceiroService promptService,
            ChatClient.Builder builder
    ) {
        this.contextoService = contextoService;
        this.promptService = promptService;
        this.chatClient = builder.build();
    }

    public ChatResponse conversar(ChatRequest request) {

        String contexto =
                contextoService.gerarContextoFinanceiro();

        String prompt =
                promptService.construirPrompt(
                        contexto,
                        request.pergunta()
                );

        String resposta = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        return new ChatResponse(resposta);
    }
}