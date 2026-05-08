package com.extratoPopular.infrastructure.ai;

import com.extratoPopular.application.service.FinancialContextService;
import com.extratoPopular.domain.enums.TipoTransacao;
import com.extratoPopular.domain.model.Transacao;
import com.extratoPopular.infrastructure.persistence.TransacaoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FinancialContextServiceImpl implements FinancialContextService {

    private final TransacaoRepository transacaoRepository;

    public FinancialContextServiceImpl(TransacaoRepository transacaoRepository) {
        this.transacaoRepository = transacaoRepository;
    }

    @Override
    public String buildContext(Long userId) {
        LocalDate hoje = LocalDate.now();
        LocalDate noventaDiasAtras = hoje.minusDays(90);

        List<Transacao> transacoes = transacaoRepository.findAllByUserId(userId)
                .stream()
                .filter(t -> !t.getData().isBefore(noventaDiasAtras))
                .sorted(Comparator.comparing(Transacao::getData).reversed())
                .limit(50)
                .toList();

        if (transacoes.isEmpty()) {
            return "Nenhuma transação encontrada nos últimos 90 dias.";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("=== CONTEXTO FINANCEIRO ===\n");
        sb.append("Período: ").append(noventaDiasAtras).append(" a ").append(hoje).append("\n");
        sb.append("Total de transações: ").append(transacoes.size()).append("\n\n");

        sb.append(String.format("%-10s | %-13s | %-7s | %-10s | %s%n",
                "DATA", "CATEGORIA", "TIPO", "VALOR", "DESCRIÇÃO"));
        sb.append("-".repeat(70)).append("\n");

        for (Transacao t : transacoes) {
            sb.append(String.format("%-10s | %-13s | %-7s | R$%8.2f | %s%n",
                    t.getData(),
                    t.getCategoria(),
                    t.getTipo(),
                    t.getValor(),
                    t.getDescricao()));
            if (sb.length() > 3500) {
                sb.append("... (transações omitidas por limite de tamanho)\n");
                break;
            }
        }

        BigDecimal totalReceitas = transacoes.stream()
                .filter(t -> t.getTipo() == TipoTransacao.CREDITO)
                .map(Transacao::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDespesas = transacoes.stream()
                .filter(t -> t.getTipo() == TipoTransacao.DEBITO)
                .map(Transacao::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal saldo = totalReceitas.subtract(totalDespesas);

        sb.append("\n=== RESUMO ===\n");
        sb.append(String.format("Total receitas: R$ %.2f%n", totalReceitas));
        sb.append(String.format("Total despesas: R$ %.2f%n", totalDespesas));
        sb.append(String.format("Saldo: R$ %.2f%n", saldo));

        Map<String, BigDecimal> gastosPorCategoria = transacoes.stream()
                .filter(t -> t.getTipo() == TipoTransacao.DEBITO)
                .collect(Collectors.groupingBy(
                        t -> t.getCategoria().name(),
                        Collectors.reducing(BigDecimal.ZERO, Transacao::getValor, BigDecimal::add)
                ));

        sb.append("\nTop 3 categorias de gastos:\n");
        gastosPorCategoria.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(3)
                .forEach(e -> sb.append(String.format("- %s: R$ %.2f%n", e.getKey(), e.getValue())));

        String resultado = sb.toString();
        if (resultado.length() > 4000) {
            return resultado.substring(0, 4000);
        }
        return resultado;
    }
}
