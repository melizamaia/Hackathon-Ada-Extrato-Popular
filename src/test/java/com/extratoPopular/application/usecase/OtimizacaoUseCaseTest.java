package com.extratoPopular.application.usecase;

import com.extratoPopular.domain.enums.Categoria;
import com.extratoPopular.domain.enums.FonteImportacao;
import com.extratoPopular.domain.enums.TipoTransacao;
import com.extratoPopular.domain.exception.UsuarioNaoAutenticadoException;
import com.extratoPopular.domain.model.Orcamento;
import com.extratoPopular.domain.model.Transacao;
import com.extratoPopular.domain.model.User;
import com.extratoPopular.application.service.otimizacao.OtimizacaoStrategy;
import com.extratoPopular.application.service.otimizacao.OtimizacaoStrategyFactory;
import com.extratoPopular.application.service.otimizacao.ResultadoOtimizacao;
import com.extratoPopular.infrastructure.persistence.OrcamentoRepository;
import com.extratoPopular.infrastructure.persistence.TransacaoRepository;
import com.extratoPopular.infrastructure.persistence.UserRepository;
import com.extratoPopular.interfaces.dto.OtimizacaoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtimizacaoUseCaseTest {

    @Mock private TransacaoRepository   transacaoRepository;
    @Mock private OrcamentoRepository   orcamentoRepository;
    @Mock private UserRepository        userRepository;
    @Mock private OtimizacaoStrategyFactory strategyFactory;
    @Mock private OtimizacaoStrategy    mockStrategy;

    @InjectMocks
    private OtimizacaoUseCase useCase;

    private static final Long USER_ID = 1L;
    private static final int MES = 5;
    private static final int ANO = 2026;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(USER_ID);
        user.setNome("Teste");
        user.setEmail("teste@email.com");
        user.setSenha("senha");
        user.setRendaMensal(new BigDecimal("3000.00"));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        lenient().when(strategyFactory.get(any()))
                .thenReturn(mockStrategy);
        lenient().when(mockStrategy.otimizar(any()))
                .thenReturn(new ResultadoOtimizacao(List.of(), BigDecimal.ZERO, "KNAPSACK", "desc"));
        lenient().when(mockStrategy.getNome()).thenReturn("KNAPSACK");
        lenient().when(mockStrategy.getDescricao()).thenReturn("desc");
    }

    private Transacao debito(BigDecimal valor, Categoria categoria) {
        Transacao t = new Transacao();
        t.setUserId(USER_ID);
        t.setData(LocalDate.of(ANO, MES, 10));
        t.setValor(valor.negate());
        t.setDescricao("desc");
        t.setCategoria(categoria);
        t.setTipo(TipoTransacao.DEBITO);
        t.setFonte(FonteImportacao.CSV);
        t.setHash("h" + Math.random());
        return t;
    }

    private Transacao credito(BigDecimal valor, Categoria categoria) {
        Transacao t = new Transacao();
        t.setUserId(USER_ID);
        t.setData(LocalDate.of(ANO, MES, 10));
        t.setValor(valor);
        t.setDescricao("desc");
        t.setCategoria(categoria);
        t.setTipo(TipoTransacao.CREDITO);
        t.setFonte(FonteImportacao.CSV);
        t.setHash("h" + Math.random());
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
    void deve_retornar_estrutura_vazia_quando_sem_transacoes() {
        when(transacaoRepository.findAllByUserIdAndDataBetween(anyLong(), any(), any()))
                .thenReturn(List.of());
        when(orcamentoRepository.findByUserIdAndMesAndAno(anyLong(), anyInt(), anyInt()))
                .thenReturn(List.of());

        OtimizacaoResponse response = useCase.execute(USER_ID, MES, ANO, null);

        assertTrue(response.gastosAcimaOrcamento().isEmpty());
        assertTrue(response.categoriasSeemOrcamento().isEmpty());
        assertEquals(BigDecimal.ZERO, response.economiasPotenciais());
    }

    @Test
    void deve_identificar_categoria_com_gasto_acima_do_orcamento() {
        Transacao d = debito(new BigDecimal("600.00"), Categoria.ALIMENTACAO);
        Orcamento o = orcamento(Categoria.ALIMENTACAO, new BigDecimal("400.00"));

        when(transacaoRepository.findAllByUserIdAndDataBetween(anyLong(), any(), any()))
                .thenReturn(List.of(d));
        when(orcamentoRepository.findByUserIdAndMesAndAno(anyLong(), anyInt(), anyInt()))
                .thenReturn(List.of(o));

        OtimizacaoResponse response = useCase.execute(USER_ID, MES, ANO, null);

        assertEquals(1, response.gastosAcimaOrcamento().size());
        assertEquals("ALIMENTACAO", response.gastosAcimaOrcamento().get(0).categoria());
        assertEquals(new BigDecimal("200.00"), response.gastosAcimaOrcamento().get(0).excesso());
        assertEquals(new BigDecimal("200.00"), response.economiasPotenciais());
    }

    @Test
    void nao_deve_listar_categoria_dentro_do_orcamento_como_excesso() {
        Transacao d = debito(new BigDecimal("300.00"), Categoria.ALIMENTACAO);
        Orcamento o = orcamento(Categoria.ALIMENTACAO, new BigDecimal("400.00"));

        when(transacaoRepository.findAllByUserIdAndDataBetween(anyLong(), any(), any()))
                .thenReturn(List.of(d));
        when(orcamentoRepository.findByUserIdAndMesAndAno(anyLong(), anyInt(), anyInt()))
                .thenReturn(List.of(o));

        OtimizacaoResponse response = useCase.execute(USER_ID, MES, ANO, null);

        assertTrue(response.gastosAcimaOrcamento().isEmpty());
        assertEquals(BigDecimal.ZERO, response.economiasPotenciais());
    }

    @Test
    void deve_identificar_categoria_com_gasto_mas_sem_orcamento() {
        Transacao d = debito(new BigDecimal("150.00"), Categoria.LAZER);

        when(transacaoRepository.findAllByUserIdAndDataBetween(anyLong(), any(), any()))
                .thenReturn(List.of(d));
        when(orcamentoRepository.findByUserIdAndMesAndAno(anyLong(), anyInt(), anyInt()))
                .thenReturn(List.of());

        OtimizacaoResponse response = useCase.execute(USER_ID, MES, ANO, null);

        assertTrue(response.categoriasSeemOrcamento().contains("LAZER"));
    }

    @Test
    void nao_deve_incluir_em_semOrcamento_categoria_que_tem_orcamento_definido() {
        Transacao d = debito(new BigDecimal("150.00"), Categoria.ALIMENTACAO);
        Orcamento o = orcamento(Categoria.ALIMENTACAO, new BigDecimal("300.00"));

        when(transacaoRepository.findAllByUserIdAndDataBetween(anyLong(), any(), any()))
                .thenReturn(List.of(d));
        when(orcamentoRepository.findByUserIdAndMesAndAno(anyLong(), anyInt(), anyInt()))
                .thenReturn(List.of(o));

        OtimizacaoResponse response = useCase.execute(USER_ID, MES, ANO, null);

        assertFalse(response.categoriasSeemOrcamento().contains("ALIMENTACAO"));
    }

    @Test
    void deve_calcular_percentual_renda_comprometida() {
        Transacao d = debito(new BigDecimal("1500.00"), Categoria.MORADIA);

        when(transacaoRepository.findAllByUserIdAndDataBetween(anyLong(), any(), any()))
                .thenReturn(List.of(d));
        when(orcamentoRepository.findByUserIdAndMesAndAno(anyLong(), anyInt(), anyInt()))
                .thenReturn(List.of());

        OtimizacaoResponse response = useCase.execute(USER_ID, MES, ANO, null);

        assertEquals(new BigDecimal("50.00"), response.percentualRendaComprometida());
    }

    @Test
    void deve_calcular_saldo_corretamente() {
        Transacao c = credito(new BigDecimal("3000.00"), Categoria.SALARIO);
        Transacao d = debito(new BigDecimal("500.00"), Categoria.ALIMENTACAO);

        when(transacaoRepository.findAllByUserIdAndDataBetween(anyLong(), any(), any()))
                .thenReturn(List.of(c, d));
        when(orcamentoRepository.findByUserIdAndMesAndAno(anyLong(), anyInt(), anyInt()))
                .thenReturn(List.of());

        OtimizacaoResponse response = useCase.execute(USER_ID, MES, ANO, null);

        assertEquals(new BigDecimal("2500.00"), response.saldoMensal());
    }

    @Test
    void deve_incluir_mes_e_ano_na_resposta() {
        when(transacaoRepository.findAllByUserIdAndDataBetween(anyLong(), any(), any()))
                .thenReturn(List.of());
        when(orcamentoRepository.findByUserIdAndMesAndAno(anyLong(), anyInt(), anyInt()))
                .thenReturn(List.of());

        OtimizacaoResponse response = useCase.execute(USER_ID, MES, ANO, null);

        assertEquals(MES, response.mes());
        assertEquals(ANO, response.ano());
    }

    @Test
    void deve_lancar_excecao_quando_usuario_nao_encontrado() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(UsuarioNaoAutenticadoException.class,
                () -> useCase.execute(USER_ID, MES, ANO, null));
    }

    @Test
    void deve_somar_economias_de_multiplas_categorias_acima_do_limite() {
        Transacao d1 = debito(new BigDecimal("600.00"), Categoria.ALIMENTACAO);
        Transacao d2 = debito(new BigDecimal("250.00"), Categoria.TRANSPORTE);
        Orcamento o1 = orcamento(Categoria.ALIMENTACAO, new BigDecimal("400.00")); // excesso: 200
        Orcamento o2 = orcamento(Categoria.TRANSPORTE,  new BigDecimal("200.00")); // excesso: 50

        when(transacaoRepository.findAllByUserIdAndDataBetween(anyLong(), any(), any()))
                .thenReturn(List.of(d1, d2));
        when(orcamentoRepository.findByUserIdAndMesAndAno(anyLong(), anyInt(), anyInt()))
                .thenReturn(List.of(o1, o2));

        OtimizacaoResponse response = useCase.execute(USER_ID, MES, ANO, null);

        assertEquals(2, response.gastosAcimaOrcamento().size());
        assertEquals(new BigDecimal("250.00"), response.economiasPotenciais());
    }
}
