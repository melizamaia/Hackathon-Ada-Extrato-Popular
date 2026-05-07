package com.extratoPopular.interfaces.controller;

import com.extratoPopular.infrastructure.ai.TesteOpenAIService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
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