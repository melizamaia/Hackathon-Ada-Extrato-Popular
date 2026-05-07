package com.extratoPopular.interfaces.controller;

import com.extratoPopular.interfaces.dto.ChatRequest;
import com.extratoPopular.interfaces.dto.ChatResponse;
import com.extratoPopular.application.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat")
@Tag(
        name = "IA Financeira",
        description = "Endpoint responsável pela geração de insights financeiros utilizando IA e RAG"
)
public class ChatController {

    private final ChatService service;

    public ChatController(ChatService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Realiza interação com assistente financeiro inteligente")
    public ChatResponse conversar(
            @RequestBody ChatRequest request
    ) {

        return service.conversar(request);
    }
}