package com.extratoPopular.infrastructure.ai;

import com.extratoPopular.domain.enums.Categoria;
import com.extratoPopular.domain.enums.FonteImportacao;
import com.extratoPopular.domain.enums.TipoTransacao;
import com.extratoPopular.domain.model.Transacao;
import com.extratoPopular.infrastructure.persistence.TransacaoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinancialContextServiceImplTest {

    @Mock
    private TransacaoRepository transacaoRepository;

    @InjectMocks
    private FinancialContextServiceImpl service;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Transacao criarTransacao(LocalDate data, TipoTransacao tipo,
                                     Categoria categoria, BigDecimal valor,
                                     String descricao) {
        Transacao t = new Transacao();
        t.setUserId(1L);
        t.setData(data);
        t.setTipo(tipo);
        t.setCategoria(categoria);
        t.setValor(valor);
        t.setDescricao(descricao);
        t.setFonte(FonteImportacao.CSV);
        t.setHash("hash-" + descricao);
        return t;
    }

    // -------------------------------------------------------------------------
    // Sem transações
    // -------------------------------------------------------------------------

    @Test
    void deve_retornar_mensagem_sem_dados_quando_repositorio_vazio() {
        when(transacaoRepository.findAllByUserId(1L)).thenReturn(Collections.emptyList());

        String resultado = service.buildContext(1L);

        assertEquals("Nenhuma transação encontrada nos últimos 90 dias.", resultado);
    }

    @Test
    void deve_retornar_mensagem_sem_dados_quando_todas_transacoes_sao_antigas() {
        Transacao antiga = criarTransacao(
                LocalDate.now().minusDays(100),
                TipoTransacao.DEBITO, Categoria.ALIMENTACAO,
                new BigDecimal("50.00"), "Mercado antigo");
        when(transacaoRepository.findAllByUserId(1L)).thenReturn(List.of(antiga));

        String resultado = service.buildContext(1L);

        assertEquals("Nenhuma transação encontrada nos últimos 90 dias.", resultado);
    }

    // -------------------------------------------------------------------------
    // Contexto com dados
    // -------------------------------------------------------------------------

    @Test
    void deve_incluir_cabecalho_com_periodo() {
        Transacao t = criarTransacao(LocalDate.now().minusDays(10),
                TipoTransacao.DEBITO, Categoria.ALIMENTACAO,
                new BigDecimal("100.00"), "Supermercado");
        when(transacaoRepository.findAllByUserId(1L)).thenReturn(List.of(t));

        String resultado = service.buildContext(1L);

        assertTrue(resultado.contains("CONTEXTO FINANCEIRO"));
        assertTrue(resultado.contains("Período:"));
    }

    @Test
    void deve_incluir_colunas_da_tabela() {
        Transacao t = criarTransacao(LocalDate.now().minusDays(5),
                TipoTransacao.DEBITO, Categoria.TRANSPORTE,
                new BigDecimal("25.50"), "Uber");
        when(transacaoRepository.findAllByUserId(1L)).thenReturn(List.of(t));

        String resultado = service.buildContext(1L);

        assertTrue(resultado.contains("DATA"));
        assertTrue(resultado.contains("CATEGORIA"));
        assertTrue(resultado.contains("TIPO"));
        assertTrue(resultado.contains("VALOR"));
        assertTrue(resultado.contains("DESCRIÇÃO"));
    }

    @Test
    void deve_incluir_dados_da_transacao_no_contexto() {
        Transacao t = criarTransacao(LocalDate.now().minusDays(3),
                TipoTransacao.DEBITO, Categoria.ALIMENTACAO,
                new BigDecimal("80.00"), "iFood");
        when(transacaoRepository.findAllByUserId(1L)).thenReturn(List.of(t));

        String resultado = service.buildContext(1L);

        assertTrue(resultado.contains("ALIMENTACAO"));
        assertTrue(resultado.contains("DEBITO"));
        assertTrue(resultado.contains("80,00"));
        assertTrue(resultado.contains("iFood"));
    }

    @Test
    void deve_calcular_total_receitas_e_despesas_no_rodape() {
        List<Transacao> transacoes = List.of(
                criarTransacao(LocalDate.now().minusDays(5),
                        TipoTransacao.CREDITO, Categoria.SALARIO,
                        new BigDecimal("3000.00"), "Salário"),
                criarTransacao(LocalDate.now().minusDays(3),
                        TipoTransacao.DEBITO, Categoria.ALIMENTACAO,
                        new BigDecimal("500.00"), "Mercado")
        );
        when(transacaoRepository.findAllByUserId(1L)).thenReturn(transacoes);

        String resultado = service.buildContext(1L);

        assertTrue(resultado.contains("Total receitas"));
        assertTrue(resultado.contains("3000,00"));
        assertTrue(resultado.contains("Total despesas"));
        assertTrue(resultado.contains("500,00"));
        assertTrue(resultado.contains("Saldo"));
    }

    @Test
    void deve_calcular_saldo_positivo_corretamente() {
        List<Transacao> transacoes = List.of(
                criarTransacao(LocalDate.now().minusDays(5),
                        TipoTransacao.CREDITO, Categoria.SALARIO,
                        new BigDecimal("2000.00"), "Salário"),
                criarTransacao(LocalDate.now().minusDays(3),
                        TipoTransacao.DEBITO, Categoria.MORADIA,
                        new BigDecimal("800.00"), "Aluguel")
        );
        when(transacaoRepository.findAllByUserId(1L)).thenReturn(transacoes);

        String resultado = service.buildContext(1L);

        assertTrue(resultado.contains("1200,00"));
    }

    @Test
    void deve_exibir_top3_categorias_de_gastos() {
        List<Transacao> transacoes = List.of(
                criarTransacao(LocalDate.now().minusDays(1), TipoTransacao.DEBITO,
                        Categoria.ALIMENTACAO, new BigDecimal("300.00"), "Mercado"),
                criarTransacao(LocalDate.now().minusDays(2), TipoTransacao.DEBITO,
                        Categoria.TRANSPORTE, new BigDecimal("200.00"), "Uber"),
                criarTransacao(LocalDate.now().minusDays(3), TipoTransacao.DEBITO,
                        Categoria.LAZER, new BigDecimal("150.00"), "Cinema"),
                criarTransacao(LocalDate.now().minusDays(4), TipoTransacao.DEBITO,
                        Categoria.SAUDE, new BigDecimal("100.00"), "Farmácia")
        );
        when(transacaoRepository.findAllByUserId(1L)).thenReturn(transacoes);

        String resultado = service.buildContext(1L);

        assertTrue(resultado.contains("Top 3 categorias de gastos"));
        // Top 3 deve estar na seção de resumo
        String secaoTop3 = resultado.substring(resultado.indexOf("Top 3 categorias de gastos"));
        assertTrue(secaoTop3.contains("ALIMENTACAO"));
        assertTrue(secaoTop3.contains("TRANSPORTE"));
        assertTrue(secaoTop3.contains("LAZER"));
        assertFalse(secaoTop3.contains("SAUDE")); // SAUDE é 4º, não deve aparecer no top 3
    }

    @Test
    void deve_filtrar_transacoes_com_mais_de_90_dias() {
        List<Transacao> transacoes = List.of(
                criarTransacao(LocalDate.now().minusDays(10), TipoTransacao.DEBITO,
                        Categoria.ALIMENTACAO, new BigDecimal("100.00"), "Recente"),
                criarTransacao(LocalDate.now().minusDays(91), TipoTransacao.DEBITO,
                        Categoria.LAZER, new BigDecimal("999.00"), "Antiga demais")
        );
        when(transacaoRepository.findAllByUserId(1L)).thenReturn(transacoes);

        String resultado = service.buildContext(1L);

        assertTrue(resultado.contains("Recente"));
        assertFalse(resultado.contains("Antiga demais"));
    }

    @Test
    void deve_limitar_resultado_a_4000_caracteres() {
        List<Transacao> transacoes = new java.util.ArrayList<>();
        for (int i = 0; i < 50; i++) {
            transacoes.add(criarTransacao(
                    LocalDate.now().minusDays(i % 89 + 1),
                    TipoTransacao.DEBITO, Categoria.OUTROS,
                    new BigDecimal("10.00"),
                    "Descrição muito longa para testar limite de caracteres - item " + i));
        }
        when(transacaoRepository.findAllByUserId(1L)).thenReturn(transacoes);

        String resultado = service.buildContext(1L);

        assertTrue(resultado.length() <= 4000);
    }

    @Test
    void deve_chamar_repositorio_com_userId_correto() {
        when(transacaoRepository.findAllByUserId(42L)).thenReturn(Collections.emptyList());

        service.buildContext(42L);

        verify(transacaoRepository).findAllByUserId(42L);
        verify(transacaoRepository, never()).findAllByUserId(1L);
    }

    @Test
    void deve_incluir_total_de_transacoes_no_cabecalho() {
        List<Transacao> transacoes = List.of(
                criarTransacao(LocalDate.now().minusDays(1), TipoTransacao.CREDITO,
                        Categoria.SALARIO, new BigDecimal("1000.00"), "Salário"),
                criarTransacao(LocalDate.now().minusDays(2), TipoTransacao.DEBITO,
                        Categoria.ALIMENTACAO, new BigDecimal("200.00"), "Mercado")
        );
        when(transacaoRepository.findAllByUserId(1L)).thenReturn(transacoes);

        String resultado = service.buildContext(1L);

        assertTrue(resultado.contains("Total de transações: 2"));
    }
}
