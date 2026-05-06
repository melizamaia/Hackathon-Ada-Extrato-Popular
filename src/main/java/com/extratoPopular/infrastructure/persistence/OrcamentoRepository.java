package com.extratoPopular.infrastructure.persistence;

import com.extratoPopular.domain.enums.Categoria;
import com.extratoPopular.domain.model.Orcamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrcamentoRepository extends JpaRepository<Orcamento, Long> {

    List<Orcamento> findByUserIdAndMesAndAno(Long userId, Integer mes, Integer ano);

    Optional<Orcamento> findByUserIdAndCategoriaAndMesAndAno(Long userId, Categoria categoria, Integer mes, Integer ano);

    List<Orcamento> findAllByUserId(Long userId);
}
