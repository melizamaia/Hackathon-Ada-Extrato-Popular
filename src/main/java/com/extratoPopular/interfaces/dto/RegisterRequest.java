package com.extratoPopular.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

@Schema(description = "Requisição para registro de um novo usuário")
public record RegisterRequest(

        @Schema(description = "Nome completo do usuário", example = "Maria Silva")
        @NotBlank(message = "O nome é obrigatório")
        String nome,

        @Schema(description = "E-mail do usuário", example = "maria.silva@email.com")
        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Formato de e-mail inválido")
        String email,

        @Schema(description = "Senha do usuário", example = "SenhaSegura123!")
        @NotBlank(message = "A senha é obrigatória")
        String senha,

        @Schema(description = "Renda mensal declarada pelo usuário", example = "2500.00")
        @PositiveOrZero(message = "A renda mensal não pode ser negativa")
        BigDecimal rendaMensal
) {
}
