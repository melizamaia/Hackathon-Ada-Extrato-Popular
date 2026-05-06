package com.extratoPopular.application.usecase;

import com.extratoPopular.application.dto.TransacaoRaw;
import com.extratoPopular.application.service.CategorizacaoService;
import com.extratoPopular.application.service.HashService;
import com.extratoPopular.domain.enums.Categoria;
import com.extratoPopular.domain.enums.FonteImportacao;
import com.extratoPopular.domain.enums.TipoTransacao;
import com.extratoPopular.domain.model.Transacao;
import com.extratoPopular.infrastructure.persistence.TransacaoRepository;
import com.extratoPopular.interfaces.dto.BulkImportResponse;
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
class IngestaoTransacoesUseCaseTest {

    @Mock private TransacaoRepository transacaoRepository;
    @Mock private HashService          hashService;
    @Mock private CategorizacaoService categorizacaoService;

    @InjectMocks
    private IngestaoTransacoesUseCase useCase;

    private static final Long   USER_ID = 1L;
    private static final LocalDate DATA = LocalDate.of(2024, 6, 15);

    private TransacaoRaw raw(String descricao, String valor) {
        return new TransacaoRaw(DATA, new BigDecimal(valor), descricao);
    }

    private Transacao transacaoSalva(Long id, TipoTransacao tipo, Categoria categoria) {
        Transacao t = new Transacao();
        t.setId(id);
        t.setUserId(USER_ID);
        t.setData(DATA);
        t.setValor(new BigDecimal("-50.00"));
        t.setDescricao("DESC");
        t.setCategoria(categoria);
        t.setTipo(tipo);
        t.setFonte(FonteImportacao.CSV);
        t.setHash("hash" + id);
        return t;
    }

    @BeforeEach
    void setUp() {
        // por padrão: sem duplicata, categoria OUTROS
        when(transacaoRepository.findByUserIdAndHash(anyLong(), anyString()))
                .thenReturn(Optional.empty());
        when(categorizacaoService.categorizar(anyString()))
                .thenReturn(Categoria.OUTROS);
    }

    // -------------------------------------------------------------------------
    // Importação bem-sucedida
    // -------------------------------------------------------------------------

    @Test
    void deve_importar_transacao_nova_e_retornar_importadas_1() {
        TransacaoRaw raw = raw("IFOOD RESTAURANTE", "-150.00");

        when(hashService.gerarHash(any(), any(), any())).thenReturn("abc123");
        when(categorizacaoService.categorizar("IFOOD RESTAURANTE")).thenReturn(Categoria.ALIMENTACAO);

        Transacao salva = transacaoSalva(10L, TipoTransacao.DEBITO, Categoria.ALIMENTACAO);
        when(transacaoRepository.save(any())).thenReturn(salva);

        BulkImportResponse response = useCase.execute(USER_ID, List.of(raw), FonteImportacao.CSV);

        assertEquals(1, response.importadas());
        assertEquals(0, response.duplicatas());
        assertEquals(0, response.erros());
        assertEquals(1, response.transacoes().size());
        assertEquals(10L, response.transacoes().get(0).id());
    }

    @Test
    void deve_importar_multiplas_transacoes_novas() {
        List<TransacaoRaw> raws = List.of(
                raw("MERCADO EXTRA",   "-200.00"),
                raw("UBER TRIP",        "-50.00"),
                raw("SALARIO JUNHO",  "3000.00")
        );

        when(hashService.gerarHash(any(), any(), any()))
                .thenReturn("h1", "h2", "h3");
        when(transacaoRepository.save(any()))
                .thenAnswer(inv -> {
                    Transacao t = inv.getArgument(0);
                    t.setId((long) (Math.random() * 1000));
                    return t;
                });

        BulkImportResponse response = useCase.execute(USER_ID, raws, FonteImportacao.CSV);

        assertEquals(3, response.importadas());
        assertEquals(0, response.duplicatas());
        assertEquals(0, response.erros());
        assertEquals(3, response.transacoes().size());
    }

