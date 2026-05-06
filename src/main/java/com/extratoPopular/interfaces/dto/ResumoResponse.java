package com.extratoPopular.interfaces.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ResumoResponse(
        Integer mes,
        Integer ano,
        BigDecimal totalReceitas,
        BigDecimal totalDespesas,
        BigDecimal saldo,
        int totalTransacoes,
        Map<String, BigDecimal> gastosPorCategoria,
        List<AlertaOrcamentoResponse> alertas
) {}
