package com.extratoPopular.infrastructure.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class OpenAiClient {

    private final RestClient restClient;
    private final String apiKey;

    public OpenAiClient(@Value("${openai.api-key}") String apiKey) {
        this.apiKey = apiKey;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .build();
    }

    public String gerarResposta(String prompt) {

        OpenAiRequest request = new OpenAiRequest(
                "gpt-4o-mini",
                List.of(
                        new Message("system", "Você é um assistente financeiro especializado em análise de gastos."),
                        new Message("user", prompt)
                ),
                0.3
        );

        OpenAiResponse response = restClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(request)
                .retrieve()
                .body(OpenAiResponse.class);

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            return "Não foi possível gerar uma resposta da IA.";
        }

        return response.choices().getFirst().message().content();
    }

    public record OpenAiRequest(
            String model,
            List<Message> messages,
            Double temperature
    ) {}

    public record Message(
            String role,
            String content
    ) {}

    public record OpenAiResponse(
            List<Choice> choices
    ) {}

    public record Choice(
            Message message
    ) {}
}