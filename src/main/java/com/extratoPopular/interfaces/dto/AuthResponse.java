package com.extratoPopular.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta de autenticação contendo o token JWT")
public record AuthResponse(

        @Schema(description = "Token JWT gerado para a sessão", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String token,

        @Schema(description = "ID único do usuário", example = "1")
        Long userId,

        @Schema(description = "E-mail do usuário logado", example = "maria.silva@email.com")
        String email
) {
}
