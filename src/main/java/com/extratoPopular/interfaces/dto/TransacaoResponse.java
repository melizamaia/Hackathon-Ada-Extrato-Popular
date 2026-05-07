package com.extratoPopular.interfaces.dto;

import com.extratoPopular.domain.enums.Categoria;
import com.extratoPopular.domain.enums.TipoTransacao;
import com.extratoPopular.domain.model.Transacao;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Dados de uma transação importada")
public record TransacaoResponse(

        @Schema(description = "ID da transação", example = "42")
        Long id,

        @Schema(description = "Data da transação", example = "2024-06-15")
        LocalDate data,

        @Schema(description = "Valor da transação (negativo = débito)", example = "-150.00")
        BigDecimal valor,

        @Schema(description = "Descrição original da transação", example = "IFOOD*RESTAURANTE")
        String descricao,

        @Schema(description = "Categoria inferida automaticamente", example = "ALIMENTACAO")
        Categoria categoria,

        @Schema(description = "Tipo da transação", example = "DEBITO")
        TipoTransacao tipo
) {
    public static TransacaoResponse de(Transacao t) {
        return new TransacaoResponse(t.getId(), t.getData(), t.getValor(),
                t.getDescricao(), t.getCategoria(), t.getTipo());
    }
}
