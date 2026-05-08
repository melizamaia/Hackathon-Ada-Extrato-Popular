package com.extratoPopular.interfaces.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @NotBlank
        String pergunta
) {
}