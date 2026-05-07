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

        Long userId = SecurityUtils.getCurrentUserId();

        int mes = LocalDate.now().getMonthValue();
        int ano = LocalDate.now().getYear();

        var resumo = resumoUseCase.execute(userId, mes, ano);
        var insights = insightsUseCase.execute(userId);

        return """
            CONTEXTO FINANCEIRO DO USUÁRIO

            RESUMO:
            %s

            INSIGHTS:
            %s
            """.formatted(resumo, insights);
    }
}