package com.extratoPopular.application.service.rag;

import com.extratoPopular.application.rag.TransacaoContextBuilder;
import com.extratoPopular.application.usecase.InsightsTransacoesUseCase;
import com.extratoPopular.application.usecase.ResumoTransacoesUseCase;
import com.extratoPopular.interfaces.dto.InsightsResponse;
import com.extratoPopular.interfaces.dto.ResumoResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class ContextoFinanceiroService {

    private final ResumoTransacoesUseCase resumoUseCase;
    private final InsightsTransacoesUseCase insightsUseCase;
    private final TransacaoContextBuilder contextBuilder = new TransacaoContextBuilder();

    public ContextoFinanceiroService(
            ResumoTransacoesUseCase resumoUseCase,
            InsightsTransacoesUseCase insightsUseCase
    ) {
        this.resumoUseCase   = resumoUseCase;
        this.insightsUseCase = insightsUseCase;
    }

    public String gerarContextoFinanceiro(Long userId) {
        int mes = LocalDate.now().getMonthValue();
        int ano = LocalDate.now().getYear();

        ResumoResponse  resumo   = resumoUseCase.execute(userId, mes, ano);
        InsightsResponse insights = insightsUseCase.execute(userId);

        String contextoBase = contextBuilder.build(
                resumo.gastosPorCategoria(),
                resumo.totalReceitas(),
                resumo.totalDespesas()
        );

        return contextoBase + formatarComplemento(mes, ano, resumo, insights);
    }

    private String formatarComplemento(int mes, int ano,
                                       ResumoResponse resumo,
                                       InsightsResponse insights) {
        StringBuilder sb = new StringBuilder();

        sb.append("\nINFORMAÇÕES ADICIONAIS:\n");
        sb.append("- Período: ").append(mes).append("/").append(ano).append("\n");
        sb.append("- Percentual da renda comprometida: ")
                .append(insights.percentualRendaComprometida()).append("%\n");
        sb.append("- Média gasto por transação: R$ ")
                .append(insights.mediaGastoPorTransacao()).append("\n");

        if (insights.alertaGastoElevado()) {
            sb.append("- ALERTA: gastos totais superiores à renda mensal!\n");
        }

        if (!resumo.alertas().isEmpty()) {
            sb.append("\nALERTAS DE ORÇAMENTO:\n");
            resumo.alertas().forEach(a ->
                    sb.append("- [").append(a.nivel()).append("] ")
                      .append(a.mensagem()).append("\n")
            );
        }

        return sb.toString();
    }
}
