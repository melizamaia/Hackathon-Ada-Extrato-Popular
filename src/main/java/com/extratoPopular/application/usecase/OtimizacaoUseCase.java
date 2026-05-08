package com.extratoPopular.application.usecase;

import com.extratoPopular.application.service.otimizacao.AcaoOtimizacao;
import com.extratoPopular.application.service.otimizacao.ItemOtimizacao;
import com.extratoPopular.application.service.otimizacao.OtimizacaoStrategy;
import com.extratoPopular.application.service.otimizacao.OtimizacaoStrategyFactory;
import com.extratoPopular.application.service.otimizacao.ResultadoOtimizacao;
import com.extratoPopular.domain.enums.TipoTransacao;
import com.extratoPopular.domain.exception.UsuarioNaoAutenticadoException;
import com.extratoPopular.domain.model.Orcamento;
import com.extratoPopular.domain.model.Transacao;
import com.extratoPopular.domain.model.User;
import com.extratoPopular.infrastructure.persistence.OrcamentoRepository;
import com.extratoPopular.infrastructure.persistence.TransacaoRepository;
import com.extratoPopular.infrastructure.persistence.UserRepository;
import com.extratoPopular.interfaces.dto.OtimizacaoResponse;
import com.extratoPopular.interfaces.dto.OtimizacaoResponse.AcaoRecomendada;
import com.extratoPopular.interfaces.dto.OtimizacaoResponse.SugestaoCategoria;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OtimizacaoUseCase {

    private final TransacaoRepository transacaoRepository;
    private final OrcamentoRepository orcamentoRepository;
    private final UserRepository userRepository;
    private final OtimizacaoStrategyFactory strategyFactory;

    public OtimizacaoUseCase(TransacaoRepository transacaoRepository,
                              OrcamentoRepository orcamentoRepository,
                              UserRepository userRepository,
                              OtimizacaoStrategyFactory strategyFactory) {
        this.transacaoRepository = transacaoRepository;
        this.orcamentoRepository  = orcamentoRepository;
        this.userRepository       = userRepository;
        this.strategyFactory      = strategyFactory;
    }

    public OtimizacaoResponse execute(Long userId, int mes, int ano, String algoritmo) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsuarioNaoAutenticadoException("Usuário não encontrado."));

        LocalDate inicio = LocalDate.of(ano, mes, 1);
        LocalDate fim    = inicio.withDayOfMonth(inicio.lengthOfMonth());

        List<Transacao> transacoes = transacaoRepository.findAllByUserIdAndDataBetween(userId, inicio, fim);
        List<Orcamento> orcamentos = orcamentoRepository.findByUserIdAndMesAndAno(userId, mes, ano);

        Map<String, BigDecimal> gastosPorCategoria = new HashMap<>();
        BigDecimal totalReceitas = BigDecimal.ZERO;
        BigDecimal totalDespesas = BigDecimal.ZERO;

        for (Transacao t : transacoes) {
            if (t.getTipo() == TipoTransacao.CREDITO) {
                totalReceitas = totalReceitas.add(t.getValor());
            } else {
                BigDecimal valorAbsoluto = t.getValor().abs();
                totalDespesas = totalDespesas.add(valorAbsoluto);
                gastosPorCategoria.merge(t.getCategoria().name(), valorAbsoluto, BigDecimal::add);
            }
        }

        List<SugestaoCategoria> gastosAcimaOrcamento = new ArrayList<>();
        List<ItemOtimizacao> itensParaOtimizar = new ArrayList<>();
        BigDecimal economiasPotenciais = BigDecimal.ZERO;

        Set<String> categoriasComOrcamento = orcamentos.stream()
                .map(o -> o.getCategoria().name())
                .collect(Collectors.toSet());

        for (Orcamento orcamento : orcamentos) {
            BigDecimal gasto = gastosPorCategoria.getOrDefault(orcamento.getCategoria().name(), BigDecimal.ZERO);
            if (gasto.compareTo(orcamento.getValorLimite()) > 0) {
                BigDecimal excesso = gasto.subtract(orcamento.getValorLimite());
                economiasPotenciais = economiasPotenciais.add(excesso);

                gastosAcimaOrcamento.add(new SugestaoCategoria(
                        orcamento.getCategoria().name(),
                        orcamento.getValorLimite(),
                        gasto,
                        excesso,
                        "Reduza R$ " + excesso.setScale(2, RoundingMode.HALF_UP)
                                + " em " + orcamento.getCategoria() + " para ficar dentro do orçamento"
                ));

                itensParaOtimizar.add(new ItemOtimizacao(
                        orcamento.getCategoria().name(),
                        excesso,
                        orcamento.getValorLimite(),
                        gasto,
                        calcularDificuldade(excesso, orcamento.getValorLimite())
                ));
            }
        }

        List<String> categoriasSeemOrcamento = gastosPorCategoria.keySet().stream()
                .filter(c -> !categoriasComOrcamento.contains(c))
                .sorted()
                .toList();

        BigDecimal saldo = totalReceitas.subtract(totalDespesas);
        BigDecimal rendaMensal = user.getRendaMensal();
        BigDecimal percentualRendaComprometida = rendaMensal.compareTo(BigDecimal.ZERO) > 0
                ? totalDespesas.divide(rendaMensal, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        String recomendacaoGeral = gerarRecomendacao(
                percentualRendaComprometida, gastosAcimaOrcamento.size(),
                categoriasSeemOrcamento.size(), economiasPotenciais);

        OtimizacaoStrategy strategy = strategyFactory.get(algoritmo);
        ResultadoOtimizacao resultado = strategy.otimizar(itensParaOtimizar);

        List<AcaoRecomendada> acoesRecomendadas = resultado.acoes().stream()
                .map(a -> new AcaoRecomendada(
                        a.categoria(),
                        a.economiaPotencial(),
                        a.dificuldade(),
                        a.descricaoAcao(),
                        a.prioridade()
                ))
                .toList();

        return new OtimizacaoResponse(
                mes, ano, saldo, percentualRendaComprometida,
                gastosAcimaOrcamento, categoriasSeemOrcamento,
                economiasPotenciais, recomendacaoGeral,
                resultado.nomeAlgoritmo(),
                resultado.descricaoAlgoritmo(),
                acoesRecomendadas
        );
    }

    private int calcularDificuldade(BigDecimal excesso, BigDecimal limite) {
        if (limite.compareTo(BigDecimal.ZERO) == 0) return 5;
        BigDecimal pct = excesso.divide(limite, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        if (pct.compareTo(BigDecimal.valueOf(200)) > 0) return 10;
        if (pct.compareTo(BigDecimal.valueOf(100)) > 0) return 8;
        if (pct.compareTo(BigDecimal.valueOf(50))  > 0) return 6;
        if (pct.compareTo(BigDecimal.valueOf(20))  > 0) return 4;
        return 2;
    }

    private String gerarRecomendacao(BigDecimal percentualRenda, int qtdAcima,
                                     int qtdSemOrcamento, BigDecimal economiasPotenciais) {
        if (percentualRenda.compareTo(BigDecimal.valueOf(100)) > 0) {
            return "Atenção: seus gastos superam sua renda mensal. Eliminar os excessos pouparia R$ "
                    + economiasPotenciais.setScale(2, RoundingMode.HALF_UP) + ".";
        }
        if (qtdAcima > 0) {
            return "Você tem " + qtdAcima + " categoria(s) acima do orçamento. "
                    + "Economias potenciais de R$ " + economiasPotenciais.setScale(2, RoundingMode.HALF_UP)
                    + " se os limites fossem respeitados.";
        }
        if (qtdSemOrcamento > 0) {
            return "Seus gastos estão dentro dos orçamentos definidos. "
                    + "Considere definir orçamentos para as " + qtdSemOrcamento
                    + " categoria(s) ainda sem limite.";
        }
        return "Parabéns! Seus gastos estão dentro de todos os orçamentos definidos. "
                + percentualRenda + "% da renda mensal comprometida.";
    }
}
