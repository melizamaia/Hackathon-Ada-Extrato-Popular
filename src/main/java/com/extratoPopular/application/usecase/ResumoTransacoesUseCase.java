package com.extratoPopular.application.usecase;

import com.extratoPopular.domain.enums.NivelAlerta;
import com.extratoPopular.domain.enums.TipoTransacao;
import com.extratoPopular.domain.model.Orcamento;
import com.extratoPopular.domain.model.Transacao;
import com.extratoPopular.infrastructure.persistence.OrcamentoRepository;
import com.extratoPopular.infrastructure.persistence.TransacaoRepository;
import com.extratoPopular.interfaces.dto.AlertaOrcamentoResponse;
import com.extratoPopular.interfaces.dto.ResumoResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class ResumoTransacoesUseCase {

    private static final BigDecimal LIMIAR_INFO    = new BigDecimal("70");
    private static final BigDecimal LIMIAR_AVISO   = new BigDecimal("90");
    private static final BigDecimal LIMIAR_CRITICO = new BigDecimal("100");

    private final TransacaoRepository transacaoRepository;
    private final OrcamentoRepository orcamentoRepository;

    public ResumoTransacoesUseCase(TransacaoRepository transacaoRepository,
                                   OrcamentoRepository orcamentoRepository) {
        this.transacaoRepository = transacaoRepository;
        this.orcamentoRepository = orcamentoRepository;
    }

    public ResumoResponse execute(Long userId, int mes, int ano) {
        LocalDate inicio = LocalDate.of(ano, mes, 1);
        LocalDate fim    = inicio.withDayOfMonth(inicio.lengthOfMonth());

        List<Transacao> transacoes = transacaoRepository.findAllByUserIdAndDataBetween(userId, inicio, fim);

        BigDecimal totalReceitas = BigDecimal.ZERO;
        BigDecimal totalDespesas = BigDecimal.ZERO;
        Map<String, BigDecimal> gastosPorCategoria = new TreeMap<>();

        for (Transacao t : transacoes) {
            if (t.getTipo() == TipoTransacao.CREDITO) {
                totalReceitas = totalReceitas.add(t.getValor());
            } else {
                BigDecimal valorAbsoluto = t.getValor().abs();
                totalDespesas = totalDespesas.add(valorAbsoluto);
                gastosPorCategoria.merge(t.getCategoria().name(), valorAbsoluto, BigDecimal::add);
            }
        }

        BigDecimal saldo = totalReceitas.subtract(totalDespesas);
        List<AlertaOrcamentoResponse> alertas = computarAlertas(userId, mes, ano, gastosPorCategoria);

        return new ResumoResponse(mes, ano, totalReceitas, totalDespesas, saldo,
                transacoes.size(), gastosPorCategoria, alertas);
    }

    private List<AlertaOrcamentoResponse> computarAlertas(Long userId, int mes, int ano,
                                                           Map<String, BigDecimal> gastosPorCategoria) {
        List<Orcamento> orcamentos = orcamentoRepository.findByUserIdAndMesAndAno(userId, mes, ano);
        List<AlertaOrcamentoResponse> alertas = new ArrayList<>();

        for (Orcamento orcamento : orcamentos) {
            BigDecimal gasto = gastosPorCategoria.getOrDefault(orcamento.getCategoria().name(), BigDecimal.ZERO);

            if (gasto.compareTo(BigDecimal.ZERO) == 0) continue;

            BigDecimal percentual = gasto
                    .divide(orcamento.getValorLimite(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);

            NivelAlerta nivel;
            String mensagem;

            if (percentual.compareTo(LIMIAR_CRITICO) >= 0) {
                nivel = NivelAlerta.CRITICO;
                mensagem = "Orçamento de " + orcamento.getCategoria() + " estourou! " +
                           percentual + "% utilizado (R$ " + gasto + " de R$ " + orcamento.getValorLimite() + ")";
            } else if (percentual.compareTo(LIMIAR_AVISO) >= 0) {
                nivel = NivelAlerta.AVISO;
                mensagem = "90% do orçamento de " + orcamento.getCategoria() + " foi atingido. " +
                           percentual + "% utilizado";
            } else if (percentual.compareTo(LIMIAR_INFO) >= 0) {
                nivel = NivelAlerta.INFO;
                mensagem = "70% do orçamento de " + orcamento.getCategoria() + " foi atingido. " +
                           percentual + "% utilizado";
            } else {
                continue;
            }

            alertas.add(new AlertaOrcamentoResponse(
                    orcamento.getCategoria().name(), nivel,
                    orcamento.getValorLimite(), gasto, percentual, mensagem));
        }

        return alertas;
    }
}
