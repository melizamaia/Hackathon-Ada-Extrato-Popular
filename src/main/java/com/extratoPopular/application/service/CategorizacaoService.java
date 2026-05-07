package com.extratoPopular.application.service;

import com.extratoPopular.domain.enums.Categoria;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CategorizacaoService {

    // LinkedHashMap preserva a ordem de inserção — TRANSFERENCIA antes de OUTROS é intencional
    private static final Map<Categoria, List<String>> REGRAS = new LinkedHashMap<>();

    static {
        REGRAS.put(Categoria.ALIMENTACAO,   List.of("ifood", "restaurante", "mercado", "supermercado", "padaria", "açougue"));
        REGRAS.put(Categoria.TRANSPORTE,    List.of("uber", "99", "onibus", "metro", "combustivel", "posto", "estacionamento"));
        REGRAS.put(Categoria.SAUDE,         List.of("farmacia", "hospital", "clinica", "laboratorio", "drogaria"));
        REGRAS.put(Categoria.EDUCACAO,      List.of("escola", "faculdade", "curso", "livraria", "mensalidade"));
        REGRAS.put(Categoria.LAZER,         List.of("netflix", "spotify", "cinema", "lazer", "teatro", "streaming"));
        REGRAS.put(Categoria.MORADIA,       List.of("aluguel", "condominio", "luz", "agua", "gas", "internet", "telefone"));
        REGRAS.put(Categoria.SALARIO,       List.of("salario", "pagamento folha", "renda"));
        REGRAS.put(Categoria.TRANSFERENCIA, List.of("transferencia", "pix", "ted", "doc"));
    }

    public Categoria categorizar(String descricao) {
        if (descricao == null || descricao.isBlank()) {
            return Categoria.OUTROS;
        }

        String normalizada = descricao.toLowerCase().trim();

        for (Map.Entry<Categoria, List<String>> entry : REGRAS.entrySet()) {
            for (String palavra : entry.getValue()) {
                if (normalizada.contains(palavra)) {
                    return entry.getKey();
                }
            }
        }

        return Categoria.OUTROS;
    }
}