    @Test
    void deve_persistir_userId_fonte_hash_corretamente() {
        TransacaoRaw raw = raw("NETFLIX", "-39.90");
        when(hashService.gerarHash(any(), any(), any())).thenReturn("hashNetflix");
        when(transacaoRepository.save(any())).thenAnswer(inv -> {
            Transacao t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        useCase.execute(USER_ID, List.of(raw), FonteImportacao.CSV);

        verify(transacaoRepository).save(argThat(t ->
                t.getUserId().equals(USER_ID) &&
                t.getFonte() == FonteImportacao.CSV &&
                "hashNetflix".equals(t.getHash())
        ));
    }

    // -------------------------------------------------------------------------
    // TipoTransacao
    // -------------------------------------------------------------------------

    @Test
    void deve_derivar_tipo_DEBITO_quando_valor_negativo() {
        TransacaoRaw raw = raw("ALUGUEL", "-1500.00");
        when(hashService.gerarHash(any(), any(), any())).thenReturn("h1");
        when(transacaoRepository.save(any())).thenAnswer(inv -> {
            Transacao t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        useCase.execute(USER_ID, List.of(raw), FonteImportacao.CSV);

        verify(transacaoRepository).save(argThat(t -> t.getTipo() == TipoTransacao.DEBITO));
    }

    @Test
    void deve_derivar_tipo_CREDITO_quando_valor_positivo() {
        TransacaoRaw raw = raw("SALARIO", "3000.00");
        when(hashService.gerarHash(any(), any(), any())).thenReturn("h1");
        when(transacaoRepository.save(any())).thenAnswer(inv -> {
            Transacao t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        useCase.execute(USER_ID, List.of(raw), FonteImportacao.CSV);

        verify(transacaoRepository).save(argThat(t -> t.getTipo() == TipoTransacao.CREDITO));
    }

    @Test
    void deve_derivar_tipo_CREDITO_quando_valor_zero() {
        TransacaoRaw raw = raw("AJUSTE ZERO", "0.00");
        when(hashService.gerarHash(any(), any(), any())).thenReturn("h1");
        when(transacaoRepository.save(any())).thenAnswer(inv -> {
            Transacao t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        useCase.execute(USER_ID, List.of(raw), FonteImportacao.CSV);

        verify(transacaoRepository).save(argThat(t -> t.getTipo() == TipoTransacao.CREDITO));
    }

    // -------------------------------------------------------------------------
    // Deduplicação
    // -------------------------------------------------------------------------

    @Test
    void deve_incrementar_duplicatas_quando_hash_ja_existe() {
        TransacaoRaw raw = raw("IFOOD", "-50.00");
        when(hashService.gerarHash(any(), any(), any())).thenReturn("hashExistente");
        when(transacaoRepository.findByUserIdAndHash(USER_ID, "hashExistente"))
                .thenReturn(Optional.of(new Transacao()));

        BulkImportResponse response = useCase.execute(USER_ID, List.of(raw), FonteImportacao.CSV);

        assertEquals(0, response.importadas());
        assertEquals(1, response.duplicatas());
        assertEquals(0, response.erros());
        verify(transacaoRepository, never()).save(any());
    }

    @Test
    void deve_processar_mix_de_novas_e_duplicatas() {
        TransacaoRaw nova      = raw("MERCADO",   "-100.00");
        TransacaoRaw duplicada = raw("UBER",        "-30.00");

        when(hashService.gerarHash(any(), any(), any()))
                .thenReturn("hashNova", "hashDuplicada");
        when(transacaoRepository.findByUserIdAndHash(USER_ID, "hashNova"))
                .thenReturn(Optional.empty());
        when(transacaoRepository.findByUserIdAndHash(USER_ID, "hashDuplicada"))
                .thenReturn(Optional.of(new Transacao()));
        when(transacaoRepository.save(any())).thenAnswer(inv -> {
            Transacao t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        BulkImportResponse response = useCase.execute(USER_ID, List.of(nova, duplicada), FonteImportacao.CSV);

        assertEquals(1, response.importadas());
        assertEquals(1, response.duplicatas());
        assertEquals(0, response.erros());
    }

    // -------------------------------------------------------------------------
    // Tratamento de erros inesperados
    // -------------------------------------------------------------------------

    @Test
    void deve_incrementar_erros_quando_save_lanca_excecao_e_continuar_proximas() {
        TransacaoRaw raw1 = raw("COM ERRO",   "-50.00");
        TransacaoRaw raw2 = raw("SEM ERRO",  "-80.00");

        when(hashService.gerarHash(any(), any(), any()))
                .thenReturn("h1", "h2");

        Transacao salva = transacaoSalva(2L, TipoTransacao.DEBITO, Categoria.OUTROS);
        when(transacaoRepository.save(any()))
                .thenThrow(new RuntimeException("Erro de banco simulado"))
                .thenReturn(salva);

        BulkImportResponse response = useCase.execute(USER_ID, List.of(raw1, raw2), FonteImportacao.CSV);

        assertEquals(1, response.importadas());
        assertEquals(0, response.duplicatas());
        assertEquals(1, response.erros());
    }

    @Test
    void deve_incrementar_erros_quando_hashService_lanca_excecao() {
        TransacaoRaw raw1 = raw("HASH FALHA", "-50.00");
        TransacaoRaw raw2 = raw("HASH OK",    "-80.00");

        when(hashService.gerarHash(any(), any(), any()))
                .thenThrow(new RuntimeException("Falha no hash"))
                .thenReturn("hashOk");

        Transacao salva = transacaoSalva(1L, TipoTransacao.DEBITO, Categoria.OUTROS);
        when(transacaoRepository.save(any())).thenReturn(salva);

        BulkImportResponse response = useCase.execute(USER_ID, List.of(raw1, raw2), FonteImportacao.CSV);

        assertEquals(1, response.importadas());
        assertEquals(1, response.erros());
    }

    // -------------------------------------------------------------------------
    // Lista vazia
    // -------------------------------------------------------------------------

    @Test
    void deve_retornar_zeros_quando_lista_vazia() {
        BulkImportResponse response = useCase.execute(USER_ID, List.of(), FonteImportacao.OFX);

        assertEquals(0, response.importadas());
        assertEquals(0, response.duplicatas());
        assertEquals(0, response.erros());
        assertTrue(response.transacoes().isEmpty());
        verifyNoInteractions(hashService, categorizacaoService, transacaoRepository);
    }

    // -------------------------------------------------------------------------
    // Integração de categorização
    // -------------------------------------------------------------------------

    @Test
    void deve_chamar_categorizacaoService_com_descricao_original() {
        TransacaoRaw raw = raw("FARMACIA DROGASIL", "-35.00");
        when(hashService.gerarHash(any(), any(), any())).thenReturn("h1");
        when(categorizacaoService.categorizar("FARMACIA DROGASIL")).thenReturn(Categoria.SAUDE);
        when(transacaoRepository.save(any())).thenAnswer(inv -> {
            Transacao t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        useCase.execute(USER_ID, List.of(raw), FonteImportacao.CSV);

        verify(categorizacaoService).categorizar("FARMACIA DROGASIL");
        verify(transacaoRepository).save(argThat(t -> t.getCategoria() == Categoria.SAUDE));
    }
}
