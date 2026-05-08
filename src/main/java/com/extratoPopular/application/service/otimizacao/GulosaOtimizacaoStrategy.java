package com.extratoPopular.application.service.otimizacao;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Algoritmo guloso (greedy) por eficiência.
 *
 * Ordena as categorias pelo índice eficiência = excesso / dificuldade (maior = melhor)
 * e seleciona itens enquanto houver capacidade disponível (CAPACIDADE_MAXIMA = 20).
 *
 * Rápido e intuitivo, porém não garante solução globalmente ótima — pode perder
 * combinações onde um item "caro" libera uma economia maior que vários baratos.
 * Use quando a velocidade de decisão importa mais que a precisão.
 */
@Component
public class GulosaOtimizacaoStrategy implements OtimizacaoStrategy {

    private static final int CAPACIDADE_MAXIMA = 20;

    @Override
    public ResultadoOtimizacao otimizar(List<ItemOtimizacao> itens) {
        if (itens.isEmpty()) {
            return new ResultadoOtimizacao(List.of(), BigDecimal.ZERO, getNome(), getDescricao());
        }

        List<ItemOtimizacao> ordenados = itens.stream()
                .sorted(Comparator.comparingDouble(
                        i -> -i.excesso().doubleValue() / i.dificuldade()))
                .toList();

        List<AcaoOtimizacao> acoes = new ArrayList<>();
        int capacidadeRestante = CAPACIDADE_MAXIMA;
        int prioridade = 1;

        for (ItemOtimizacao item : ordenados) {
            if (item.dificuldade() > capacidadeRestante) continue;
            acoes.add(new AcaoOtimizacao(
                    item.categoria(),
                    item.excesso(),
                    item.dificuldade(),
                    "Reduza R$ " + item.excesso().setScale(2, RoundingMode.HALF_UP)
                            + " em " + item.categoria()
                            + " — maior eficiência (economia/esforço)",
                    prioridade++
            ));
            capacidadeRestante -= item.dificuldade();
        }

        BigDecimal total = acoes.stream()
                .map(AcaoOtimizacao::economiaPotencial)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ResultadoOtimizacao(acoes, total, getNome(), getDescricao());
    }

    @Override
    public String getNome() {
        return "GULOSA";
    }

    @Override
    public String getDescricao() {
        return "Algoritmo guloso por eficiência (excesso ÷ dificuldade): prioriza as categorias com maior "
                + "retorno por unidade de esforço. Solução próxima do ótimo com menor custo computacional.";
    }
}
