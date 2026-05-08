package com.extratoPopular.application.service.rag;

import com.extratoPopular.application.usecase.InsightsTransacoesUseCase;
import com.extratoPopular.application.usecase.ResumoTransacoesUseCase;
import com.extratoPopular.domain.enums.NivelAlerta;
import com.extratoPopular.interfaces.dto.AlertaOrcamentoResponse;
import com.extratoPopular.interfaces.dto.InsightsResponse;
import com.extratoPopular.interfaces.dto.ResumoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContextoFinanceiroServiceTest {

    @Mock private ResumoTransacoesUseCase resumoUseCase;
    @Mock private InsightsTransacoesUseCase insightsUseCase;

    @InjectMocks
    private ContextoFinanceiroService service;

    private ResumoResponse resumoSemAlertas;
    private InsightsResponse insightsSemAlerta;

    @BeforeEach
    void setUp() {
        resumoSemAlertas = new ResumoResponse(
                5, 2026,
                new BigDecimal("3000"), new BigDecimal("1500"),
                new BigDecimal("1500"), 10,
                Map.of("ALIMENTACAO", new BigDecimal("500")),
                List.of()
        );

        insightsSemAlerta = new InsightsResponse(
                "ALIMENTACAO",
                new BigDecimal("50.00"),
                new BigDecimal("150.00"),
                false,
                List.of()
        );
    }

    @Test
    void deve_gerar_contexto_sem_alertas() {
        when(resumoUseCase.execute(anyLong(), anyInt(), anyInt())).thenReturn(resumoSemAlertas);
        when(insightsUseCase.execute(anyLong())).thenReturn(insightsSemAlerta);

        String contexto = service.gerarContextoFinanceiro(1L);

        assertNotNull(contexto);
        assertTrue(contexto.contains("50.00"));
        assertFalse(contexto.contains("ALERTA: gastos totais"));
        assertFalse(contexto.contains("ALERTAS DE ORÇAMENTO"));
    }

    @Test
    void deve_incluir_alerta_gasto_elevado_quando_true() {
        InsightsResponse comAlerta = new InsightsResponse(
                "ALIMENTACAO",
                new BigDecimal("110.00"),
                new BigDecimal("200.00"),
                true,
                List.of()
        );
        when(resumoUseCase.execute(anyLong(), anyInt(), anyInt())).thenReturn(resumoSemAlertas);
        when(insightsUseCase.execute(anyLong())).thenReturn(comAlerta);

        String contexto = service.gerarContextoFinanceiro(1L);

        assertTrue(contexto.contains("ALERTA: gastos totais superiores"));
    }

    @Test
    void deve_incluir_alertas_de_orcamento_quando_presentes() {
        AlertaOrcamentoResponse alerta = new AlertaOrcamentoResponse(
                "LAZER", NivelAlerta.CRITICO,
                new BigDecimal("200"), new BigDecimal("350"),
                new BigDecimal("175.00"),
                "Limite ultrapassado em LAZER"
        );
        ResumoResponse resumoComAlerta = new ResumoResponse(
                5, 2026,
                new BigDecimal("3000"), new BigDecimal("1500"),
                new BigDecimal("1500"), 10,
                Map.of("LAZER", new BigDecimal("350")),
                List.of(alerta)
        );

        when(resumoUseCase.execute(anyLong(), anyInt(), anyInt())).thenReturn(resumoComAlerta);
        when(insightsUseCase.execute(anyLong())).thenReturn(insightsSemAlerta);

        String contexto = service.gerarContextoFinanceiro(1L);

        assertTrue(contexto.contains("ALERTAS DE ORÇAMENTO"));
        assertTrue(contexto.contains("Limite ultrapassado em LAZER"));
        assertTrue(contexto.contains("CRITICO"));
    }

    @Test
    void deve_incluir_periodo_no_contexto() {
        when(resumoUseCase.execute(anyLong(), anyInt(), anyInt())).thenReturn(resumoSemAlertas);
        when(insightsUseCase.execute(anyLong())).thenReturn(insightsSemAlerta);

        String contexto = service.gerarContextoFinanceiro(1L);

        assertTrue(contexto.contains("Período:"));
    }
}
