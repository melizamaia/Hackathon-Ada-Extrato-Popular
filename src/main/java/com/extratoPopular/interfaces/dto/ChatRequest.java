package com.extratoPopular.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;

public record ChatRequest(
        @NotBlank
        String pergunta,

        @Schema(hidden = true)
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @Null(message = "userId must not be provided")
        Long userId
) {
}