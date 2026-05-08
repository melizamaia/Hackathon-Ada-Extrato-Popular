package com.extratoPopular.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ChatFinanceService {

    private static final Logger log = LoggerFactory.getLogger(ChatFinanceService.class);

    /**
     * Processa uma mensagem de chat financeiro para o usuário.
     *
     * @param userId  ID do usuário autenticado (via SecurityUtils)
     * @param message mensagem enviada pelo usuário
     * @return resposta do assistente financeiro
     */
    public String chat(Long userId, String message) {
        log.info("Chat financeiro solicitado — userId={}", userId);

        // TODO M10: integrar com Spring AI + RAG (ContextoFinanceiroService)
        return "Funcionalidade de chat em integração. Sua mensagem foi recebida.";
    }
}
