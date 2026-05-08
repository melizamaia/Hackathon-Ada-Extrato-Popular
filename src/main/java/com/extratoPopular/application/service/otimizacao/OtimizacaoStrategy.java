package com.extratoPopular.application.service.otimizacao;

import java.util.List;

/**
 * Strategy Pattern (GoF) — permite trocar o algoritmo de otimização em tempo de execução
 * sem alterar clientes (OtimizacaoUseCase). Respeita OCP, SRP e DIP:
 *   OCP  — novos algoritmos são adicionados criando novas implementações, sem modificar código existente.
 *   SRP  — cada estratégia encapsula exatamente um algoritmo de seleção.
 *   DIP  — o use-case depende desta abstração, não das implementações concretas.
 */
public interface OtimizacaoStrategy {

    ResultadoOtimizacao otimizar(List<ItemOtimizacao> itens);

    String getNome();

    String getDescricao();
}
