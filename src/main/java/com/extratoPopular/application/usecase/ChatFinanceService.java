package com.extratoPopular.application.usecase;

import com.extratoPopular.application.service.FinancialContextService;
import com.extratoPopular.domain.exception.AiIntegrationException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class ChatFinanceService {

    private static final String SYSTEM_MESSAGE =
            "Você é um assistente financeiro pessoal do aplicativo Extrato Popular. " +
            "Seu papel é ajudar usuários de baixa renda a entender suas finanças de forma simples e sem julgamentos. " +
            "Responda APENAS com base nas informações financeiras fornecidas no contexto abaixo. " +
            "Nunca invente valores, datas ou categorias que não estejam no contexto. " +
            "Nunca mencione dados de outros usuários — você tem acesso apenas aos dados do usuário atual. " +
            "Nunca faça aconselhamento jurídico ou médico. " +
            "Se a pergunta não puder ser respondida com o contexto fornecido, diga claramente que não há dados suficientes. " +
            "Responda sempre em português brasileiro, com linguagem simples e tom encorajador.";

    private static final String SEM_DADOS =
            "Nenhuma transação encontrada nos últimos 90 dias.";

    private final FinancialContextService financialContextService;
    private final ChatClient chatClient;

    public ChatFinanceService(FinancialContextService financialContextService, ChatClient chatClient) {
        this.financialContextService = financialContextService;
        this.chatClient = chatClient;
    }

    public String chat(Long userId, String message) {
        String contexto = financialContextService.buildContext(userId);

        if (SEM_DADOS.equals(contexto)) {
            return "Ainda não há transações registradas nos últimos 90 dias. " +
                   "Importe seu extrato para que eu possa ajudar com sua análise financeira.";
        }

        String userMessage = message + "\n\nContexto financeiro:\n" + contexto;

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
