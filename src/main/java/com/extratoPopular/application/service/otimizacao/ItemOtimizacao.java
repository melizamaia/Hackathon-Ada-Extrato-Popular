package com.extratoPopular.application.service.otimizacao;

import java.math.BigDecimal;

/**
 * Representa uma categoria de gasto acima do orçamento.
 * dificuldade (1-10) modela o custo comportamental de cortar aquela categoria.
 */
public record ItemOtimizacao(
        String categoria,
        BigDecimal excesso,
        BigDecimal valorOrcamento,
        BigDecimal valorGasto,
        int dificuldade
) {}
