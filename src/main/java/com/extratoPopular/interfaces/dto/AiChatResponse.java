package com.extratoPopular.interfaces.dto;

import java.time.LocalDateTime;

public record AiChatResponse(
        String resposta,
        LocalDateTime timestamp
) {
}
