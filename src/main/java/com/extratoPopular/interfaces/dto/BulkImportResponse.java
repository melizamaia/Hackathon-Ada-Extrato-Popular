package com.extratoPopular.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Resultado da importação em lote de transações")
public record BulkImportResponse(

        @Schema(description = "Número de transações importadas com sucesso", example = "42")
        int importadas,

        @Schema(description = "Número de transações ignoradas por duplicidade", example = "3")
        int duplicatas,

        @Schema(description = "Número de transações com erro durante o processamento", example = "1")
        int erros,

        @Schema(description = "Transações importadas com sucesso nesta operação")
        List<TransacaoResponse> transacoes
) {
}
