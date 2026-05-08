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
        String recomendacaoGeral,
        String algoritmoUtilizado,
        String descricaoAlgoritmo,
        List<AcaoRecomendada> acoesRecomendadas
) {
    public record SugestaoCategoria(
            String categoria,
            BigDecimal valorOrcamento,
            BigDecimal valorGasto,
            BigDecimal excesso,
            String sugestao
    ) {}

    public record AcaoRecomendada(
            String categoria,
            BigDecimal economiaPotencial,
            int dificuldade,
            String descricaoAcao,
            int prioridade
    ) {}
}
