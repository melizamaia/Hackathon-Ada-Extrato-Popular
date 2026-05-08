package com.extratoPopular.application.usecase;

import com.extratoPopular.application.service.FinancialContextService;
import com.extratoPopular.domain.exception.AiIntegrationException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class ReportFinanceService {

    private static final String SYSTEM_MESSAGE =
            "Você é um analista financeiro pessoal do aplicativo Extrato Popular. " +
            "Seu papel é gerar relatórios claros e encorajadores sobre a saúde financeira do usuário. " +
            "Analise APENAS os dados fornecidos no contexto. Nunca invente informações. " +
            "Nunca mencione dados de outros usuários. " +
            "O relatório deve conter obrigatoriamente: " +
            "1. Resumo do período analisado (datas, total de transações). " +
            "2. Avaliação da saúde financeira: Positiva, Atenção ou Crítica — com justificativa simples. " +
            "3. Top 3 pontos de atenção nos gastos (baseados nos dados reais do contexto). " +
            "4. 3 sugestões práticas e realistas de melhoria (adequadas à renda do usuário). " +
            "Limite o relatório a no máximo 800 palavras. " +
            "Responda em português brasileiro, tom encorajador, linguagem simples, sem jargão financeiro.";

    private static final String SEM_DADOS =
            "Nenhuma transação encontrada nos últimos 90 dias.";

    private final FinancialContextService financialContextService;
    private final ChatClient chatClient;

    public ReportFinanceService(FinancialContextService financialContextService, ChatClient chatClient) {
        this.financialContextService = financialContextService;
        this.chatClient = chatClient;
    }

    public String generateReport(Long userId) {
        String contexto = financialContextService.buildContext(userId);

        if (SEM_DADOS.equals(contexto)) {
            return "Ainda não há transações registradas nos últimos 90 dias. " +
                   "Importe seu extrato para que eu possa gerar seu relatório financeiro.";
        }

        String userMessage = "Gere o relatório financeiro com base no contexto abaixo:\n\n" + contexto;

        try {
            return chatClient.prompt()
                    .system(SYSTEM_MESSAGE)
                    .user(userMessage)
                    .call()
                    .content();
        } catch (RuntimeException e) {
            throw new AiIntegrationException("Falha ao consultar o serviço de IA", e);
        }
    }
}
