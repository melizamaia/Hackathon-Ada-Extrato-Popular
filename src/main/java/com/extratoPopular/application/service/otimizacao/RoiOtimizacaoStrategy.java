package com.extratoPopular.application.service.otimizacao;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Ranqueamento por ROI (Return on Investment).
 *
 * Não há restrição de capacidade: todas as categorias acima do orçamento
 * são listadas em ordem decrescente de ROI = excesso / dificuldade.
 * Útil para visualizar o retorno financeiro esperado de cada ação
 * e decidir manualmente por onde começar.
 */
@Component
public class RoiOtimizacaoStrategy implements OtimizacaoStrategy {

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
        for (int k = 0; k < ordenados.size(); k++) {
            ItemOtimizacao item = ordenados.get(k);
            double roi = item.excesso().doubleValue() / item.dificuldade();
            acoes.add(new AcaoOtimizacao(
                    item.categoria(),
                    item.excesso(),
                    item.dificuldade(),
                    "Reduza R$ " + item.excesso().setScale(2, RoundingMode.HALF_UP)
                            + " em " + item.categoria()
                            + " — ROI = " + String.format("%.1f", roi) + " (R$/esforço)",
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
        return "ROI";
    }

    @Override
    public String getDescricao() {
        return "Ranqueamento por ROI (excesso ÷ dificuldade): lista todas as categorias acima do orçamento "
                + "em ordem de retorno financeiro por unidade de esforço, sem restrição de capacidade.";
    }
}
