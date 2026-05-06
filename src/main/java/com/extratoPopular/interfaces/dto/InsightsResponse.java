package com.extratoPopular.interfaces.dto;

import java.math.BigDecimal;
import java.util.List;

public record InsightsResponse(
        String categoriaComMaiorGasto,
        BigDecimal percentualRendaComprometida,
        BigDecimal mediaGastoPorTransacao,
        boolean alertaGastoElevado,
        List<CategoriaInsight> topCategorias
) {
    public record CategoriaInsight(
            String categoria,
            BigDecimal total,
            BigDecimal percentualDoTotal
    ) {}
}
