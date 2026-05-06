package com.extratoPopular.interfaces.controller;

import com.extratoPopular.application.usecase.OrcamentoUseCase;
import com.extratoPopular.application.usecase.OtimizacaoUseCase;
import com.extratoPopular.infrastructure.security.SecurityUtils;
import com.extratoPopular.interfaces.dto.OrcamentoRequest;
import com.extratoPopular.interfaces.dto.OrcamentoResponse;
import com.extratoPopular.interfaces.dto.OtimizacaoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/orcamentos")
@Tag(name = "Orçamentos", description = "Gerenciamento de orçamentos por categoria e algoritmo de otimização")
public class OrcamentoController {

    private final OrcamentoUseCase orcamentoUseCase;
    private final OtimizacaoUseCase otimizacaoUseCase;

    public OrcamentoController(OrcamentoUseCase orcamentoUseCase,
                               OtimizacaoUseCase otimizacaoUseCase) {
        this.orcamentoUseCase  = orcamentoUseCase;
        this.otimizacaoUseCase = otimizacaoUseCase;
    }

    @Operation(summary = "Criar orçamento para uma categoria em um mês/ano")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Orçamento criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content),
            @ApiResponse(responseCode = "409", description = "Já existe orçamento para esta categoria/mês/ano", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido", content = @Content)
    })
    @PostMapping
    public ResponseEntity<OrcamentoResponse> criar(@Valid @RequestBody OrcamentoRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(orcamentoUseCase.criar(userId, request));
    }

    @Operation(summary = "Listar orçamentos do mês/ano informado (padrão: mês atual)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido", content = @Content)
    })
    @GetMapping
    public ResponseEntity<List<OrcamentoResponse>> listar(
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer ano) {
        Long userId = SecurityUtils.getCurrentUserId();
        int m = mes != null ? mes : LocalDate.now().getMonthValue();
        int a = ano != null ? ano : LocalDate.now().getYear();
        return ResponseEntity.ok(orcamentoUseCase.listar(userId, m, a));
    }

    @Operation(summary = "Atualizar orçamento")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orçamento atualizado"),
            @ApiResponse(responseCode = "404", description = "Orçamento não encontrado", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido", content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<OrcamentoResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody OrcamentoRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(orcamentoUseCase.atualizar(userId, id, request));
    }

    @Operation(summary = "Deletar orçamento")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Orçamento deletado"),
            @ApiResponse(responseCode = "404", description = "Orçamento não encontrado", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        orcamentoUseCase.deletar(userId, id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Algoritmo de otimização financeira para o mês/ano informado (padrão: mês atual)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Otimização calculada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido", content = @Content)
    })
    @GetMapping("/otimizacao")
    public ResponseEntity<OtimizacaoResponse> otimizacao(
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer ano) {
        Long userId = SecurityUtils.getCurrentUserId();
        int m = mes != null ? mes : LocalDate.now().getMonthValue();
        int a = ano != null ? ano : LocalDate.now().getYear();
        return ResponseEntity.ok(otimizacaoUseCase.execute(userId, m, a));
    }
}
