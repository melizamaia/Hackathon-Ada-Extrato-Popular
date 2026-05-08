package com.extratoPopular.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ReportFinanceService {

    private static final Logger log = LoggerFactory.getLogger(ReportFinanceService.class);

    /**
     * Gera um relatório financeiro personalizado para o usuário.
     *
     * @param userId ID do usuário autenticado (via SecurityUtils)
     * @return conteúdo do relatório gerado
     */
    public String generateReport(Long userId) {
        log.info("Relatório financeiro solicitado — userId={}", userId);

        // TODO M10: integrar com Spring AI + RAG (ContextoFinanceiroService)
        return "Relatório financeiro em integração.";
    }
}
