package com.extratoPopular.application.service.otimizacao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OtimizacaoStrategiesTest {

    private GulosaOtimizacaoStrategy gulosa;
    private KnapsackOtimizacaoStrategy knapsack;
    private RoiOtimizacaoStrategy roi;

    @BeforeEach
    void setUp() {
        gulosa = new GulosaOtimizacaoStrategy();
        knapsack = new KnapsackOtimizacaoStrategy();
        roi = new RoiOtimizacaoStrategy();
    }

    private ItemOtimizacao item(String cat, double excesso, int dificuldade) {
        return new ItemOtimizacao(cat, BigDecimal.valueOf(excesso), BigDecimal.valueOf(200), BigDecimal.valueOf(200 + excesso), dificuldade);
    }

    // -------- AcaoOtimizacao record --------

    @Test
    void acao_deve_expor_todos_os_campos() {
        AcaoOtimizacao acao = new AcaoOtimizacao("ALIMENTACAO", BigDecimal.valueOf(150), 3, "Reduza R$ 150.00 em ALIMENTACAO", 1);
        assertEquals("ALIMENTACAO", acao.categoria());
        assertEquals(BigDecimal.valueOf(150), acao.economiaPotencial());
        assertEquals(3, acao.dificuldade());
        assertEquals("Reduza R$ 150.00 em ALIMENTACAO", acao.descricaoAcao());
        assertEquals(1, acao.prioridade());
    }

    // -------- GulosaOtimizacaoStrategy --------

    @Test
    void gulosa_deve_retornar_vazio_para_lista_vazia() {
        ResultadoOtimizacao resultado = gulosa.otimizar(List.of());
        assertTrue(resultado.acoes().isEmpty());
        assertEquals(BigDecimal.ZERO, resultado.totalEconomiaSelecionada());
        assertEquals("GULOSA", resultado.nomeAlgoritmo());
    }

    @Test
    void gulosa_deve_selecionar_item_que_cabe_na_capacidade() {
        var itens = List.of(item("ALIMENTACAO", 200, 5));
        ResultadoOtimizacao resultado = gulosa.otimizar(itens);
        assertEquals(1, resultado.acoes().size());
        assertEquals("ALIMENTACAO", resultado.acoes().get(0).categoria());
        assertEquals(BigDecimal.valueOf(200).stripTrailingZeros(),
                resultado.totalEconomiaSelecionada().stripTrailingZeros());
    }

    @Test
    void gulosa_deve_excluir_item_que_excede_capacidade() {
        var itens = List.of(item("LAZER", 300, 25)); // dificuldade > 20
        ResultadoOtimizacao resultado = gulosa.otimizar(itens);
        assertTrue(resultado.acoes().isEmpty());
        assertEquals(BigDecimal.ZERO, resultado.totalEconomiaSelecionada());
    }

    @Test
    void gulosa_deve_priorizar_por_eficiencia() {
        var alta = item("LAZER", 100, 2);        // eficiência 50
        var baixa = item("ALIMENTACAO", 90, 9);  // eficiência 10
        var itens = List.of(baixa, alta);
        ResultadoOtimizacao resultado = gulosa.otimizar(itens);
        assertEquals("LAZER", resultado.acoes().get(0).categoria());
    }

    @Test
    void gulosa_deve_ter_prioridade_incrementada() {
        var i1 = item("CAT1", 100, 3);
        var i2 = item("CAT2", 50, 2);
        ResultadoOtimizacao resultado = gulosa.otimizar(List.of(i1, i2));
        assertEquals(1, resultado.acoes().get(0).prioridade());
        assertEquals(2, resultado.acoes().get(1).prioridade());
    }

    @Test
    void gulosa_deve_conter_descricao() {
        assertFalse(gulosa.getDescricao().isBlank());
    }

    // -------- KnapsackOtimizacaoStrategy --------

    @Test
    void knapsack_deve_retornar_vazio_para_lista_vazia() {
        ResultadoOtimizacao resultado = knapsack.otimizar(List.of());
        assertTrue(resultado.acoes().isEmpty());
        assertEquals(BigDecimal.ZERO, resultado.totalEconomiaSelecionada());
        assertEquals("KNAPSACK", resultado.nomeAlgoritmo());
    }

    @Test
    void knapsack_deve_selecionar_melhor_combinacao() {
        var a = item("ALIMENTACAO", 200, 10);
        var b = item("LAZER", 180, 10);
        var c = item("TRANSPORTE", 50, 2);
        // Capacidade=20: A+C (250) > B+C (230) > A ou B sozinhos
        ResultadoOtimizacao resultado = knapsack.otimizar(List.of(a, b, c));
        int totalEconomia = resultado.acoes().stream()
                .mapToInt(ac -> ac.economiaPotencial().intValue())
                .sum();
        assertTrue(totalEconomia >= 250);
    }

    @Test
    void knapsack_deve_ignorar_item_que_excede_capacidade() {
        var pesado = item("MORADIA", 500, 25); // peso > 20
        ResultadoOtimizacao resultado = knapsack.otimizar(List.of(pesado));
        assertTrue(resultado.acoes().isEmpty());
    }

    @Test
    void knapsack_deve_ter_prioridade_incrementada() {
        var a = item("CAT1", 100, 5);
        var b = item("CAT2", 80, 5);
        ResultadoOtimizacao resultado = knapsack.otimizar(List.of(a, b));
        List<AcaoOtimizacao> acoes = resultado.acoes();
        for (int i = 0; i < acoes.size(); i++) {
            assertEquals(i + 1, acoes.get(i).prioridade());
        }
    }

    @Test
    void knapsack_deve_conter_descricao() {
        assertFalse(knapsack.getDescricao().isBlank());
    }

    // -------- RoiOtimizacaoStrategy --------

    @Test
    void roi_deve_retornar_vazio_para_lista_vazia() {
        ResultadoOtimizacao resultado = roi.otimizar(List.of());
        assertTrue(resultado.acoes().isEmpty());
        assertEquals(BigDecimal.ZERO, resultado.totalEconomiaSelecionada());
        assertEquals("ROI", resultado.nomeAlgoritmo());
    }

    @Test
    void roi_deve_incluir_todos_os_itens_sem_limite_de_capacidade() {
        var a = item("ALIMENTACAO", 200, 15);
        var b = item("LAZER", 180, 15);
        var c = item("TRANSPORTE", 50, 15);
        ResultadoOtimizacao resultado = roi.otimizar(List.of(a, b, c));
        assertEquals(3, resultado.acoes().size());
    }

    @Test
    void roi_deve_ordenar_por_roi_decrescente() {
        var alto = item("LAZER", 200, 2);    // roi=100
        var baixo = item("ALIMENTACAO", 200, 10); // roi=20
        ResultadoOtimizacao resultado = roi.otimizar(List.of(baixo, alto));
        assertEquals("LAZER", resultado.acoes().get(0).categoria());
    }

    @Test
    void roi_deve_ter_prioridade_incrementada() {
        var i1 = item("CAT1", 100, 5);
        var i2 = item("CAT2", 50, 5);
        ResultadoOtimizacao resultado = roi.otimizar(List.of(i1, i2));
        assertEquals(1, resultado.acoes().get(0).prioridade());
        assertEquals(2, resultado.acoes().get(1).prioridade());
    }

    @Test
    void roi_descricao_contem_texto() {
        assertTrue(roi.getDescricao().contains("ROI"));
    }

    @Test
    void roi_descricao_nao_esta_em_branco() {
        assertFalse(roi.getDescricao().isBlank());
    }

    // -------- OtimizacaoStrategyFactory --------

    @Test
    void factory_deve_retornar_knapsack_para_nome_nulo() {
        OtimizacaoStrategyFactory factory = new OtimizacaoStrategyFactory(
                List.of(knapsack, gulosa, roi));
        assertInstanceOf(KnapsackOtimizacaoStrategy.class, factory.get(null));
    }

    @Test
    void factory_deve_retornar_knapsack_para_nome_invalido() {
        OtimizacaoStrategyFactory factory = new OtimizacaoStrategyFactory(
                List.of(knapsack, gulosa, roi));
        assertInstanceOf(KnapsackOtimizacaoStrategy.class, factory.get("DESCONHECIDO"));
    }

    @Test
    void factory_deve_retornar_gulosa_pelo_nome() {
        OtimizacaoStrategyFactory factory = new OtimizacaoStrategyFactory(
                List.of(knapsack, gulosa, roi));
        assertInstanceOf(GulosaOtimizacaoStrategy.class, factory.get("GULOSA"));
    }

    @Test
    void factory_deve_retornar_roi_pelo_nome() {
        OtimizacaoStrategyFactory factory = new OtimizacaoStrategyFactory(
                List.of(knapsack, gulosa, roi));
        assertInstanceOf(RoiOtimizacaoStrategy.class, factory.get("ROI"));
    }

    @Test
    void factory_deve_aceitar_nome_em_minusculo() {
        OtimizacaoStrategyFactory factory = new OtimizacaoStrategyFactory(
                List.of(knapsack, gulosa, roi));
        assertInstanceOf(GulosaOtimizacaoStrategy.class, factory.get("gulosa"));
    }
}
