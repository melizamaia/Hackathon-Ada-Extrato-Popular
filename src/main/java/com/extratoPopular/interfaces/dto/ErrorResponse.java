package com.extratoPopular.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Resposta padronizada para erros da API")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        @Schema(description = "Código ou tipo do erro", example = "EMAIL_JA_CADASTRADO")
        String erro,

        @Schema(description = "Lista opcional contendo detalhes da validação", example = "[\"nome: O nome é obrigatório\"]")
        List<String> detalhes
) {
}
