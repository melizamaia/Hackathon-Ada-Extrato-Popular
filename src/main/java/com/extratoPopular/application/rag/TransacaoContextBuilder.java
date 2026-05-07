package com.extratoPopular.application.rag;

import java.math.BigDecimal;
import java.util.Map;

public class TransacaoContextBuilder {

    public String build(
            Map<String, BigDecimal> porCategoria,
            BigDecimal totalEntradas,
            BigDecimal totalSaidas
    ) {

        StringBuilder sb = new StringBuilder();

        sb.append("CONTEXTO FINANCEIRO DO USUÁRIO\n\n");

        sb.append("GASTOS POR CATEGORIA:\n");

        if (porCategoria == null || porCategoria.isEmpty()) {
            sb.append("- Nenhuma categoria encontrada\n");
        } else {
            porCategoria.forEach((categoria, valor) -> sb.append("- ")
                    .append(categoria)
                    .append(": R$ ")
                    .append(valor)
                    .append("\n"));
        }

        sb.append("\nRESUMO GERAL:\n");
        sb.append("- Total de entradas: R$ ")
                .append(totalEntradas != null ? totalEntradas : BigDecimal.ZERO)
                .append("\n");

        sb.append("- Total de saídas: R$ ")
                .append(totalSaidas != null ? totalSaidas : BigDecimal.ZERO)
                .append("\n");

        BigDecimal entradas = totalEntradas != null ? totalEntradas : BigDecimal.ZERO;
        BigDecimal saidas = totalSaidas != null ? totalSaidas : BigDecimal.ZERO;
        BigDecimal saldo = entradas.subtract(saidas);

        sb.append("- Saldo: R$ ").append(saldo).append("\n");

        sb.append("\nINSIGHT BASE:\n");

        String maiorCategoria = porCategoria == null || porCategoria.isEmpty()
                ? "N/A"
                : porCategoria.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        sb.append("- Maior gasto: ").append(maiorCategoria).append("\n");

        return sb.toString();
    }
}