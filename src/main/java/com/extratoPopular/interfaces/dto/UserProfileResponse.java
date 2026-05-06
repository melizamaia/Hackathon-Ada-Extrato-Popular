package com.extratoPopular.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Dados do perfil do usuário autenticado")
public record UserProfileResponse(
        @Schema(description = "ID do usuário", example = "1")
        Long id,

        @Schema(description = "Nome completo do usuário", example = "João Souza")
        String nome,

        @Schema(description = "E-mail do usuário", example = "joao.souza@email.com")
        String email,

        @Schema(description = "Renda mensal do usuário", example = "3500.50")
        BigDecimal rendaMensal
) {
}
