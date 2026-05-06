package com.extratoPopular.application.dto;

import java.util.List;

public record ParseResult(List<TransacaoRaw> transacoes, int erros) {
}
