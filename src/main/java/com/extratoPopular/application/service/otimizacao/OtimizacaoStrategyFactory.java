package com.extratoPopular.application.service.otimizacao;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Factory que resolve a estratégia a partir de um nome em string.
 * Spring injeta automaticamente todas as implementações de OtimizacaoStrategy
 * — adicionar um novo algoritmo não requer alterar esta classe (OCP).
 */
@Component
public class OtimizacaoStrategyFactory {

    private static final String DEFAULT = "KNAPSACK";

    private final Map<String, OtimizacaoStrategy> strategies;

    public OtimizacaoStrategyFactory(List<OtimizacaoStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(s -> s.getNome().toUpperCase(), s -> s));
    }

    public OtimizacaoStrategy get(String nome) {
        if (nome == null) return strategies.get(DEFAULT);
        OtimizacaoStrategy s = strategies.get(nome.toUpperCase());
        return s != null ? s : strategies.get(DEFAULT);
    }
}
