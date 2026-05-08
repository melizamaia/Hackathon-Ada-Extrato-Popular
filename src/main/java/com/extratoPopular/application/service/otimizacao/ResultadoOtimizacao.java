package com.extratoPopular.application.service.otimizacao;

import java.math.BigDecimal;
import java.util.List;

public record ResultadoOtimizacao(
        List<AcaoOtimizacao> acoes,
        BigDecimal totalEconomiaSelecionada,
        String nomeAlgoritmo,
        String descricaoAlgoritmo
) {}
