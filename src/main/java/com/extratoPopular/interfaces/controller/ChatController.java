package com.extratoPopular.interfaces.controller;

import com.extratoPopular.interfaces.dto.ChatRequest;
import com.extratoPopular.interfaces.dto.ChatResponse;
import com.extratoPopular.application.service.ChatService;
import com.extratoPopular.infrastructure.security.SecurityUtils;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat")
@Tag(
        name = "IA Financeira",
        description = "Endpoints responsáveis pela geração de insights financeiros utilizando IA e RAG"
)
@SecurityRequirement(name = "BearerAuth")
public class ChatController {

    private final ChatService service;

    public ChatController(ChatService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Realiza interação com assistente financeiro inteligente")
    public ChatResponse conversar(
            @Valid @RequestBody ChatRequest request
    ) {
        Long userId = SecurityUtils.getCurrentUserId();
        return service.conversar(userId, request);
    }
}