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
import com.extratoPopular.interfaces.dto.TransacaoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class IngestaoTransacoesUseCase {

    private static final Logger log = LoggerFactory.getLogger(IngestaoTransacoesUseCase.class);

    private final TransacaoRepository transacaoRepository;
    private final HashService hashService;
    private final CategorizacaoService categorizacaoService;

    public IngestaoTransacoesUseCase(TransacaoRepository transacaoRepository,
                                     HashService hashService,
                                     CategorizacaoService categorizacaoService) {
        this.transacaoRepository  = transacaoRepository;
        this.hashService          = hashService;
        this.categorizacaoService = categorizacaoService;
    }

    public BulkImportResponse execute(Long userId, List<TransacaoRaw> transacoes, FonteImportacao fonte) {
        return execute(userId, transacoes, fonte, 0);
    }

    public BulkImportResponse execute(Long userId, List<TransacaoRaw> transacoes, FonteImportacao fonte, int parseErrors) {
        int importadas = 0;
        int duplicatas = 0;
        int erros      = parseErrors;
        List<TransacaoResponse> importadasList = new ArrayList<>();

        for (int i = 0; i < transacoes.size(); i++) {
            TransacaoRaw raw = transacoes.get(i);
            try {
                String hash = hashService.gerarHash(raw.data(), raw.valor(), raw.descricao());

                if (transacaoRepository.findByUserIdAndHash(userId, hash).isPresent()) {
                    duplicatas++;
                    continue;
                }

                Categoria   categoria = categorizacaoService.categorizar(raw.descricao());
                TipoTransacao tipo    = raw.valor().compareTo(BigDecimal.ZERO) < 0
                                        ? TipoTransacao.DEBITO
                                        : TipoTransacao.CREDITO;

                Transacao transacao = new Transacao();
                transacao.setUserId(userId);
                transacao.setData(raw.data());
                transacao.setValor(raw.valor());
                transacao.setDescricao(raw.descricao());
                transacao.setCategoria(categoria);
                transacao.setTipo(tipo);
                transacao.setFonte(fonte);
                transacao.setHash(hash);

                Transacao salva = transacaoRepository.save(transacao);
                importadasList.add(TransacaoResponse.de(salva));
                importadas++;

            } catch (Exception e) {
                log.error("Erro ao processar transação #{} [{}]: {}", i + 1, raw.descricao(), e.getMessage());
                erros++;
            }
        }

        return new BulkImportResponse(importadas, duplicatas, erros, importadasList);
    }
}
