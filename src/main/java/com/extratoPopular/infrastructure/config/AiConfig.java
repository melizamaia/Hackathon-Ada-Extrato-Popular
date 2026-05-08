package com.extratoPopular.infrastructure.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração central do cliente de IA.
 *
 * Decisão técnica: usamos ChatClient.create(chatModel) — forma canônica do Spring AI 1.0.0.
 * O ChatModel já vem totalmente configurado pelo autoconfigure do Spring AI,
 * que lê model, temperature e max-tokens diretamente do application.properties.
 * Nenhuma configuração extra é necessária aqui para não criar redundância.
 */
@Configuration
public class AiConfig {

    /**
     * Expõe um ChatClient como Bean singleton.
     * O ChatModel é injetado automaticamente pelo Spring AI
     * (bean do tipo OpenAiChatModel, configurado via spring.ai.openai.*).
     */
    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.create(chatModel);
    }
}
