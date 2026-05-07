package com.extratoPopular.application.usecase;

import com.extratoPopular.domain.enums.Categoria;
import com.extratoPopular.domain.exception.OrcamentoDuplicadoException;
import com.extratoPopular.domain.exception.OrcamentoNaoEncontradoException;
import com.extratoPopular.domain.model.Orcamento;
import com.extratoPopular.infrastructure.persistence.OrcamentoRepository;
import com.extratoPopular.interfaces.dto.OrcamentoRequest;
import com.extratoPopular.interfaces.dto.OrcamentoResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrcamentoUseCaseTest {

    @Mock
    private OrcamentoRepository orcamentoRepository;

    @InjectMocks
    private OrcamentoUseCase useCase;

    private static final Long USER_ID = 1L;

    private OrcamentoRequest request(Categoria categoria, double limite) {
        return new OrcamentoRequest(categoria, BigDecimal.valueOf(limite), 5, 2026);
    }

    private Orcamento orcamentoSalvo(Long id, Categoria categoria, double limite) {
        Orcamento o = new Orcamento();
        o.setId(id);
        o.setUserId(USER_ID);
        o.setCategoria(categoria);
        o.setValorLimite(BigDecimal.valueOf(limite));
        o.setMes(5);
        o.setAno(2026);
        return o;
    }

    // -------------------------------------------------------------------------
    // criar
    // -------------------------------------------------------------------------

    @Test
    void deve_criar_orcamento_quando_nao_existe_duplicata() {
        when(orcamentoRepository.findByUserIdAndCategoriaAndMesAndAno(anyLong(), any(), anyInt(), anyInt()))
                .thenReturn(Optional.empty());
        when(orcamentoRepository.save(any())).thenReturn(orcamentoSalvo(1L, Categoria.ALIMENTACAO, 500));

        OrcamentoResponse response = useCase.criar(USER_ID, request(Categoria.ALIMENTACAO, 500));

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals(Categoria.ALIMENTACAO, response.categoria());
        verify(orcamentoRepository).save(any(Orcamento.class));
    }

    @Test
    void deve_persistir_userId_correto_ao_criar() {
        when(orcamentoRepository.findByUserIdAndCategoriaAndMesAndAno(anyLong(), any(), anyInt(), anyInt()))
                .thenReturn(Optional.empty());
        when(orcamentoRepository.save(any())).thenReturn(orcamentoSalvo(1L, Categoria.ALIMENTACAO, 500));

        useCase.criar(USER_ID, request(Categoria.ALIMENTACAO, 500));

        verify(orcamentoRepository).save(argThat(o -> o.getUserId().equals(USER_ID)));
    }

    @Test
    void deve_lancar_excecao_ao_criar_orcamento_duplicado() {
        when(orcamentoRepository.findByUserIdAndCategoriaAndMesAndAno(anyLong(), any(), anyInt(), anyInt()))
                .thenReturn(Optional.of(orcamentoSalvo(1L, Categoria.ALIMENTACAO, 500)));

        assertThrows(OrcamentoDuplicadoException.class,
                () -> useCase.criar(USER_ID, request(Categoria.ALIMENTACAO, 500)));

        verify(orcamentoRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // listar
    // -------------------------------------------------------------------------

    @Test
    void deve_retornar_lista_vazia_quando_sem_orcamentos() {
        when(orcamentoRepository.findByUserIdAndMesAndAno(USER_ID, 5, 2026)).thenReturn(List.of());

        List<OrcamentoResponse> lista = useCase.listar(USER_ID, 5, 2026);

        assertTrue(lista.isEmpty());
    }

    @Test
    void deve_retornar_todos_os_orcamentos_do_mes() {
        List<Orcamento> orcamentos = List.of(
                orcamentoSalvo(1L, Categoria.ALIMENTACAO, 500),
                orcamentoSalvo(2L, Categoria.TRANSPORTE,  300),
                orcamentoSalvo(3L, Categoria.LAZER,       200)
        );
        when(orcamentoRepository.findByUserIdAndMesAndAno(USER_ID, 5, 2026)).thenReturn(orcamentos);

        List<OrcamentoResponse> lista = useCase.listar(USER_ID, 5, 2026);

        assertEquals(3, lista.size());
    }

    // -------------------------------------------------------------------------
    // atualizar
    // -------------------------------------------------------------------------

    @Test
    void deve_atualizar_orcamento_existente_do_mesmo_usuario() {
        Orcamento existente = orcamentoSalvo(1L, Categoria.ALIMENTACAO, 500);
        Orcamento atualizado = orcamentoSalvo(1L, Categoria.ALIMENTACAO, 800);

        when(orcamentoRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(orcamentoRepository.save(any())).thenReturn(atualizado);

        OrcamentoResponse response = useCase.atualizar(USER_ID, 1L, request(Categoria.ALIMENTACAO, 800));

        assertEquals(BigDecimal.valueOf(800), response.valorLimite());
        verify(orcamentoRepository).save(any());
    }

    @Test
    void deve_lancar_excecao_ao_atualizar_orcamento_inexistente() {
        when(orcamentoRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(OrcamentoNaoEncontradoException.class,
                () -> useCase.atualizar(USER_ID, 99L, request(Categoria.ALIMENTACAO, 500)));
    }

    @Test
    void deve_lancar_excecao_ao_atualizar_orcamento_de_outro_usuario() {
        Orcamento outroUsuario = orcamentoSalvo(1L, Categoria.ALIMENTACAO, 500);
        outroUsuario.setUserId(99L);

        when(orcamentoRepository.findById(1L)).thenReturn(Optional.of(outroUsuario));

        assertThrows(OrcamentoNaoEncontradoException.class,
                () -> useCase.atualizar(USER_ID, 1L, request(Categoria.ALIMENTACAO, 500)));

        verify(orcamentoRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // deletar
    // -------------------------------------------------------------------------

    @Test
    void deve_deletar_orcamento_do_mesmo_usuario() {
        Orcamento existente = orcamentoSalvo(1L, Categoria.ALIMENTACAO, 500);
        when(orcamentoRepository.findById(1L)).thenReturn(Optional.of(existente));

        useCase.deletar(USER_ID, 1L);

        verify(orcamentoRepository).delete(existente);
    }

    @Test
    void deve_lancar_excecao_ao_deletar_orcamento_inexistente() {
        when(orcamentoRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(OrcamentoNaoEncontradoException.class,
                () -> useCase.deletar(USER_ID, 99L));

        verify(orcamentoRepository, never()).delete(any());
    }

    @Test
    void deve_lancar_excecao_ao_deletar_orcamento_de_outro_usuario() {
        Orcamento outroUsuario = orcamentoSalvo(1L, Categoria.ALIMENTACAO, 500);
        outroUsuario.setUserId(99L);

        when(orcamentoRepository.findById(1L)).thenReturn(Optional.of(outroUsuario));

        assertThrows(OrcamentoNaoEncontradoException.class,
                () -> useCase.deletar(USER_ID, 1L));

        verify(orcamentoRepository, never()).delete(any());
    }
}
