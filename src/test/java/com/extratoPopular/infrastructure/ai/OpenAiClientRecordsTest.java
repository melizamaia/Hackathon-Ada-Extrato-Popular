package com.extratoPopular.infrastructure.ai;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OpenAiClientRecordsTest {

    @Test
    void message_deve_expor_role_e_content() {
        OpenAiClient.Message msg = new OpenAiClient.Message("user", "olá");
        assertEquals("user", msg.role());
        assertEquals("olá", msg.content());
    }

    @Test
    void openAiRequest_deve_expor_campos() {
        OpenAiClient.Message msg = new OpenAiClient.Message("user", "prompt");
        OpenAiClient.OpenAiRequest req = new OpenAiClient.OpenAiRequest("gpt-4o-mini", List.of(msg), 0.3);
        assertEquals("gpt-4o-mini", req.model());
        assertEquals(1, req.messages().size());
        assertEquals(0.3, req.temperature());
    }

    @Test
    void openAiResponse_deve_expor_choices() {
        OpenAiClient.Message msg = new OpenAiClient.Message("assistant", "resposta");
        OpenAiClient.Choice choice = new OpenAiClient.Choice(msg);
        OpenAiClient.OpenAiResponse response = new OpenAiClient.OpenAiResponse(List.of(choice));
        assertEquals(1, response.choices().size());
        assertEquals("resposta", response.choices().getFirst().message().content());
    }

    @Test
    void choice_deve_expor_message() {
        OpenAiClient.Message msg = new OpenAiClient.Message("assistant", "ok");
        OpenAiClient.Choice choice = new OpenAiClient.Choice(msg);
        assertSame(msg, choice.message());
    }
}
