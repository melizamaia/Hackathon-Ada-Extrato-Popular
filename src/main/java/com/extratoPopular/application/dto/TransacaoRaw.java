package com.extratoPopular.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransacaoRaw(
        LocalDate data,
        BigDecimal valor,
        String descricao
) {
}
