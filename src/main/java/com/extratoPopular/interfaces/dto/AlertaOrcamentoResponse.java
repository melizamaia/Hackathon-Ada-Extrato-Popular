package com.extratoPopular.interfaces.dto;

import com.extratoPopular.domain.enums.NivelAlerta;

import java.math.BigDecimal;

public record AlertaOrcamentoResponse(
        String categoria,
        NivelAlerta nivel,
        BigDecimal valorLimite,
        BigDecimal valorGasto,
        BigDecimal percentual,
        String mensagem
) {}
