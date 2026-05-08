package com.extratoPopular.application.service.otimizacao;

import java.math.BigDecimal;

public record AcaoOtimizacao(
        String categoria,
        BigDecimal economiaPotencial,
        int dificuldade,
        String descricaoAcao,
        int prioridade
) {}
