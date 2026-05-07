package com.extratoPopular.interfaces.controller;

import com.extratoPopular.application.service.RelatorioService;
import com.extratoPopular.interfaces.dto.RelatorioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/relatorio")
@Tag(
        name = "Relatórios IA",
        description = "Endpoints responsáveis pela geração de relatórios financeiros inteligentes"
)
public class RelatorioController {

    private final RelatorioService relatorioService;

    public RelatorioController(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }

    @GetMapping
    @Operation(summary = "Gera relatório financeiro personalizado com IA")
    @SecurityRequirement(name = "BearerAuth")
    public RelatorioResponse gerar() {
        return relatorioService.gerarRelatorio();
    }
}