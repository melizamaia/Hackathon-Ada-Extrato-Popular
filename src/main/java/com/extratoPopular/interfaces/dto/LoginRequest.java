package com.extratoPopular.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Requisição para autenticação de usuário")
public record LoginRequest(

        @Schema(description = "E-mail do usuário", example = "maria.silva@email.com")
        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Formato de e-mail inválido")
        String email,

        @Schema(description = "Senha do usuário", example = "SenhaSegura123!")
        @NotBlank(message = "A senha é obrigatória")
        String senha
) {
}
