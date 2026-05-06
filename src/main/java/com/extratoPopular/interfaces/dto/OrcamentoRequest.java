package com.extratoPopular.interfaces.dto;

import com.extratoPopular.domain.enums.Categoria;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record OrcamentoRequest(
        @NotNull(message = "A categoria é obrigatória")
        Categoria categoria,

        @NotNull(message = "O valor limite é obrigatório")
        @Positive(message = "O valor limite deve ser positivo")
        BigDecimal valorLimite,

        @NotNull(message = "O mês é obrigatório")
        @Min(value = 1, message = "Mês deve ser entre 1 e 12")
        @Max(value = 12, message = "Mês deve ser entre 1 e 12")
        Integer mes,

        @NotNull(message = "O ano é obrigatório")
        @Min(value = 2000, message = "Ano inválido")
        Integer ano
) {}
