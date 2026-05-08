package com.extratoPopular.application.service.otimizacao;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Algoritmo mochila 0/1 via programação dinâmica.
 *
 * Modelo:
 *   - Cada categoria com gasto acima do orçamento é um item.
 *   - Valor   = excesso em reais (retorno financeiro ao cortar aquela categoria).
 *   - Peso    = dificuldade (1-10) de mudar o comportamento naquela categoria.
 *   - Capaci  = CAPACIDADE_MAXIMA (20 unidades de esforço disponíveis).
 *
 * Garante a seleção ÓTIMA de categorias a cortar — maximiza a economia total
 * dentro da capacidade de esforço do usuário.
 * Complexidade: O(n × W), onde n = nº de categorias e W = 20.
 */
@Component
public class KnapsackOtimizacaoStrategy implements OtimizacaoStrategy {

    private static final int CAPACIDADE_MAXIMA = 20;

    @Override
    public ResultadoOtimizacao otimizar(List<ItemOtimizacao> itens) {
        if (itens.isEmpty()) {
            return new ResultadoOtimizacao(List.of(), BigDecimal.ZERO, getNome(), getDescricao());
        }

        int n = itens.size();
        int W = CAPACIDADE_MAXIMA;

        // dp[i][w] = máximo excesso recuperável usando os primeiros i itens com capacidade w
        int[][] dp = new int[n + 1][W + 1];

        for (int i = 1; i <= n; i++) {
            ItemOtimizacao item = itens.get(i - 1);
            int valor = item.excesso().intValue(); // reais inteiros
            int peso  = item.dificuldade();

            for (int w = 0; w <= W; w++) {
                dp[i][w] = dp[i - 1][w];
                if (peso <= w) {
                    int comItem = dp[i - 1][w - peso] + valor;
                    if (comItem > dp[i][w]) {
                        dp[i][w] = comItem;
                    }
                }
            }
        }

        // Backtracking para identificar quais itens foram selecionados
        List<ItemOtimizacao> selecionados = new ArrayList<>();
        int w = W;
        for (int i = n; i >= 1; i--) {
            if (dp[i][w] != dp[i - 1][w]) {
                selecionados.add(0, itens.get(i - 1));
                w -= itens.get(i - 1).dificuldade();
            }
        }

        List<AcaoOtimizacao> acoes = new ArrayList<>();
        for (int k = 0; k < selecionados.size(); k++) {
            ItemOtimizacao item = selecionados.get(k);
            acoes.add(new AcaoOtimizacao(
                    item.categoria(),
                    item.excesso(),
                    item.dificuldade(),
                    "Reduza R$ " + item.excesso().setScale(2, RoundingMode.HALF_UP)
                            + " em " + item.categoria()
                            + " — selecionado pelo algoritmo mochila (máxima economia com mínimo esforço)",
                    k + 1
            ));
        }

        BigDecimal total = acoes.stream()
                .map(AcaoOtimizacao::economiaPotencial)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ResultadoOtimizacao(acoes, total, getNome(), getDescricao());
    }

    @Override
    public String getNome() {
        return "KNAPSACK";
    }

    @Override
    public String getDescricao() {
        return "Programação dinâmica (mochila 0/1): seleciona o subconjunto ótimo de categorias a cortar, "
                + "maximizando a economia total dentro de uma capacidade de esforço de " + CAPACIDADE_MAXIMA + " pontos.";
    }
}
