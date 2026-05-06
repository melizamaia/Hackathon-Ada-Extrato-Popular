package com.extratoPopular.application.usecase;

import com.extratoPopular.domain.exception.OrcamentoDuplicadoException;
import com.extratoPopular.domain.exception.OrcamentoNaoEncontradoException;
import com.extratoPopular.domain.model.Orcamento;
import com.extratoPopular.infrastructure.persistence.OrcamentoRepository;
import com.extratoPopular.interfaces.dto.OrcamentoRequest;
import com.extratoPopular.interfaces.dto.OrcamentoResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrcamentoUseCase {

    private final OrcamentoRepository orcamentoRepository;

    public OrcamentoUseCase(OrcamentoRepository orcamentoRepository) {
        this.orcamentoRepository = orcamentoRepository;
    }

    public OrcamentoResponse criar(Long userId, OrcamentoRequest request) {
        orcamentoRepository
                .findByUserIdAndCategoriaAndMesAndAno(userId, request.categoria(), request.mes(), request.ano())
                .ifPresent(existing -> {
                    throw new OrcamentoDuplicadoException(
                            "Já existe um orçamento para " + request.categoria() +
                            " em " + request.mes() + "/" + request.ano());
                });

        Orcamento orcamento = new Orcamento();
        orcamento.setUserId(userId);
        orcamento.setCategoria(request.categoria());
        orcamento.setValorLimite(request.valorLimite());
        orcamento.setMes(request.mes());
        orcamento.setAno(request.ano());

        return OrcamentoResponse.de(orcamentoRepository.save(orcamento));
    }

    public List<OrcamentoResponse> listar(Long userId, Integer mes, Integer ano) {
        return orcamentoRepository.findByUserIdAndMesAndAno(userId, mes, ano)
                .stream()
                .map(OrcamentoResponse::de)
                .toList();
    }

    public OrcamentoResponse atualizar(Long userId, Long id, OrcamentoRequest request) {
        Orcamento orcamento = orcamentoRepository.findById(id)
                .filter(o -> o.getUserId().equals(userId))
                .orElseThrow(() -> new OrcamentoNaoEncontradoException("Orçamento não encontrado: id=" + id));

        orcamento.setCategoria(request.categoria());
        orcamento.setValorLimite(request.valorLimite());
        orcamento.setMes(request.mes());
        orcamento.setAno(request.ano());

        return OrcamentoResponse.de(orcamentoRepository.save(orcamento));
    }

    public void deletar(Long userId, Long id) {
        Orcamento orcamento = orcamentoRepository.findById(id)
                .filter(o -> o.getUserId().equals(userId))
                .orElseThrow(() -> new OrcamentoNaoEncontradoException("Orçamento não encontrado: id=" + id));

        orcamentoRepository.delete(orcamento);
    }
}
