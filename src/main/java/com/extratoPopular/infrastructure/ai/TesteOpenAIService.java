package com.extratoPopular.infrastructure.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class TesteOpenAIService {

    private final ChatClient chatClient;

    public TesteOpenAIService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String testar() {

        return chatClient.prompt()
                .user("Responda apenas: OpenAI funcionando")
                .call()
                .content();
    }
}