package com.extratoPopular.application.service.rag;

import org.springframework.stereotype.Service;

@Service
public class PromptFinanceiroService {

    public String construirPrompt(
            String contexto,
            String pergunta
    ) {

        return """
                Você é um assistente financeiro inteligente.

                Seu papel é:
                - analisar hábitos financeiros
                - identificar excessos
                - sugerir economia
                - responder de forma clara e objetiva

                Utilize exclusivamente o contexto financeiro fornecido.

                CONTEXTO FINANCEIRO:
                %s

                PERGUNTA DO USUÁRIO:
                %s
                """.formatted(
                contexto,
                pergunta
        );
    }
}