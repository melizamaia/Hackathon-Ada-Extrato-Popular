package com.extratoPopular.application.service.rag;

import com.extratoPopular.application.usecase.InsightsTransacoesUseCase;
import com.extratoPopular.application.usecase.ResumoTransacoesUseCase;
import com.extratoPopular.infrastructure.security.SecurityUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class ContextoFinanceiroService {

    private final ResumoTransacoesUseCase resumoUseCase;
    private final InsightsTransacoesUseCase insightsUseCase;

    public ContextoFinanceiroService(
            ResumoTransacoesUseCase resumoUseCase,
            InsightsTransacoesUseCase insightsUseCase
    ) {
        this.resumoUseCase = resumoUseCase;
        this.insightsUseCase = insightsUseCase;
    }

    public String gerarContextoFinanceiro() {

        return """
            CONTEXTO FINANCEIRO DO USUÁRIO

            RESUMO:
            - Receita mensal: R$ 4500
            - Gastos com alimentação: R$ 850
            - Gastos com transporte: R$ 320
            - Gastos com lazer: R$ 540

            INSIGHTS:
            - Usuário gastou acima da média em lazer.
            - Há oportunidade de economia em delivery.
            - O saldo mensal permanece positivo.
            """;
    }
}