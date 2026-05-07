package com.extratoPopular.application.service;

import com.extratoPopular.domain.enums.Categoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CategorizacaoServiceTest {

    private CategorizacaoService categorizacaoService;

    @BeforeEach
    void setUp() {
        categorizacaoService = new CategorizacaoService();
    }

    @ParameterizedTest(name = "[{index}] \"{0}\" → ALIMENTACAO")
    @ValueSource(strings = {
        "IFOOD*RESTAURANTE PIZZA",
        "SUPERMERCADO EXTRA LTDA",
        "PADARIA DO ZÉ",
        "AÇOUGUE CENTRAL",
        "Mercado Livre Alimentação",
        "restaurante casa da vovó"
    })
    void deve_categorizar_como_ALIMENTACAO(String descricao) {
        assertEquals(Categoria.ALIMENTACAO, categorizacaoService.categorizar(descricao));
    }

    @ParameterizedTest(name = "[{index}] \"{0}\" → TRANSPORTE")
    @ValueSource(strings = {
        "UBER TRIP 12345",
        "99 TAXI CORRIDA",
        "POSTO IPIRANGA COMBUSTIVEL",
        "METRO SP BILHETE UNICO",
        "ESTACIONAMENTO SHOPPING",
        "ONIBUS INTERMUNICIPAL"
    })
    void deve_categorizar_como_TRANSPORTE(String descricao) {
        assertEquals(Categoria.TRANSPORTE, categorizacaoService.categorizar(descricao));
    }

    @ParameterizedTest(name = "[{index}] \"{0}\" → SAUDE")
    @ValueSource(strings = {
        "FARMACIA DROGASIL",
        "HOSPITAL ALBERT EINSTEIN",
        "CLINICA MEDICA SÃO LUCAS",
        "LABORATORIO FLEURY",
        "DROGARIA SÃO PAULO"
    })
    void deve_categorizar_como_SAUDE(String descricao) {
        assertEquals(Categoria.SAUDE, categorizacaoService.categorizar(descricao));
    }

    @ParameterizedTest(name = "[{index}] \"{0}\" → EDUCACAO")
    @ValueSource(strings = {
        "ESCOLA ESTADUAL JOSE BONIFACIO",
        "FACULDADE ANHANGUERA MENSALIDADE",
        "CURSO DE PROGRAMACAO ALURA",
        "LIVRARIA CULTURA",
        "MENSALIDADE CURSO INGLES"
    })
    void deve_categorizar_como_EDUCACAO(String descricao) {
        assertEquals(Categoria.EDUCACAO, categorizacaoService.categorizar(descricao));
    }

    @ParameterizedTest(name = "[{index}] \"{0}\" → LAZER")
    @ValueSource(strings = {
        "NETFLIX.COM",
        "SPOTIFY PREMIUM",
        "CINEMA KINOPLEX",
        "TEATRO MUNICIPAL INGRESSO",
        "STREAMING PRIME VIDEO",
        "PARQUE LAZER FAMÍLIA"
    })
    void deve_categorizar_como_LAZER(String descricao) {
        assertEquals(Categoria.LAZER, categorizacaoService.categorizar(descricao));
    }

    @ParameterizedTest(name = "[{index}] \"{0}\" → MORADIA")
    @ValueSource(strings = {
        "ALUGUEL APTO MARÇO",
        "CONDOMINIO EDIFICIO SOLAR",
        "CONTA LUZ ENEL",
        "CONTA AGUA SABESP",
        "GAS COMGAS FATURA",
        "INTERNET VIVO FIBRA",
        "TELEFONE CLARO FATURA"
    })
    void deve_categorizar_como_MORADIA(String descricao) {
        assertEquals(Categoria.MORADIA, categorizacaoService.categorizar(descricao));
    }

    @ParameterizedTest(name = "[{index}] \"{0}\" → SALARIO")
    @ValueSource(strings = {
        "SALARIO MAIO 2024",
        "PAGAMENTO FOLHA EMPRESA XYZ",
        "RENDA MENSAL DEPOSITO"
    })
    void deve_categorizar_como_SALARIO(String descricao) {
        assertEquals(Categoria.SALARIO, categorizacaoService.categorizar(descricao));
    }

    @ParameterizedTest(name = "[{index}] \"{0}\" → TRANSFERENCIA")
    @ValueSource(strings = {
        "TRANSFERENCIA ENTRE CONTAS",
        "PIX RECEBIDO JOAO SILVA",
        "TED BRADESCO 12345",
        "DOC BANCO INTER"
    })
    void deve_categorizar_como_TRANSFERENCIA(String descricao) {
        assertEquals(Categoria.TRANSFERENCIA, categorizacaoService.categorizar(descricao));
    }

    @ParameterizedTest(name = "[{index}] \"{0}\" → OUTROS (fallback)")
    @ValueSource(strings = {
        "SAQUE CAIXA ELETRONICO",
        "COMPRA ONLINE GENÉRICA",
        "IOF OPERACAO CAMBIO",
        "TARIFA BANCARIA MENSAL",
        "XYZ PAGAMENTO DIVERSO"
    })
    void deve_categorizar_como_OUTROS_quando_semMatch(String descricao) {
        assertEquals(Categoria.OUTROS, categorizacaoService.categorizar(descricao));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void deve_categorizar_como_OUTROS_quando_descricaoNulaOuVazia(String descricao) {
        assertEquals(Categoria.OUTROS, categorizacaoService.categorizar(descricao));
    }

    @Test
    void deve_ser_case_insensitive() {
        assertEquals(Categoria.ALIMENTACAO, categorizacaoService.categorizar("ifood*restaurante"));
        assertEquals(Categoria.ALIMENTACAO, categorizacaoService.categorizar("IFOOD*RESTAURANTE"));
        assertEquals(Categoria.ALIMENTACAO, categorizacaoService.categorizar("IFood*Restaurante"));
    }

    @Test
    void deve_priorizar_ALIMENTACAO_sobre_TRANSFERENCIA_quando_mercado_e_pix_presentes() {
        // ALIMENTACAO aparece antes de TRANSFERENCIA no mapa de regras
        assertEquals(Categoria.ALIMENTACAO, categorizacaoService.categorizar("MERCADO PIX"));
    }
}
