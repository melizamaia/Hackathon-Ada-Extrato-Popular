package com.extratoPopular.infrastructure.ai;

import com.extratoPopular.domain.exception.AiIndisponivelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Component
public class OpenAiClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);

    private final RestClient restClient;
    private final String apiKey;

    public OpenAiClient(
            @Value("${openai.api-key}") String apiKey,
            @Value("${openai.timeout-ms:10000}") int timeoutMs) {
        this.apiKey = apiKey;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(timeoutMs);

        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .requestFactory(factory)
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

        try {
            OpenAiResponse response = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(request)
                    .retrieve()
                    .body(OpenAiResponse.class);

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                throw new AiIndisponivelException("IA retornou resposta vazia.");
            }

            return response.choices().getFirst().message().content();

        } catch (ResourceAccessException e) {
            log.error("Timeout na comunicação com a OpenAI: {}", e.getMessage());
            throw new AiIndisponivelException("A IA demorou demais para responder. Tente novamente em instantes.");
        } catch (RestClientResponseException e) {
            log.error("Erro HTTP da OpenAI: status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new AiIndisponivelException("Serviço de IA indisponível (HTTP " + e.getStatusCode() + ").");
        } catch (AiIndisponivelException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro inesperado ao chamar OpenAI: {}", e.getMessage());
            throw new AiIndisponivelException("IA temporariamente indisponível. Tente novamente.");
        }
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