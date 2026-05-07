package com.extratoPopular.interfaces.controller;

import com.extratoPopular.infrastructure.ai.TesteOpenAIService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@Tag(
        name = "IA Financeira",
        description = "Endpoints responsáveis pela geração de insights financeiros utilizando IA e RAG"
)
public class TesteIAController {

    private final TesteOpenAIService service;

    public TesteIAController(TesteOpenAIService service) {
        this.service = service;
    }

    @GetMapping("/teste-ia")
    public String testar() {

        try {
            return service.testar();

        } catch (Exception e) {

            e.printStackTrace();

            return e.getMessage();
        }
    }
}