package com.extratoPopular.interfaces.controller;

import com.extratoPopular.infrastructure.security.SecurityUtils;
import com.extratoPopular.interfaces.dto.AiChatRequest;
import com.extratoPopular.interfaces.dto.AiChatResponse;
import com.extratoPopular.interfaces.dto.RelatorioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1")
@Tag(
        name = "IA Financeira v1",
        description = "Endpoints de IA financeira — chat e relatórios inteligentes (v1)"
)
@SecurityRequirement(name = "BearerAuth")
public class AiController {

    @PostMapping("/chat")
    @Operation(summary = "Interação com assistente financeiro inteligente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resposta do assistente"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    })
    public ResponseEntity<AiChatResponse> chat(
            @Valid @RequestBody AiChatRequest request
    ) {
        Long userId = SecurityUtils.getCurrentUserId();

        // TODO M9: substituir mock por chatFinanceService.chat(userId, request.message())
        AiChatResponse response = new AiChatResponse(
                "Funcionalidade em integração",
                LocalDateTime.now()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/relatorio")
    @Operation(summary = "Gera relatório financeiro personalizado com IA")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Relatório gerado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    })
    public ResponseEntity<RelatorioResponse> relatorio() {
        Long userId = SecurityUtils.getCurrentUserId();

        // TODO M9: substituir mock por reportFinanceService.generateReport(userId)
        RelatorioResponse response = new RelatorioResponse(
                "Relatório em integração"
        );

        return ResponseEntity.ok(response);
    }
}
