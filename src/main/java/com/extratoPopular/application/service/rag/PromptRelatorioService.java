package com.extratoPopular.application.service.rag;

import org.springframework.stereotype.Service;

@Service
public class PromptRelatorioService {

    public String construirPrompt(String contexto) {

        return """
                Você é um analista financeiro especialista em educação financeira.

                Gere um relatório financeiro inteligente baseado no contexto abaixo.

                O relatório deve conter:

                - resumo financeiro geral
                - principais categorias de gastos
                - hábitos financeiros preocupantes
                - oportunidades de economia
                - recomendações práticas
                - análise final

                Seja claro, organizado e objetivo.

                CONTEXTO:
                %s
                """.formatted(contexto);
    }
}