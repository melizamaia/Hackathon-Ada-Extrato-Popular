package com.extratoPopular.interfaces.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OtimizacaoResponseTest {

    @Test
    void acaoRecomendada_deve_expor_todos_os_campos() {
        OtimizacaoResponse.AcaoRecomendada acao = new OtimizacaoResponse.AcaoRecomendada(
                "LAZER", BigDecimal.valueOf(150), 3, "Reduza gastos em LAZER", 1
        );
        assertEquals("LAZER", acao.categoria());
        assertEquals(BigDecimal.valueOf(150), acao.economiaPotencial());
        assertEquals(3, acao.dificuldade());
        assertEquals("Reduza gastos em LAZER", acao.descricaoAcao());
        assertEquals(1, acao.prioridade());
    }

    @Test
    void otimizacaoResponse_deve_expor_acoes_recomendadas() {
        OtimizacaoResponse.AcaoRecomendada acao = new OtimizacaoResponse.AcaoRecomendada(
                "ALIMENTACAO", BigDecimal.valueOf(200), 5, "desc", 1
        );
        OtimizacaoResponse resp = new OtimizacaoResponse(
                5, 2026,
                BigDecimal.valueOf(1500),
                BigDecimal.valueOf(50),
                List.of(),
                List.of(),
                BigDecimal.valueOf(200),
                "recomendacao",
                "KNAPSACK",
                "desc algoritmo",
                List.of(acao)
        );
        assertEquals(1, resp.acoesRecomendadas().size());
        assertEquals("ALIMENTACAO", resp.acoesRecomendadas().get(0).categoria());
    }
}
