package com.extratoPopular.interfaces.dto;

import com.extratoPopular.domain.enums.Categoria;
import com.extratoPopular.domain.model.Orcamento;

import java.math.BigDecimal;

public record OrcamentoResponse(
        Long id,
        Categoria categoria,
        BigDecimal valorLimite,
        Integer mes,
        Integer ano
) {
    public static OrcamentoResponse de(Orcamento o) {
        return new OrcamentoResponse(o.getId(), o.getCategoria(), o.getValorLimite(), o.getMes(), o.getAno());
    }
}
