package com.extratoPopular.infrastructure.persistence;

import com.extratoPopular.domain.model.Transacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransacaoRepository extends JpaRepository<Transacao, Long> {

    Optional<Transacao> findByUserIdAndHash(Long userId, String hash);

    List<Transacao> findAllByUserId(Long userId);
}
