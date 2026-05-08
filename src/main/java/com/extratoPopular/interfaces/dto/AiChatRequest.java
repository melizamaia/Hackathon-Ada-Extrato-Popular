package com.extratoPopular.interfaces.dto;

import jakarta.validation.constraints.NotBlank;

public record AiChatRequest(
        @NotBlank(message = "A mensagem não pode estar vazia")
        String message
) {
}
