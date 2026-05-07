package com.extratoPopular.application.usecase;

import com.extratoPopular.domain.enums.Categoria;
import com.extratoPopular.domain.enums.FonteImportacao;
import com.extratoPopular.domain.enums.TipoTransacao;
import com.extratoPopular.domain.model.Orcamento;
import com.extratoPopular.domain.model.Transacao;
import com.extratoPopular.infrastructure.persistence.OrcamentoRepository;
import com.extratoPopular.infrastructure.persistence.TransacaoRepository;
import com.extratoPopular.interfaces.dto.ResumoResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumoTransacoesUseCaseTest {

    @Mock private TransacaoRepository transacaoRepository;
    @Mock private OrcamentoRepository orcamentoRepository;

    @InjectMocks
    private ResumoTransacoesUseCase useCase;

    private static final Long USER_ID = 1L;
    private static final int MES = 5;
    private static final int ANO = 2026;

    private Transacao transacao(BigDecimal valor, Categoria categoria, TipoTransacao tipo) {
        Transacao t = new Transacao();
        t.setId(1L);
        t.setUserId(USER_ID);
        t.setData(LocalDate.of(ANO, MES, 10));
        t.setValor(valor);
        t.setDescricao("desc");
        t.setCategoria(categoria);
        t.setTipo(tipo);
        t.setFonte(FonteImportacao.CSV);
        t.setHash("hash");
        return t;
    }

    private Orcamento orcamento(Categoria categoria, BigDecimal limite) {
        Orcamento o = new Orcamento();
        o.setId(1L);
        o.setUserId(USER_ID);
        o.setCategoria(categoria);
        o.setValorLimite(limite);
        o.setMes(MES);
        o.setAno(ANO);
        return o;
    }

    @Test
    void deve_retornar_zeros_quando_sem_transacoes() {
        when(transacaoRepository.findAllByUserIdAndDataBetween(anyLong(), any(), any()))
                .thenReturn(List.of());
        when(orcamentoRepository.findByUserIdAndMesAndAno(anyLong(), anyInt(), anyInt()))
                .thenReturn(List.of());

        ResumoResponse response = useCase.execute(USER_ID, MES, ANO);

        assertEquals(BigDecimal.ZERO, response.totalReceitas());
        assertEquals(BigDecimal.ZERO, response.totalDespesas());
        assertEquals(BigDecimal.ZERO, response.saldo());
        assertEquals(0, response.totalTransacoes());
        assertTrue(response.gastosPorCategoria().isEmpty());
        assertTrue(response.alertas().isEmpty());
    }

    @Test
    void deve_somar_apenas_creditos_em_totalReceitas() {
        Transacao c1 = transacao(new BigDecimal("3000.00"), Categoria.SALARIO, TipoTransacao.CREDITO);
        Transacao c2 = transacao(new BigDecimal("500.00"), Categoria.OUTROS, TipoTransacao.CREDITO);

        when(transacaoRepository.findAllByUserIdAndDataBetween(anyLong(), any(), any()))
                .thenReturn(List.of(c1, c2));
        when(orcamentoRepository.findByUserIdAndMesAndAno(anyLong(), anyInt(), anyInt()))
                .thenReturn(List.of());

        ResumoResponse response = useCase.execute(USER_ID, MES, ANO);

        assertEquals(new BigDecimal("3500.00"), response.totalReceitas());
        assertEquals(BigDecimal.ZERO, response.totalDespesas());
        assertEquals(new BigDecimal("3500.00"), response.saldo());
    }

    @Test
    void deve_somar_valores_absolutos_dos_debitos_em_totalDespesas() {
        Transacao d1 = transacao(new BigDecimal("-150.00"), Categoria.ALIMENTACAO, TipoTransacao.DEBITO);
        Transacao d2 = transacao(new BigDecimal("-80.00"), Categoria.TRANSPORTE, TipoTransacao.DEBITO);

        when(transacaoRepository.findAllByUserIdAndDataBetween(anyLong(), any(), any()))
                .thenReturn(List.of(d1, d2));
        when(orcamentoRepository.findByUserIdAndMesAndAno(anyLong(), anyInt(), anyInt()))
                .thenReturn(List.of());

        ResumoResponse response = useCase.execute(USER_ID, MES, ANO);

        assertEquals(BigDecimal.ZERO, response.totalReceitas());
        assertEquals(new BigDecimal("230.00"), response.totalDespesas());
        assertEquals(new BigDecimal("-230.00"), response.saldo());
    }

    @Test
    void deve_calcular_saldo_correto_com_receitas_e_despesas() {
        Transacao credito = transacao(new BigDecimal("3000.00"), Categoria.SALARIO, TipoTransacao.CREDITO);
        Transacao debito  = transacao(new BigDecimal("-500.00"), Categoria.ALIMENTACAO, TipoTransacao.DEBITO);

        when(transacaoRepository.findAllByUserIdAndDataBetween(anyLong(), any(), any()))
                .thenReturn(List.of(credito, debito));
        when(orcamentoRepository.findByUserIdAndMesAndAno(anyLong(), anyInt(), anyInt()))
                .thenReturn(List.of());

        ResumoResponse response = useCase.execute(USER_ID, MES, ANO);

        assertEquals(new BigDecimal("3000.00"), response.totalReceitas());
        assertEquals(new BigDecimal("500.00"), response.totalDespesas());
        assertEquals(new BigDecimal("2500.00"), response.saldo());
        assertEquals(2, response.totalTransacoes());
    }

    @Test
    void deve_agrupar_gastos_por_categoria() {
        Transacao t1 = transacao(new BigDecimal("-100.00"), Categoria.ALIMENTACAO, TipoTransacao.DEBITO);
        Transacao t2 = transacao(new BigDecimal("-50.00"),  Categoria.ALIMENTACAO, TipoTransacao.DEBITO);
        Transacao t3 = transacao(new BigDecimal("-80.00"),  Categoria.TRANSPORTE,  TipoTransacao.DEBITO);

        when(transacaoRepository.findAllByUserIdAndDataBetween(anyLong(), any(), any()))
                .thenReturn(List.of(t1, t2, t3));
        when(orcamentoRepository.findByUserIdAndMesAndAno(anyLong(), anyInt(), anyInt()))
                .thenReturn(List.of());

        ResumoResponse response = useCase.execute(USER_ID, MES, ANO);

        assertEquals(new BigDecimal("150.00"), response.gastosPorCategoria().get("ALIMENTACAO"));
        assertEquals(new BigDecimal("80.00"),  response.gastosPorCategoria().get("TRANSPORTE"));
        assertNull(response.gastosPorCategoria().get("SAUDE"));
    }

    @Test
    void deve_gerar_alerta_INFO_quando_gasto_atinge_70_porcento() {
        Transacao debito = transacao(new BigDecimal("-350.00"), Categoria.ALIMENTACAO, TipoTransacao.DEBITO);
        Orcamento orc    = orcamento(Categoria.ALIMENTACAO, new BigDecimal("500.00")); // 70%

        when(transacaoRepository.findAllByUserIdAndDataBetween(anyLong(), any(), any()))
                .thenReturn(List.of(debito));
        when(orcamentoRepository.findByUserIdAndMesAndAno(anyLong(), anyInt(), anyInt()))
                .thenReturn(List.of(orc));

        ResumoResponse response = useCase.execute(USER_ID, MES, ANO);

        assertEquals(1, response.alertas().size());
        assertEquals("INFO", response.alertas().get(0).nivel().name());
        assertEquals("ALIMENTACAO", response.alertas().get(0).categoria());
    }

    @Test
    void deve_gerar_alerta_AVISO_quando_gasto_atinge_90_porcento() {
        Transacao debito = transacao(new BigDecimal("-450.00"), Categoria.ALIMENTACAO, TipoTransacao.DEBITO);
        Orcamento orc    = orcamento(Categoria.ALIMENTACAO, new BigDecimal("500.00")); // 90%

        when(transacaoRepository.findAllByUserIdAndDataBetween(anyLong(), any(), any()))
                .thenReturn(List.of(debito));
        when(orcamentoRepository.findByUserIdAndMesAndAno(anyLong(), anyInt(), anyInt()))
                .thenReturn(List.of(orc));

        ResumoResponse response = useCase.execute(USER_ID, MES, ANO);

        assertEquals(1, response.alertas().size());
        assertEquals("AVISO", response.alertas().get(0).nivel().name());
    }

    @Test
    void deve_gerar_alerta_CRITICO_quando_gasto_supera_orcamento() {
        Transacao debito = transacao(new BigDecimal("-600.00"), Categoria.ALIMENTACAO, TipoTransacao.DEBITO);
        Orcamento orc    = orcamento(Categoria.ALIMENTACAO, new BigDecimal("500.00")); // 120%

        when(transacaoRepository.findAllByUserIdAndDataBetween(anyLong(), any(), any()))
                .thenReturn(List.of(debito));
        when(orcamentoRepository.findByUserIdAndMesAndAno(anyLong(), anyInt(), anyInt()))
                .thenReturn(List.of(orc));

        ResumoResponse response = useCase.execute(USER_ID, MES, ANO);

        assertEquals(1, response.alertas().size());
        assertEquals("CRITICO", response.alertas().get(0).nivel().name());
    }

    @Test
    void nao_deve_gerar_alerta_quando_gasto_abaixo_de_70_porcento() {
        Transacao debito = transacao(new BigDecimal("-300.00"), Categoria.ALIMENTACAO, TipoTransacao.DEBITO);
        Orcamento orc    = orcamento(Categoria.ALIMENTACAO, new BigDecimal("500.00")); // 60%

        when(transacaoRepository.findAllByUserIdAndDataBetween(anyLong(), any(), any()))
                .thenReturn(List.of(debito));
        when(orcamentoRepository.findByUserIdAndMesAndAno(anyLong(), anyInt(), anyInt()))
                .thenReturn(List.of(orc));

        ResumoResponse response = useCase.execute(USER_ID, MES, ANO);

        assertTrue(response.alertas().isEmpty());
    }

    @Test
    void deve_incluir_mes_e_ano_na_resposta() {
        when(transacaoRepository.findAllByUserIdAndDataBetween(anyLong(), any(), any()))
                .thenReturn(List.of());
        when(orcamentoRepository.findByUserIdAndMesAndAno(anyLong(), anyInt(), anyInt()))
                .thenReturn(List.of());

        ResumoResponse response = useCase.execute(USER_ID, MES, ANO);

        assertEquals(MES, response.mes());
        assertEquals(ANO, response.ano());
    }

    @Test
    void nao_deve_gerar_alerta_para_categoria_sem_gastos() {
        Orcamento orc = orcamento(Categoria.EDUCACAO, new BigDecimal("300.00"));

        when(transacaoRepository.findAllByUserIdAndDataBetween(anyLong(), any(), any()))
                .thenReturn(List.of());
        when(orcamentoRepository.findByUserIdAndMesAndAno(anyLong(), anyInt(), anyInt()))
                .thenReturn(List.of(orc));

        ResumoResponse response = useCase.execute(USER_ID, MES, ANO);

        assertTrue(response.alertas().isEmpty());
    }
}
