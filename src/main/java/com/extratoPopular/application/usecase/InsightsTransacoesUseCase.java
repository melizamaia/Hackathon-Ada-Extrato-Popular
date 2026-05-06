package com.extratoPopular.application.usecase;

import com.extratoPopular.domain.enums.TipoTransacao;
import com.extratoPopular.domain.exception.UsuarioNaoAutenticadoException;
import com.extratoPopular.domain.model.Transacao;
import com.extratoPopular.domain.model.User;
import com.extratoPopular.infrastructure.persistence.TransacaoRepository;
import com.extratoPopular.infrastructure.persistence.UserRepository;
import com.extratoPopular.interfaces.dto.InsightsResponse;
import com.extratoPopular.interfaces.dto.InsightsResponse.CategoriaInsight;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class InsightsTransacoesUseCase {

    private final TransacaoRepository transacaoRepository;
    private final UserRepository userRepository;

    public InsightsTransacoesUseCase(TransacaoRepository transacaoRepository,
                                     UserRepository userRepository) {
        this.transacaoRepository = transacaoRepository;
        this.userRepository = userRepository;
    }

    public InsightsResponse execute(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsuarioNaoAutenticadoException("Usuário não encontrado."));

        List<Transacao> transacoes = transacaoRepository.findAllByUserId(userId);

        Map<String, BigDecimal> gastosPorCategoria = new HashMap<>();
        BigDecimal totalDespesas = BigDecimal.ZERO;
        int qtdDebitos = 0;

        for (Transacao t : transacoes) {
            if (t.getTipo() == TipoTransacao.DEBITO) {
                BigDecimal valorAbsoluto = t.getValor().abs();
                totalDespesas = totalDespesas.add(valorAbsoluto);
                gastosPorCategoria.merge(t.getCategoria().name(), valorAbsoluto, BigDecimal::add);
                qtdDebitos++;
            }
        }

        String categoriaComMaiorGasto = gastosPorCategoria.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        BigDecimal mediaGastoPorTransacao = qtdDebitos > 0
                ? totalDespesas.divide(BigDecimal.valueOf(qtdDebitos), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal rendaMensal = user.getRendaMensal();
        BigDecimal percentualRendaComprometida = rendaMensal.compareTo(BigDecimal.ZERO) > 0
                ? totalDespesas.divide(rendaMensal, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        boolean alertaGastoElevado = totalDespesas.compareTo(rendaMensal) > 0;

        final BigDecimal totalDespesasRef = totalDespesas;
        List<CategoriaInsight> topCategorias = gastosPorCategoria.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .map(e -> {
                    BigDecimal percentual = totalDespesasRef.compareTo(BigDecimal.ZERO) > 0
                            ? e.getValue().divide(totalDespesasRef, 4, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100))
                                    .setScale(2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    return new CategoriaInsight(e.getKey(), e.getValue(), percentual);
                })
                .toList();

        return new InsightsResponse(
                categoriaComMaiorGasto,
                percentualRendaComprometida,
                mediaGastoPorTransacao,
                alertaGastoElevado,
                topCategorias
        );
    }
}
