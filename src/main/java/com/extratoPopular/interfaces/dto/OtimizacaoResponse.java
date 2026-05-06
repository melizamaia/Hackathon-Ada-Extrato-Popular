package com.extratoPopular.interfaces.dto;

import java.math.BigDecimal;
import java.util.List;

public record OtimizacaoResponse(
        Integer mes,
        Integer ano,
        BigDecimal saldoMensal,
        BigDecimal percentualRendaComprometida,
        List<SugestaoCategoria> gastosAcimaOrcamento,
        List<String> categoriasSeemOrcamento,
        BigDecimal economiasPotenciais,
        String recomendacaoGeral
) {
    public record SugestaoCategoria(
            String categoria,
            BigDecimal valorOrcamento,
            BigDecimal valorGasto,
            BigDecimal excesso,
            String sugestao
    ) {}
}
